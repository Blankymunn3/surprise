import CoreModel
import DesignSystem
import Foundation
import MapKit
import SwiftUI

/**
 **지도 — 패미컴 컨트롤러 스타일.**

 안드로이드 `MapPlastic.kt` 와 같은 짜임새입니다. 상태·Store 는 `MapView` 에 있고
 이 파일은 그리기만 합니다.

 **조작하는 것이 지도 위에 뜨지 않습니다.**

 지도를 몸통에 **끼운 화면**으로 다루고, 조작은 전부 몸통
 위(화면 밖)로 내립니다 — 컨트롤러의 버튼이 TV 화면 안에 있지 않은 것과 같습니다.

 잃는 것도 있습니다 — **지도가 그만큼 좁아집니다.** 지도 앱에서 지도 넓이는 그냥
 손해라, 이 스타일을 채택할지 정할 때 가장 크게 저울질할 대목입니다.
 */
struct PlasticMapBody: View {
    let store: MapStore
    let onAddPhoto: (Region?) -> Void

    @Binding var position: MapCameraPosition
    @Binding var visibleRegion: MKCoordinateRegion?
    /// 지역을 칠할 대표사진. 오버레이가 픽셀을 그려야 해서 `Image` 가 아니라 `CGImage` 입니다.
    @Binding var covers: [String: CGImage]
    @FocusState.Binding var searching: Bool

    /// 못 찾았을 때 알릴 말. 화면 아래에 잠깐 뜹니다.
    @State private var notice: String?

    /// 위치를 찾는 사람. 화면이 살아 있는 동안 하나만 둡니다 —
    /// 누를 때마다 새로 만들면 권한 창의 답을 받을 자리가 사라집니다.
    @State private var finder = MyLocationFinder()
    /// 찾은 내 자리. 지도에 표시로 남습니다.
    @State private var me: CLLocationCoordinate2D?

    var body: some View {
        VStack(spacing: 0) {
            cartridgeSlot

            // 끼운 화면. 지도와 지역 시트가 **둘 다 이 안에** 있습니다 —
            // 시트가 몸통 위로 올라오면 화면 밖에 그림이 그려지는 꼴이라 어색합니다.
            screen
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .sunken(PlasticRadius.screen)

            pad
        }
        .padding(.horizontal, MemorySpace.s)
        .background(PlasticColor.body)
    }

    private var screen: some View {
        map
            .overlay(alignment: .top) {
                // 검색 결과는 화면 **안** 위쪽에 겹칩니다. 슬롯 바로 아래에 두면
                // 몸통 위에 종이가 붙은 것처럼 떠 보입니다.
                if !store.state.results.isEmpty {
                    slotResults.padding(MemorySpace.s)
                }
            }
            .overlay(alignment: .bottom) {
                if let sheet = store.state.sheet {
                    PlasticRegionSheet(sheet: sheet, store: store, onAddPhoto: onAddPhoto)
                } else if let notice {
                    // 시트가 떠 있으면 알림을 띄우지 않습니다 — 같은 자리라 겹칩니다.
                    Text(notice)
                        .font(MemoryFont.font(12.5, .semibold))
                        .foregroundStyle(PlasticColor.onPlate)
                        .padding(.horizontal, MemorySpace.m)
                        .padding(.vertical, MemorySpace.s)
                        .background(
                            RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                                .fill(PlasticColor.plate)
                        )
                        .padding(MemorySpace.m)
                        .transition(.opacity)
                }
            }
    }

    // MARK: 카트리지 슬롯 = 검색칸

    /**
     컨트롤러가 아니라 본체에서 가져온 형태입니다. 위쪽에 가로로 길게 파인 홈이 있고
     거기에 무언가를 꽂는다 — 지역을 찾아 넣는 자리로 이만한 그림이 없습니다.
     */
    private var cartridgeSlot: some View {
        HStack(spacing: MemorySpace.s) {
            // 슬롯 왼쪽의 작은 홈. 돋보기 아이콘 대신입니다 — 이 판 위에서는
            // 아이콘 하나가 떠 보이는데, 파인 홈은 슬롯의 일부로 읽힙니다.
            RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                .fill(PlasticColor.ink)
                .frame(width: 3, height: 14)

            TextField(
                "",
                text: Binding(
                    get: { store.state.query },
                    set: { value in Task { await store.search(value) } }
                ),
                prompt: Text(localized("map_search_placeholder"))
                    .foregroundStyle(PlasticColor.onPlateDim)
            )
            .textFieldStyle(.plain)
            .font(MemoryFont.font(15, .semibold))
            .foregroundStyle(PlasticColor.onPlate)
            .tint(PlasticColor.red)
            .focused($searching)

            if !store.state.query.isEmpty {
                Button { Task { await store.search("") } } label: {
                    Text("×")
                        .font(MemoryFont.font(17, .semibold))
                        .foregroundStyle(PlasticColor.onPlateDim)
                        .padding(.horizontal, MemorySpace.xs)
                }
                .buttonStyle(.plasticPress)
                .accessibilityLabel(localized("map_search_clear"))
            }
        }
        .padding(.horizontal, MemorySpace.m)
        .padding(.vertical, 11)
        .sunken(PlasticRadius.chip, face: PlasticColor.plateLo)
        .padding(.vertical, MemorySpace.s)
    }

    private var slotResults: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ForEach(store.state.results) { region in
                    Button { Task { await store.open(region) } } label: {
                        HStack(alignment: .lastTextBaseline, spacing: MemorySpace.s) {
                            Text(region.name)
                                .font(MemoryFont.font(15, .semibold))
                                .foregroundStyle(PlasticColor.onPlate)
                            if let parent = region.parentName {
                                Text(parent)
                                    .font(MemoryFont.font(12.5, .semibold))
                                    .foregroundStyle(PlasticColor.onPlateDim)
                            }
                            Spacer(minLength: 0)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, MemorySpace.m)
                        .padding(.vertical, 11)
                    }
                    .buttonStyle(.plasticPress)

                    PlasticColor.plateLo.frame(height: 1)
                }
            }
        }
        .frame(maxHeight: 240)
        .background(PlasticColor.plate)
        .clipShape(RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous))
    }

    // MARK: 지도

    private var map: some View {
        // SwiftUI `Map` 이 아니라 MKMapView(`PhotoMap`)입니다 — 다녀온 지역을
        // **대표사진 한 장으로** 칠하려면 오버레이에 그림을 그릴 수 있어야 하는데,
        // SwiftUI 의 `MapPolygon` 은 색밖에 못 칠합니다. 어두운 지도·탭·딱지·내 자리도
        // 전부 그 안에 있습니다.
        #if canImport(UIKit)
        PhotoMap(
            position: $position,
            visibleRegion: $visibleRegion,
            fills: store.state.fills,
            covers: covers,
            outline: store.state.outline,
            me: me,
            onTap: { latitude, longitude in
                searching = false
                Task { await store.tapMap(latitude: latitude, longitude: longitude) }
            }
        )
        #else
        // macOS 는 컴파일만 합니다(`swift build` 검증용). UIViewRepresentable 이 없어
        // 지도 대신 검정 판을 둡니다 — 앱은 iOS 로만 나갑니다.
        PlasticColor.plate
        #endif
    }

    // MARK: 조작부

    /**
     아래 조작부 — **실물 컨트롤러의 배치 그대로**입니다.

     ```
       ✛ 십자키        ▭ ▭          ● ●
       지도 이동      축소 확대     B    A
     ```

     지금 화면 그대로 **회색 몸통 위**에 놓습니다. 실물은 버튼이 검정 페이스플레이트
     위에 있지만, 그 판을 여기 깔면 위의 지도 화면과 같은 검정이 두 덩어리가 되어
     어느 쪽이 화면인지 흐려집니다. 배치와 아이콘만 실물에서 가져옵니다.

     **A·B 글자는 붙이지 않습니다.** 무엇을 하는 버튼인지 알려 주지 않는 글자라,
     목록 화면에서 SELECT·START 를 뺀 것과 같은 이유입니다.

     */
    private var pad: some View {
        HStack {
            dpad
            Spacer(minLength: 0)

            // 가운데 고무 알약 둘 — 실물의 SELECT · START 자리입니다. 그 글자는 안 씁니다.
            HStack(spacing: MemorySpace.xs) {
                pill(plus: false, localized("map_zoom_out")) { nudgeZoom(by: 2) }
                pill(plus: true, localized("map_zoom_in")) { nudgeZoom(by: 0.5) }
            }
            .padding(PlasticSize.buttonInset)
            .raisedPlastic()

            Spacer(minLength: 0)

            // B · A — 실물처럼 B 가 왼쪽입니다. 자주 쓰는 쪽(올리기)이 A 인 것도 실물과
            // 같습니다: 오른쪽 끝 버튼이 엄지가 가장 편히 닿는 자리입니다.
            HStack(spacing: MemorySpace.s) {
                redButton("location", localized("map_my_location")) { Task { await goToMyLocation() } }
                redButton("plus", localized("map_add_photo")) { onAddPhoto(nil) }
            }
            .padding(PlasticSize.buttonInset)
            .raisedPlastic()
        }
        .padding(.horizontal, MemorySpace.xs)
        .padding(.vertical, MemorySpace.m)
    }

    /**
     고무 알약. 실물에서 SELECT·START 가 있던 자리이고, 여기서는 확대·축소입니다.

     ＋ · － 를 **직접 그립니다.** SF Symbol 의 `plus` 와 `minus` 는 굵기·길이가 서로
     달라서, 같은 크기로 놓아도 ＋ 가 눈에 띄게 커 보입니다(알약은 같은 크기인데
     버튼이 커 보이는 것으로 읽힙니다). 안드로이드는 두 아이콘을 같은 폭으로 그려 둬서
     애초에 그 문제가 없습니다 — 여기서도 같은 막대 둘로 맞춥니다.
     */
    private func pill(plus: Bool, _ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            ZStack {
                Capsule()
                    .fill(PlasticColor.onRubber)
                    .frame(width: glyphBar, height: glyphThick)
                if plus {
                    Capsule()
                        .fill(PlasticColor.onRubber)
                        .frame(width: glyphThick, height: glyphBar)
                }
            }
            .frame(width: PlasticSize.pillWidth, height: PlasticSize.pillHeight)
            .background(Capsule().fill(PlasticColor.rubber))
        }
        .buttonStyle(.plasticPress)
        .accessibilityLabel(label)
    }

    /// 빨간 A·B 버튼.
    private func redButton(
        _ symbol: String, _ label: String, action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(PlasticColor.onRed)
                .frame(width: PlasticSize.redButton, height: PlasticSize.redButton)
                .background(Circle().fill(PlasticColor.red))
        }
        .buttonStyle(.plasticPress)
        .accessibilityLabel(label)
    }

    /**
     십자키. 한 덩어리 고무 위에 눌리는 자리 넷을 얹습니다. **네 팔이 다 지도를 밉니다.**

     실물처럼 **십자 모양 하나**로 만들려면 세로 기둥과 가로 들보를 겹쳐 놓고
     그 위에 누를 자리를 배치해야 합니다. 모서리 네 곳은 비어 있습니다.

     가운데는 **누르는 자리가 아닙니다.** 실물에서도 십자키 한가운데는 그냥 플라스틱이라
     눌러도 아무 일이 없고, 여기서도 화살표 넷이 이미 할 일을 다 나눠 가졌습니다.
     */
    private var dpad: some View {
        let arm = PlasticSize.cross / 3

        return ZStack {
            RoundedRectangle(cornerRadius: PlasticRadius.knob, style: .continuous)
                .fill(PlasticColor.rubber)
                .frame(width: arm, height: PlasticSize.cross)
            RoundedRectangle(cornerRadius: PlasticRadius.knob, style: .continuous)
                .fill(PlasticColor.rubber)
                .frame(width: PlasticSize.cross, height: arm)

            arrow("chevron.up", localized("map_pad_up"), dx: 0, dy: -arm) { pan(dx: 0, dy: -panStep) }
            arrow("chevron.down", localized("map_pad_down"), dx: 0, dy: arm) { pan(dx: 0, dy: panStep) }
            arrow("chevron.left", localized("map_pad_left"), dx: -arm, dy: 0) { pan(dx: -panStep, dy: 0) }
            arrow("chevron.right", localized("map_pad_right"), dx: arm, dy: 0) { pan(dx: panStep, dy: 0) }

            // 가운데의 오목한 원. 실물의 그 원이고, 누르는 곳은 아닙니다.
            Circle()
                .fill(PlasticColor.ink)
                .frame(width: arm, height: arm)
                .overlay(Circle().fill(PlasticColor.rubberHi).frame(width: PlasticSize.dotCore))
                .allowsHitTesting(false)
        }
        .frame(width: PlasticSize.cross, height: PlasticSize.cross)
        .padding(PlasticSize.buttonInset)
        .raisedPlastic()
    }

    private func arrow(
        _ symbol: String,
        _ label: String,
        dx: CGFloat,
        dy: CGFloat,
        action: @escaping () -> Void
    ) -> some View {
        let arm = PlasticSize.cross / 3
        return Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(PlasticColor.onRubber)
                .frame(width: arm, height: arm)
        }
        .buttonStyle(.plasticArm)
        .accessibilityLabel(label)
        .offset(x: dx, y: dy)
    }

    // MARK: 지도 움직이기

    /// 보이는 넓이를 [factor] 배로. 0.5 면 확대, 2 면 축소입니다.
    /// 기준 화면(`MapView.nudgeZoom`)과 같은 계산입니다.
    private func nudgeZoom(by factor: Double) {
        guard let now = visibleRegion else { return }
        let span = MKCoordinateSpan(
            latitudeDelta: min(150, now.span.latitudeDelta * factor),
            longitudeDelta: min(300, now.span.longitudeDelta * factor)
        )
        withAnimation(.easeInOut(duration: 0.25)) {
            position = .region(MKCoordinateRegion(center: now.center, span: span))
        }
    }

    /**
     지도를 옆으로 밉니다. 미는 폭은 **지금 보이는 넓이에 대한 비율**입니다 —
     고정 도수로 밀면 확대했을 때는 화면 밖으로 날아가고 축소했을 때는 꿈쩍도 안 합니다.
     안드로이드는 화면 픽셀 비율로, 여기는 보이는 경위도 비율로 같은 결과를 냅니다.
     */
    private func pan(dx: Double, dy: Double) {
        guard let now = visibleRegion else { return }
        let center = CLLocationCoordinate2D(
            latitude: max(-85, min(85, now.center.latitude - now.span.latitudeDelta * dy)),
            longitude: now.center.longitude + now.span.longitudeDelta * dx
        )
        withAnimation(.easeInOut(duration: 0.25)) {
            position = .region(MKCoordinateRegion(center: center, span: now.span))
        }
    }

    /**
     지금 자리로 지도를 옮깁니다.

     못 찾으면 **왜 못 찾았는지**를 말합니다 — 눌렀는데 아무 일이 없으면 고장 난
     것으로 보입니다. 안드로이드는 스낵바로 같은 말을 합니다.

     **지역 시트는 건드리지 않습니다.** 지역을 보다가 "여기가 어디쯤이지" 하고 누르는
     일이라, 보던 시트가 닫히면 하던 일이 끊깁니다.
     */
    private func goToMyLocation() async {
        switch await finder.find() {
        case let .found(latitude, longitude):
            let here = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
            withAnimation(.easeInOut(duration: 0.4)) {
                me = here
                position = .region(MKCoordinateRegion(
                    center: here,
                    span: MKCoordinateSpan(latitudeDelta: meSpan, longitudeDelta: meSpan)
                ))
            }
        case .denied:
            await say(localized("map_location_denied"))
        case .off:
            await say(localized("map_location_off"))
        case .notFound:
            await say(localized("map_location_not_found"))
        }
    }

    /// 잠깐 띄웠다 지웁니다. 닫기 버튼을 두지 않습니다 — 알리기만 하는 말이라
    /// 치우는 일까지 시킬 까닭이 없습니다.
    private func say(_ text: String) async {
        withAnimation { notice = text }
        try? await Task.sleep(for: .seconds(3))
        withAnimation { notice = nil }
    }
}

/**
 내 위치로 옮길 때 보이는 넓이. **동네가 보이는 정도**입니다 — 큰길과 동 이름이 읽힙니다.

 경계 없는 지역(`MapFocus.spot`, 1.2도)보다 훨씬 좁습니다. 지역을 고를 때는 "어디쯤"
 이면 되지만 내 위치는 "지금 여기" 를 보는 것이라, 도 단위로 보이면 점만 찍히고
 정작 내가 어디 있는지는 알 수 없습니다. 안드로이드 `ME_ZOOM`(14단)과 같은 넓이입니다.
 */
private let meSpan = 0.04

/// 십자키 좌·우 한 번에 미는 폭. 보이는 넓이의 1/3 이면 밀린 것이 보이면서도 길을 잃지 않습니다.
private let panStep: Double = 0.33

/// ＋ · － 를 이루는 막대의 길이와 굵기. **둘이 같은 막대를 씁니다** —
/// ＋ 는 거기에 세로 막대 하나가 더해질 뿐이라, 두 알약이 같은 무게로 보입니다.
/// 안드로이드 아이콘(5.5→18.5, 굵기 2)과 같은 비율입니다.
private let glyphBar: CGFloat = 13
private let glyphThick: CGFloat = 2

/**
 지역 시트 — 끼운 화면 **안에서** 아래부터 올라오는 검정 판.

 몸통 색(회색)을 쓰지 않습니다. 화면 안에 몸통 색이 나타나면 플라스틱이 화면을
 뚫고 올라온 것처럼 보입니다. 화면 안의 것은 화면 색으로 그립니다.
 */
private struct PlasticRegionSheet: View {
    let sheet: RegionSheetUi
    let store: MapStore
    let onAddPhoto: (Region?) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: MemorySpace.s) {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(alignment: .lastTextBaseline, spacing: MemorySpace.s) {
                        Text(sheet.region.name)
                            .font(MemoryFont.font(17, .bold))
                            .foregroundStyle(PlasticColor.onPlate)
                            .lineLimit(1)
                        if let parent = sheet.region.parentName {
                            Text(parent)
                                .font(MemoryFont.font(12.5, .semibold))
                                .foregroundStyle(PlasticColor.onPlateDim)
                        }
                    }
                    Text(localized("map_sheet_count_and_hint", sheet.photos.count))
                        .font(MemoryFont.font(11, .semibold))
                        .foregroundStyle(PlasticColor.onPlateDim)
                }
                Spacer(minLength: 0)

                // 닫기는 검은 고무 버튼입니다 — 빨강은 주 동작에만.
                Button { store.dismissSheet() } label: {
                    Text("×")
                        .font(MemoryFont.font(17, .bold))
                        .foregroundStyle(PlasticColor.onRubber)
                        .frame(width: PlasticSize.sheetClose, height: PlasticSize.sheetClose)
                        .background(Circle().fill(PlasticColor.rubber))
                }
                .buttonStyle(.plasticPress)
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
                        .frame(width: PlasticSize.sheetPhoto, height: PlasticSize.sheetPhoto)
                        .onTapGesture { Task { await store.setCover(photo.id) } }
                    }
                }
            }
        }
        .padding(.horizontal, MemorySpace.m)
        .padding(.top, MemorySpace.m)
        .padding(.bottom, MemorySpace.l)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PlasticColor.plate)
        .clipShape(RoundedRectangle(cornerRadius: PlasticRadius.screen, style: .continuous))
        .transition(.move(edge: .bottom))
    }
}
