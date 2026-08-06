import CoreModel
import DesignSystem
import FeatureCalendar
import FeatureMap
import FeatureSpace
import FeatureUpload
import SwiftUI

/// 공간 안. 위쪽 가운데의 `지도 | 달력` 으로 오갑니다 (`docs/app/SCREENS.md`).
/// 두 탭은 같은 사진을 '어디' 와 '언제' 로 보는 것뿐입니다.
struct SpaceDetailView: View {
    let spaceId: SpaceId
    /// 혼자면 기기 안 사진, 같이 쓰면 서버 사진. 고르는 일은 `AppContainer` 가 하고
    /// 이 화면은 어느 쪽인지 모릅니다.
    let kind: SpaceKind
    /// 머리말에 그대로 나오는 짜국 이름.
    let name: String

    @Environment(\.dismiss) private var dismiss
    @State private var tab = 0
    @State private var uploading = false
    @State private var menuOpen = false
    /// 지역 시트에서 열었으면 그 지역. 아래 ＋ 로 열었으면 `nil` 입니다.
    @State private var uploadRegion: Region?
    @State private var calendar: CalendarStore
    @State private var map: MapStore

    init(spaceId: SpaceId, kind: SpaceKind, name: String) {
        self.spaceId = spaceId
        self.kind = kind
        self.name = name
        _calendar = State(initialValue: AppContainer.shared.calendarStore(spaceId, kind))
        _map = State(initialValue: AppContainer.shared.mapStore(spaceId, kind))
    }

    var body: some View {
        // 머리말과 탭은 **지도 위에 떠 있지 않고 자리를 차지합니다.** 지도가 그 아래에서
        // 시작하니, 러시아처럼 위로 긴 나라가 검색칸 뒤로 숨을 자리 자체가 없습니다.
        VStack(spacing: 0) {
            topBar
            // 패미컴 스타일에서는 탭도 몸통 위의 고무 스위치입니다.
            if plasticTrial {
                plasticTabs
            } else {
                Segmented(options: ["지도", "달력"], selection: $tab)
                    .padding(.horizontal, MemorySpace.xl)
                    .padding(.top, 2)
                    .padding(.bottom, 10)
            }

            // 탭도 옆으로 밀립니다. 누른 쪽으로 미끄러져야 어느 쪽으로 옮겼는지 보입니다.
            Group {
                if tab == 0 {
                    MapView(store: map) { region in
                        uploadRegion = region
                        uploading = true
                    }
                    .transition(.move(edge: .leading).combined(with: .opacity))
                } else {
                    calendarTab
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .animation(.easeInOut(duration: 0.3), value: tab)
        }
        .background(plasticTrial ? PlasticColor.body : MemoryColor.paper)
        .navigationBarBackButtonHidden()
        .toolbar(.hidden, for: .navigationBar)
        // 아래에서 올라오는 시트입니다. 다른 시트들처럼 **화면을 덮지 않는 판**으로
        // 뜹니다 — 사진 목록은 판 안에서 구릅니다. 안드로이드 `UPLOAD_SHEET_HEIGHT` 와
        // 같은 값입니다.
        //
        // 예전에는 `.large` 였습니다("여러 장이면 훑어 내려야 한다"). 그런데 그러면
        // 시트가 아니라 전체 화면으로 보입니다 — 만들기·참여·로그인은 다 화면 일부만
        // 덮는데 이것만 혼자 화면을 삼켜서 같은 종류의 동작으로 안 읽혔습니다.
        // 머리말과 아래 버튼이 붙박이라 몇 장을 골랐든 '올리기' 는 늘 같은 자리입니다.
        .sheet(isPresented: $uploading) {
            UploadView(store: uploadStore()) { uploading = false }
                .presentationDetents([.fraction(uploadSheetHeight)])
                .presentationDragIndicator(.hidden)
        }
        .overlay {
            if menuOpen {
                SpaceMenu(store: AppContainer.shared.spaceMenuStore(spaceId)) { menuOpen = false }
            }
        }
        .onChange(of: uploading) { _, open in
            // 시트가 닫힌 뒤에 새로 받아옵니다. 방금 올린 사진이 바로 보여야 합니다.
            guard !open else { return }
            Task {
                await AppContainer.shared.refreshPhotos(spaceId, kind)
                await map.refresh()
                await calendar.refresh()
            }
        }
    }

    /// 올리기 스토어. 지역 시트에서 왔으면 **그 지역을 미리 넣어** 둡니다 —
    /// 이미 아는 곳을 다시 고르게 하지 않으려는 것입니다.
    private func uploadStore() -> UploadStore {
        let store = AppContainer.shared.uploadStore(spaceId, kind)
        // 아직 사진을 고르기 전이라 바로 못 넣습니다. Store 가 들고 있다가
        // 사진이 들어오면 EXIF 값 대신 이 값을 씁니다.
        if let uploadRegion { store.preselect(uploadRegion) }
        return store
    }

    /// 달력에서는 지역을 알 수 없습니다 — 날짜만 아는 자리라서요.
    /// 지난번에 지역 시트에서 열어 둔 값이 남지 않게 비웁니다.
    private func openUpload() {
        uploadRegion = nil
        uploading = true
    }

    @ViewBuilder
    private var calendarTab: some View {
        // 패미컴 스타일에서는 ＋ 가 떠 있지 않고 **달력 안쪽 조작부**에 앉습니다.
        // 몸통 위에 버튼이 다 모여 있는데 하나만 화면 위에 떠 있으면 어긋납니다.
        if plasticTrial {
            CalendarView(store: calendar, onAddPhoto: openUpload)
        } else {
            CalendarView(store: calendar)
                .overlay(alignment: .bottomTrailing) {
                    MemoryFab(action: openUpload)
                        .padding(.trailing, 14)
                        .padding(.bottom, 18)
                }
        }
    }

    /**
     탭 두 칸 — 몸통 위의 고무 스위치.

     고른 쪽만 빨갛습니다. 컨트롤러에서 빨강은 "지금 누른 것" 이고,
     여기서 지금 누른 것은 보고 있는 탭입니다.
     */
    private var plasticTabs: some View {
        HStack(spacing: 0) {
            ForEach(Array(["지도", "달력"].enumerated()), id: \.offset) { index, label in
                Button { tab = index } label: {
                    Text(label)
                        .font(MemoryFont.font(15, .bold))
                        .foregroundStyle(tab == index ? PlasticColor.onRed : PlasticColor.onRubber)
                        .frame(maxWidth: .infinity)
                        .frame(height: 34)
                        .background(
                            RoundedRectangle(cornerRadius: PlasticRadius.knob, style: .continuous)
                                .fill(tab == index ? PlasticColor.red : Color.clear)
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(
            RoundedRectangle(cornerRadius: PlasticRadius.housing, style: .continuous)
                .fill(PlasticColor.rubber)
        )
        .padding(.horizontal, MemorySpace.m)
        .padding(.bottom, MemorySpace.s)
    }

    /**
     짜국 안쪽의 머리말 — 뒤로 · 이름 · ⋯.

     이름이 **가운데가 아니라 왼쪽**입니다. 가운데에 두면 이름 길이에 따라 자리가
     매번 달라지는데, 왼쪽에 붙이면 늘 같은 곳에서 시작합니다.
     */
    private var topBar: some View {
        HStack(spacing: 2) {
            barButton("chevron.left", label: "뒤로") { dismiss() }

            Text(name)
                .memoryTitle()
                // 몸통 위의 글자는 잉크가 아니라 플라스틱에 새긴 검정입니다.
                .foregroundStyle(plasticTrial ? PlasticColor.ink : MemoryColor.ink)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, alignment: .leading)

            if kind == .personal {
                // 이 딱지는 몸통 위에서 **파인 자리**로 그립니다. 흰 면에 잉크 선은
                // 플라스틱 위에서 종이를 붙인 것처럼 떠 보입니다.
                if plasticTrial {
                    Text("이 폰에만")
                        .font(MemoryFont.font(11, .bold))
                        .foregroundStyle(PlasticColor.onPlateDim)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(
                            RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                                .fill(PlasticColor.plate)
                        )
                } else {
                    Text("이 폰에만")
                        .memoryMicro()
                        .foregroundStyle(MemoryColor.ink)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 2)
                        .background(MemoryColor.surface)
                        .overlay(
                            Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border)
                        )
                }
            }

            barButton("ellipsis", label: "더 보기") { menuOpen = true }
        }
        .padding(.horizontal, MemorySpace.s)
        .padding(.vertical, 6)
    }

    private func barButton(
        _ symbol: String, label: String, action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(MemoryColor.ink)
                .frame(width: 38, height: 38)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

/// 올리기 시트가 화면에서 차지하는 높이. 안드로이드 `UPLOAD_SHEET_HEIGHT` 와 같은 값입니다.
private let uploadSheetHeight: CGFloat = 0.62
