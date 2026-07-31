import CoreModel
import DesignSystem
import FeatureCalendar
import FeatureMap
import FeatureUpload
import SwiftUI

/// 공간 안. 위쪽 가운데의 `지도 | 달력` 으로 오갑니다 (`docs/app/SCREENS.md`).
/// 두 탭은 같은 사진을 '어디' 와 '언제' 로 보는 것뿐입니다.
struct SpaceDetailView: View {
    let spaceId: SpaceId
    /// 혼자면 기기 안 사진, 둘이면 서버 사진. 고르는 일은 `AppContainer` 가 하고
    /// 이 화면은 어느 쪽인지 모릅니다.
    let kind: SpaceKind

    @Environment(\.dismiss) private var dismiss
    @State private var tab = 0
    @State private var uploading = false
    @State private var calendar: CalendarStore
    @State private var map: MapStore

    init(spaceId: SpaceId, kind: SpaceKind) {
        self.spaceId = spaceId
        self.kind = kind
        _calendar = State(initialValue: AppContainer.shared.calendarStore(spaceId, kind))
        _map = State(initialValue: AppContainer.shared.mapStore(spaceId, kind))
    }

    var body: some View {
        ZStack(alignment: .top) {
            MemoryColor.paper.ignoresSafeArea()

            // 탭도 옆으로 밀립니다. 누른 쪽으로 미끄러져야 어느 쪽으로 옮겼는지 보입니다.
            Group {
                if tab == 0 {
                    MapView(store: map) { uploading = true }
                        .transition(.move(edge: .leading).combined(with: .opacity))
                } else {
                    calendarTab
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                }
            }
            .animation(.easeInOut(duration: 0.3), value: tab)

            topBar
        }
        .navigationBarBackButtonHidden()
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $uploading) {
            UploadView(store: AppContainer.shared.uploadStore(spaceId, kind)) {
                uploading = false
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.hidden)
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

    private var calendarTab: some View {
        VStack(spacing: 0) {
            Color.clear.frame(height: 52)
            CalendarView(store: calendar)
        }
        .overlay(alignment: .bottomTrailing) {
            MemoryFab { uploading = true }
                .padding(.trailing, MemorySpace.xl)
                .padding(.bottom, 40)
        }
    }

    /// 지도 위에서는 유리, 달력(종이 위)에서는 그냥 놓입니다 — 같은 부품, 다른 층.
    private var topBar: some View {
        HStack {
            circleButton("arrow.left", label: "뒤로", floating: tab == 0) { dismiss() }
            Spacer()
            Segmented(options: ["지도", "달력"], selection: $tab, floating: tab == 0)
            Spacer()
            circleButton("ellipsis", label: "더 보기", floating: tab == 0) { }
        }
        .padding(.horizontal, MemorySpace.l)
        .padding(.top, MemorySpace.s)
    }

    private func circleButton(
        _ symbol: String, label: String, floating: Bool, action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(MemoryColor.ink)
                .frame(width: 40, height: 40)
                .background {
                    if floating {
                        Circle().fill(.ultraThinMaterial)
                            .overlay(Circle().strokeBorder(MemoryColor.ink.opacity(0.07), lineWidth: 0.5))
                            .shadow(color: MemoryColor.ink.opacity(0.14), radius: 12, y: 8)
                    }
                }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}
