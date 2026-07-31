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
    @State private var camera: MapCameraPosition = .region(.korea)
    @State private var sheetHeight: CGFloat = 0
    @FocusState private var searching: Bool
    private let topInset: CGFloat
    private let onAddPhoto: () -> Void

    public init(store: MapStore, topInset: CGFloat = 96, onAddPhoto: @escaping () -> Void) {
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
        .task { await store.refresh() }
        .onChange(of: store.state.focus) { _, focus in
            guard let focus else { return }
            withAnimation(.easeInOut(duration: 0.4)) {
                camera = .region(MKCoordinateRegion(
                    center: .init(latitude: focus.latitude, longitude: focus.longitude),
                    span: MKCoordinateSpan(latitudeDelta: 1.2, longitudeDelta: 1.2)
                ))
            }
        }
    }

    private var floatBottom: CGFloat {
        store.state.sheet == nil ? 40 : max(40, sheetHeight + MemorySpace.l)
    }

    private var map: some View {
        MapReader { proxy in
            Map(position: $camera) {
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
