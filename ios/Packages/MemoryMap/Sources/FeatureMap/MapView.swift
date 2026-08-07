import CoreModel
import DesignSystem
import Foundation
import MapKit
import SwiftUI

/**
 지도 탭.

 안드로이드는 MapLibre 를 쓰는데 여기서는 **MapKit** 을 씁니다. iOS 에 이미 들어 있어
 키도 필요 없고 받을 것도 없습니다. 지도만 다르고 **지역 판정은 두 앱이 같은 코드**로 합니다
 (`RegionCatalog.regionAt` — 기기 안에서 좌표를 지역으로 바꿉니다).

 지도가 화면 끝까지 차고, 조작하는 것만 그 위에 뜹니다. 유리는 그 떠 있는 층에만 씁니다.
 */
public struct MapView: View {
    @State private var store: MapStore
    @State private var position: MapCameraPosition = .region(.korea)
    @State private var sheetHeight: CGFloat = 0
    /// 지도가 실제로 몇 점 높이인지. 시트가 덮는 만큼을 빼고 맞추려면 있어야 합니다.
    @FocusState private var searching: Bool
    /// 지역을 칠할 대표사진. 주소가 아니라 **그림 자체**가 있어야 채울 수 있어서
    /// 미리 받아 둡니다.
    @State private var covers: [String: Image] = [:]
    /// 지금 지도가 보여 주고 있는 범위. 확대·축소를 여기서부터 계산합니다 —
    /// `MapCameraPosition` 은 우리가 넣은 값만 알려 주고, 손으로 옮긴 것은 모릅니다.
    @State private var visibleRegion: MKCoordinateRegion?
    /// 위치를 찾는 사람. 화면이 살아 있는 동안 하나만 둡니다 —
    /// 누를 때마다 새로 만들면 권한 창의 답을 받을 자리가 사라집니다.
    @State private var finder = MyLocationFinder()
    /// 위치를 못 찾았을 때 알릴 말. 지도 아래에 잠깐 뜹니다.
    @State private var notice: String?
    /// 찾은 내 자리. 지도에 표시로 남습니다 — 옮겨 준 뒤 손으로 밀어도 그 지점을
    /// 잃지 않아야 합니다.
    @State private var me: CLLocationCoordinate2D?
    /// 사진 올리기를 엽니다. **지역 시트에서 눌렀으면 그 지역**이 넘어갑니다 —
    /// 이미 아는 곳을 올리기 화면에서 다시 고르게 하면 안 됩니다.
    /// 아래 ＋ 로 눌렀으면 `nil` 이고, 그때는 사진의 정보가 지역을 정합니다.
    private let onAddPhoto: (Region?) -> Void

    public init(store: MapStore, onAddPhoto: @escaping (Region?) -> Void) {
        self._store = State(initialValue: store)
        self.onAddPhoto = onAddPhoto
    }

    public var body: some View {
        // 조작하는 것이 지도 위가 아니라 몸통 위(화면 밖)에 섭니다.
        //
        // 지도의 상태(카메라·보이는 범위·대표사진)는 **여기 그대로 둡니다.**
        // 그리는 쪽으로 옮기면 화면이 다시 그려질 때마다 보던 자리를 잃습니다.
        PlasticMapBody(
            store: store,
            onAddPhoto: onAddPhoto,
            position: $position,
            visibleRegion: $visibleRegion,
            covers: $covers,
            searching: $searching
        )
        .task { await store.refresh() }
        .task(id: store.state.fills) { await loadCovers() }
        .onChange(of: store.state.focusCount) { _, _ in fitToFocus() }
    }

    /// 고른 지역이 **시트 위쪽 화면 안에** 다 들어오게 맞춥니다.
    /// 지도는 이미 가려지는 만큼 줄여 놓았으므로, 고른 곳을 그대로 주면 됩니다.
    /// 남는 자리를 계산해 가운데를 옮기던 일은 더 안 합니다.
    private func fitToFocus() {
        guard let focus = store.state.focus else { return }
        withAnimation(.easeInOut(duration: 0.4)) {
            position = .region(focus.region)
        }
    }

    /**
     아래를 시트가 덮는 만큼 **위로 올려** 잡습니다.

     지도 위쪽 `높이 - 덮인 높이` 안에 지역이 들어가야 하므로, 남는 자리 비율만큼 위아래를
     넓히고 그 넓어진 만큼 가운데를 아래로 내립니다 — 지역의 윗변은 그대로 두고 아래로만
     자리를 벌리는 셈입니다. 안 그러면 화면에는 들어와도 아래 절반이 시트 뒤에 가립니다.
     */
    /// 대표사진을 한 번씩만 받아 둡니다. 이미 받은 주소는 건너뜁니다.
    private func loadCovers() async {
        for fill in store.state.fills where covers[fill.coverURL] == nil {
            guard let url = URL(string: fill.coverURL),
                  let (data, _) = try? await URLSession.shared.data(from: url)
            else { continue }
            #if canImport(UIKit)
            if let image = UIImage(data: data) {
                covers[fill.coverURL] = Image(uiImage: image)
            }
            #endif
        }
    }

    /**
     검색 결과. **사진이 있는 지역이 먼저** 옵니다 — 이미 다녀온 곳을 다시 찾는 일이
     새 곳을 찾는 일보다 훨씬 잦습니다. 그 순서는 Store 가 정하고 여기서는 그리기만 합니다.
    /**
     왼쪽 아래: 확대 · 축소 · 내 위치.

     세 칸이 **따로 떨어져** 섭니다 — 붙여 놓으면 가운데 선이 두 겹이 되고,
     무엇이 한 벌인지도 흐려집니다.
    /**
     지금 자리로 지도를 옮깁니다. 패미컴 화면(`PlasticMapBody.goToMyLocation`)과
     같은 일을 하고, 못 찾았을 때 알리는 방식만 이 화면의 것입니다.
}

/**
 지도 위의 표시 — **사진 수만 적은 작은 잉크 딱지**입니다.

 예전에는 지름 44 짜리 원에 대표사진을 넣었는데, 이제 지역 자체가 그 사진으로
 칠해집니다. 같은 사진을 두 번 보여 줄 까닭이 없고, 원이 지역을 덮어 가렸습니다.

 **누를 수 없습니다.** 지역을 고르는 일은 지도를 누르면 되고, 딱지까지 누르게 하면
 딱지를 살짝 빗나갔을 때만 되는 이상한 경계가 생깁니다.
 */
private struct PinBadge: View {
    let count: Int

    var body: some View {
        Text("\(count)")
            .memoryMicro()
            .foregroundStyle(MemoryColor.onAccent)
            .frame(width: 22, height: 16)
            .background(MemoryColor.ink)
            .allowsHitTesting(false)
    }
}

/**
 지역 시트. **위쪽에만 2px 잉크 선**을 긋고 나머지는 흰 면입니다.

 손잡이(작은 막대)를 두지 않습니다 — 이 시트는 끌어 올리는 것이 아니라 지역을
 누르면 나타났다가 × 로 닫는 것이라, 끌 수 있게 생기면 안 됩니다.
 */
private struct RegionSheet: View {
    let sheet: RegionSheetUi
    let store: MapStore
    let onAddPhoto: (Region?) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            MemoryColor.ink.frame(height: MemoryStroke.divider)

            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top, spacing: 10) {
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(alignment: .lastTextBaseline, spacing: MemorySpace.s) {
                            Text(sheet.region.name).memoryTitle()
                            if let parent = sheet.region.parentName {
                                Text(parent).memoryMicro().foregroundStyle(MemoryColor.ink2)
                            }
                        }
                        Text(localized("map_sheet_count", sheet.photos.count))
                            .memoryLabel()
                            .foregroundStyle(MemoryColor.ink2)
                    }
                    Spacer(minLength: 0)
                    Button { store.dismissSheet() } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(MemoryColor.ink)
                            .frame(width: 34, height: 34)
                            .overlay(
                                Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border)
                            )
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(localized("map_sheet_close"))
                }

                Spacer().frame(height: MemorySpace.m)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: MemorySpace.s) {
                        ForEach(sheet.photos) { photo in
                            PhotoThumb(
                                url: photo.downloadURL,
                                isCover: photo.id == sheet.coverId,
                                dateLabel: "\(photo.takenOn.month).\(photo.takenOn.day)"
                            )
                            .frame(width: 92, height: 92)
                            .onTapGesture { Task { await store.setCover(photo.id) } }
                        }
                    }
                }

                // 누르면 바로 대표가 되므로, 그렇다고 **말해 줘야** 합니다. 버튼이 없으니
                // 알려 주지 않으면 누를 수 있다는 것 자체를 모릅니다.
                Text(localized("map_sheet_cover_hint"))
                    .memoryMicro()
                    .foregroundStyle(MemoryColor.ink2)
                    .padding(.top, 6)

                Spacer().frame(height: MemorySpace.m)

                // 이 시트가 아는 지역을 그대로 들려 보냅니다.
                PrimaryButton(localized("map_sheet_add")) { onAddPhoto(sheet.region) }
            }
            .padding(.horizontal, 18)
            .padding(.top, 14)
            .padding(.bottom, 26)
        }
        .background(MemoryColor.surface)
        .transition(.move(edge: .bottom))
    }
}

extension MKCoordinateRegion {
    /// 처음 보여 줄 자리. 사진이 하나도 없을 때 세계지도가 뜨면 어디를 눌러야 할지 모릅니다.
    static let korea = MKCoordinateRegion(
        center: .init(latitude: 36.5, longitude: 127.8),
        span: MKCoordinateSpan(latitudeDelta: 5.5, longitudeDelta: 5.5)
    )
}

extension MapFocus {
    /// 맞출 곳을 MapKit 이 아는 말로 옮깁니다.
    var region: MKCoordinateRegion {
        switch self {
        case let .spot(latitude, longitude):
            // 맞출 넓이가 없는 장소. 전에 쓰던 배율 그대로입니다.
            return MKCoordinateRegion(
                center: .init(latitude: latitude, longitude: longitude),
                span: MKCoordinateSpan(latitudeDelta: 1.2, longitudeDelta: 1.2)
            )

        case let .area(south, west, north, east):
            // 테두리가 화면 끝에 딱 붙으면 잘린 것처럼 보입니다. 사방에 조금 여유를 둡니다.
            let center = CLLocationCoordinate2D(
                latitude: (south + north) / 2, longitude: wrapped((west + east) / 2)
            )
            return MKCoordinateRegion(
                center: center,
                span: MKCoordinateSpan(
                    latitudeDelta: min(max(north - south, 0.02) * edgeRoom, 170),
                    longitudeDelta: min(max(east - west, 0.02) * edgeRoom, 350)
                )
            )
        }
    }
}

/// 테두리와 화면 끝 사이에 남길 여유.
private let edgeRoom = 1.18

/// 지도 위 물건들의 가장자리 여백. 시안이 정한 값입니다. 안드로이드 `Edge` 와 같습니다.
private let edge: CGFloat = 14

/// 날짜변경선을 넘는 지역은 가운데가 180 을 넘어갑니다. 지도에 줄 때는 접어서 줍니다 —
/// 넓이(span)는 그대로라 접어도 보이는 범위는 같습니다.
private func wrapped(_ longitude: Double) -> Double {
    (longitude + 540).truncatingRemainder(dividingBy: 360) - 180
}
