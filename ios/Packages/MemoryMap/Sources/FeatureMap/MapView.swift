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
    @State private var mapHeight: CGFloat = 0
    @FocusState private var searching: Bool
    /// 지역을 칠할 대표사진. 주소가 아니라 **그림 자체**가 있어야 채울 수 있어서
    /// 미리 받아 둡니다.
    @State private var covers: [String: Image] = [:]
    private let topInset: CGFloat
    private let onAddPhoto: () -> Void

    /// `topInset` 은 **안전영역 아래로** 더 미는 값입니다. 위 띠(뒤로 버튼 · 지도|달력)가
    /// 안전영역 아래 8 + 높이 40 = 48 을 쓰므로, 그 바로 밑에 붙이려면 56 입니다.
    /// 안드로이드의 96 은 상태바를 포함한 값이라 숫자가 다릅니다 — 같게 맞추면 오히려 어긋납니다.
    public init(store: MapStore, topInset: CGFloat = 56, onAddPhoto: @escaping () -> Void) {
        self._store = State(initialValue: store)
        self.topInset = topInset
        self.onAddPhoto = onAddPhoto
    }

    public var body: some View {
        ZStack(alignment: .top) {
            map

            VStack(spacing: MemorySpace.s) {
                searchPill
                if !store.state.results.isEmpty { results }
            }
            .padding(.horizontal, MemorySpace.l)
            .padding(.top, topInset)
        }
        .overlay(alignment: .bottomTrailing) {
            MemoryFab(action: onAddPhoto)
                .padding(.trailing, MemorySpace.xl)
                .padding(.bottom, floatBottom)
        }
        .overlay(alignment: .bottom) {
            if let sheet = store.state.sheet {
                RegionSheet(
                    sheet: sheet, canSetCover: store.state.canSetCover,
                    store: store, onAddPhoto: onAddPhoto
                )
                    // 시트 높이는 **재서** 씁니다. 사진이 있느냐에 따라 훌쩍 달라져서,
                    // 고정값으로 두면 시트가 짧을 때 FAB 만 허공에 뜹니다.
                    .background(
                        GeometryReader { proxy in
                            Color.clear.onAppear { sheetHeight = proxy.size.height }
                                .onChange(of: proxy.size.height) { _, value in sheetHeight = value }
                        }
                    )
            }
        }
        .ignoresSafeArea(edges: .bottom)
        .background(
            GeometryReader { proxy in
                Color.clear
                    .onAppear { mapHeight = proxy.size.height }
                    .onChange(of: proxy.size.height) { _, value in mapHeight = value }
            }
        )
        .task { await store.refresh() }
        .task(id: store.state.fills) { await loadCovers() }
        // **몇 번째 맞춤인지**를 봅니다. 맞출 곳만 보면 같은 지역을 다시 골랐을 때
        // 값이 그대로라 지도가 꿈쩍도 안 합니다.
        .onChange(of: store.state.focusCount) { _, _ in fitToFocus() }
        // 시트 높이는 시트가 뜬 뒤에야 잽니다. 재고 나면 그만큼 빼고 다시 맞춥니다.
        .onChange(of: sheetHeight) { _, _ in fitToFocus() }
    }

    /// 고른 지역이 **시트 위쪽 화면 안에** 다 들어오게 맞춥니다.
    private func fitToFocus() {
        guard let focus = store.state.focus else { return }
        let covered = store.state.sheet == nil ? 0 : sheetHeight
        withAnimation(.easeInOut(duration: 0.4)) {
            position = .region(lifted(focus.region, above: covered))
        }
    }

    /**
     아래를 시트가 덮는 만큼 **위로 올려** 잡습니다.

     지도 위쪽 `높이 - 덮인 높이` 안에 지역이 들어가야 하므로, 남는 자리 비율만큼 위아래를
     넓히고 그 넓어진 만큼 가운데를 아래로 내립니다 — 지역의 윗변은 그대로 두고 아래로만
     자리를 벌리는 셈입니다. 안 그러면 화면에는 들어와도 아래 절반이 시트 뒤에 가립니다.
     */
    private func lifted(_ region: MKCoordinateRegion, above covered: CGFloat) -> MKCoordinateRegion {
        guard mapHeight > 0, covered > 0 else { return region }
        // 시트가 아무리 높아도 지도 절반까지만 뺍니다. 남는 자리가 없으면 맞출 배율도
        // 나오지 않습니다.
        let room = mapHeight - min(covered, mapHeight / 2)
        let grown = region.span.latitudeDelta * mapHeight / room
        let top = region.center.latitude + region.span.latitudeDelta / 2
        return MKCoordinateRegion(
            center: .init(latitude: top - grown / 2, longitude: region.center.longitude),
            span: MKCoordinateSpan(
                latitudeDelta: min(grown, 170), longitudeDelta: region.span.longitudeDelta
            )
        )
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

    private var floatBottom: CGFloat {
        store.state.sheet == nil ? 40 : max(40, sheetHeight + MemorySpace.l)
    }

    private var map: some View {
        MapReader { proxy in
            Map(position: $position) {
                // 다녀온 지역을 **그 지역의 대표사진으로** 칠합니다.
                //
                // 살짝 비치게(85%) 두는 이유: 완전히 덮으면 그 지역의 길·지명이 사라져서
                // 어디인지 알 수 없게 됩니다. 사진은 "다녀왔다" 는 표시이지 지도를
                // 대신하는 것이 아닙니다.
                ForEach(store.state.fills) { fill in
                    if let image = covers[fill.coverURL] {
                        ForEach(Array(fill.polygons.enumerated()), id: \.offset) { _, polygon in
                            if let outer = polygon.first {
                                MapPolygon(coordinates: outer.map {
                                    CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
                                })
                                .foregroundStyle(ImagePaint(image: image, scale: 0.25).opacity(0.85))
                            }
                        }
                    }
                }

                // 고른 지역의 테두리. 웹과 같은 표시입니다 —
                // "지금 이 지역을 보고 있다" 를 지도 위에서 알 수 있어야 합니다.
                ForEach(Array(store.state.outline.enumerated()), id: \.offset) { _, ring in
                    MapPolyline(coordinates: ring.map {
                        CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
                    })
                    .stroke(MemoryColor.accent, lineWidth: 3)
                }

                ForEach(store.state.pins) { pin in
                    Annotation(
                        pin.region.displayName,
                        coordinate: .init(latitude: pin.latitude, longitude: pin.longitude)
                    ) {
                        PinBubble(pin: pin) { Task { await store.open(pin.region) } }
                    }
                }
            }
            .mapStyle(.standard(elevation: .flat, pointsOfInterest: .excludingAll))
            // 지도 위에 **종이를 한 겹** 덮습니다. MapKit 은 색을 직접 못 바꿔서,
            // 옅게 깔아 앱의 다른 화면과 결을 맞춥니다 (안드로이드도 같은 방식).
            // 표시(핀)까지 살짝 덮이지만 14% 라 알아보는 데 지장이 없습니다.
            .overlay {
                MemoryColor.mapLand
                    .opacity(0.14)
                    .allowsHitTesting(false)
                    .ignoresSafeArea()
            }
            .onTapGesture { point in
                // 검색하다 지도를 누르면 자판부터 내려갑니다. 자판이 화면 절반을 덮은 채로
                // 지역 시트가 올라오면 아무것도 안 보입니다.
                searching = false
                // 누른 자리가 어느 지역인지 **기기 안에서** 판정합니다. 사진 EXIF 와 같은 길입니다.
                guard let coordinate = proxy.convert(point, from: .local) else { return }
                Task { await store.tapMap(latitude: coordinate.latitude, longitude: coordinate.longitude) }
            }
        }
    }

    private var searchPill: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15))
                .foregroundStyle(MemoryColor.ink3)

            TextField("지역 검색", text: Binding(
                get: { store.state.query },
                set: { value in Task { await store.search(value) } }
            ))
            .textFieldStyle(.plain)
            .memoryBody()
            .focused($searching)

            if !store.state.query.isEmpty {
                Button {
                    Task { await store.search("") }
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(MemoryColor.ink3)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, MemorySpace.l)
        .padding(.vertical, 11)
        .glass()
    }

    private var results: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ForEach(store.state.results) { region in
                    Button { Task { await store.open(region) } } label: {
                        HStack {
                            Text(region.name).memoryBody()
                            Spacer()
                            if let parent = region.parentName {
                                Text(parent).memoryLabel().foregroundStyle(MemoryColor.ink3)
                            }
                        }
                        .padding(.horizontal, MemorySpace.l)
                        .padding(.vertical, MemorySpace.m)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .frame(maxHeight: 260)
        .background(MemoryColor.surface, in: RoundedRectangle(cornerRadius: MemoryRadius.card, style: .continuous))
        .shadow(color: MemoryColor.ink.opacity(0.12), radius: 12, y: 8)
    }
}

/// 지도 위의 표시. 대표사진이 있으면 **사진이 곧 표시**입니다 —
/// 어디에 뭘 남겼는지 지도만 봐도 알 수 있게.
private struct PinBubble: View {
    let pin: RegionPin
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack {
                Circle().fill(MemoryColor.surface)
                if let cover = pin.coverURL, let url = URL(string: cover) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        MemoryColor.fill
                    }
                    .clipShape(Circle())
                } else {
                    Text("\(pin.photoCount)")
                        .memoryMicro()
                        .foregroundStyle(MemoryColor.accent)
                }
            }
            .frame(width: 44, height: 44)
            .overlay(Circle().strokeBorder(MemoryColor.accent, lineWidth: 2))
            .shadow(color: MemoryColor.ink.opacity(0.25), radius: 6, y: 3)
        }
        .buttonStyle(.plain)
    }
}

/// 시트는 **불투명 흰색**입니다. 유리로 만들면 뒤의 지도가 비쳐 사진이 지저분해 보입니다.
private struct RegionSheet: View {
    let sheet: RegionSheetUi
    let canSetCover: Bool
    let store: MapStore
    let onAddPhoto: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Capsule()
                .fill(MemoryColor.line2)
                .frame(width: 36, height: 4)
                .frame(maxWidth: .infinity)
                .padding(.top, 10)
                .onTapGesture { store.dismissSheet() }

            Spacer().frame(height: MemorySpace.l)

            HStack(alignment: .bottom) {
                Text(sheet.region.name).memoryTitle()
                if let parent = sheet.region.parentName {
                    Text(parent).memoryLabel().foregroundStyle(MemoryColor.ink3)
                }
                Spacer()
                Text("사진 \(sheet.photos.count)장").memoryLabel().foregroundStyle(MemoryColor.ink2)
            }

            Spacer().frame(height: 14)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: MemorySpace.s) {
                    ForEach(sheet.photos) { photo in
                        PhotoThumb(
                            url: photo.downloadURL,
                            isCover: photo.id == sheet.coverId,
                            dateLabel: "\(photo.takenOn.month).\(photo.takenOn.day)"
                        )
                        .frame(width: 92, height: 92)
                        .background {
                            if photo.id == sheet.selected {
                                RoundedRectangle(cornerRadius: MemoryRadius.thumb, style: .continuous)
                                    .fill(MemoryColor.accentTint)
                            }
                        }
                        .onTapGesture { store.select(photo.id) }
                    }
                }
            }

            Spacer().frame(height: MemorySpace.l)

            HStack(spacing: MemorySpace.s) {
                Button { Task { await store.setCover() } } label: {
                    Text("대표로 지정")
                        .memoryHeadline()
                        .foregroundStyle(canSetCover ? MemoryColor.ink : MemoryColor.ink3)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(
                            RoundedRectangle(cornerRadius: MemoryRadius.button, style: .continuous)
                                .fill(MemoryColor.fill)
                        )
                }
                .buttonStyle(.plain)
                .disabled(!canSetCover)

                PrimaryButton("사진 추가", action: onAddPhoto)
            }
        }
        .padding(.horizontal, MemorySpace.xl)
        .padding(.bottom, 34)
        .background(MemoryColor.surface)
        .clipShape(UnevenRoundedRectangle(
            topLeadingRadius: MemoryRadius.sheet, topTrailingRadius: MemoryRadius.sheet,
            style: .continuous
        ))
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

/// 날짜변경선을 넘는 지역은 가운데가 180 을 넘어갑니다. 지도에 줄 때는 접어서 줍니다 —
/// 넓이(span)는 그대로라 접어도 보이는 범위는 같습니다.
private func wrapped(_ longitude: Double) -> Double {
    (longitude + 540).truncatingRemainder(dividingBy: 360) - 180
}
