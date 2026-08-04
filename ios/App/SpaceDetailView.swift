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
            Segmented(options: ["지도", "달력"], selection: $tab)
                .padding(.horizontal, MemorySpace.xl)
                .padding(.top, 2)
                .padding(.bottom, 10)

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
        .background(MemoryColor.paper)
        .navigationBarBackButtonHidden()
        .toolbar(.hidden, for: .navigationBar)
        // 올리기는 **전체 화면**입니다. 사진마다 어디·언제를 훑어 내리는 일이라
        // 지도를 가린 시트 안에서 할 일이 아닙니다.
        .fullScreenCover(isPresented: $uploading) {
            UploadView(store: uploadStore()) { uploading = false }
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

    private var calendarTab: some View {
        CalendarView(store: calendar)
            .overlay(alignment: .bottomTrailing) {
                // 달력에서는 지역을 알 수 없습니다 — 날짜만 아는 자리라서요.
                // 지난번에 지역 시트에서 열어 둔 값이 남지 않게 비웁니다.
                MemoryFab {
                    uploadRegion = nil
                    uploading = true
                }
                    .padding(.trailing, 14)
                    .padding(.bottom, 18)
            }
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
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, alignment: .leading)

            if kind == .personal {
                Text("이 폰에만")
                    .memoryMicro()
                    .foregroundStyle(MemoryColor.ink)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 2)
                    .background(MemoryColor.surface)
                    .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
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
