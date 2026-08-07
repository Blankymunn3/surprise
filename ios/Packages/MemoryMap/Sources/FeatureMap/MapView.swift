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
    /// 지도가 실제로 몇 점 높이인지. 시트가 덮는 만큼을 빼고 맞추려면 있어야 합니다.
    @FocusState private var searching: Bool
    /// 지역을 칠할 대표사진. 주소가 아니라 **그림 자체**가 있어야 채울 수 있어서
    /// 미리 받아 둡니다.
    @State private var covers: [String: Image] = [:]
    /// 지금 지도가 보여 주고 있는 범위. 확대·축소를 여기서부터 계산합니다 —
    /// `MapCameraPosition` 은 우리가 넣은 값만 알려 주고, 손으로 옮긴 것은 모릅니다.
    @State private var visibleRegion: MKCoordinateRegion?
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

/// 날짜변경선을 넘는 지역은 가운데가 180 을 넘어갑니다. 지도에 줄 때는 접어서 줍니다 —
/// 넓이(span)는 그대로라 접어도 보이는 범위는 같습니다.
private func wrapped(_ longitude: Double) -> Double {
    (longitude + 540).truncatingRemainder(dividingBy: 360) - 180
}
