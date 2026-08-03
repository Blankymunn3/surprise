import CoreModel
import DesignSystem
import Foundation
import SwiftUI

/// 앱의 메인. **공간이 하나뿐이어도 여기서 시작합니다.**
/// 카드는 사진이 전부입니다 — 공간을 알아보는 건 이름이 아니라 사진이라서요.
public struct SpaceListView: View {
    @State private var store: SpaceListStore
    /// 시트 높이는 **내용에 맞춥니다.** 만들기·참여·초대코드가 각각 길이가 달라서
    /// 하나로 고정하면 어떤 것은 비고 어떤 것은 잘립니다.
    @State private var sheetHeight: CGFloat = 260
    /// ID 만이 아니라 짜국을 통째로 넘깁니다 — 들어간 화면이 **종류**를 알아야
    /// 기기 안 사진을 볼지 서버 사진을 볼지 정할 수 있습니다.
    private let onOpen: (Space) -> Void

    public init(store: SpaceListStore, onOpen: @escaping (Space) -> Void) {
        self._store = State(initialValue: store)
        self.onOpen = onOpen
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MemorySpace.m) {
                HStack {
                    Text("짜국").memoryDisplay()
                    Spacer()
                    Text("나")
                        .memoryMicro()
                        .foregroundStyle(.white)
                        .frame(width: 32, height: 32)
                        .background(Circle().fill(MemoryColor.accent))
                }
                .padding(.horizontal, MemorySpace.xl)
                .padding(.top, MemorySpace.s)

                switch store.state.spaces {
                case .loading:
                    hint("불러오는 중이에요")
                case .failed:
                    hint("목록을 불러오지 못했어요")
                case .ready(let items):
                    if items.isEmpty {
                        emptyScene("아직 짜국이 없어요. 하나 만들어 볼까요?")
                    }
                    ForEach(items) { space in
                        SpaceCardView(space: space) { onOpen(space) }
                            .padding(.horizontal, MemorySpace.xl)
                    }
                }

                actionRow("새 짜국 만들기", system: "plus", tinted: true) {
                    Task { await store.send(.createTapped) }
                }
                actionRow("초대 코드로 참여", system: "person.2", tinted: false) {
                    Task { await store.send(.joinTapped) }
                }
            }
            .padding(.bottom, MemorySpace.xxxl)
        }
        .background(MemoryColor.paper)
        .task { await store.send(.appeared) }
        .sheet(isPresented: sheetShown) {
            SpaceSheet(store: store)
                .background(
                    GeometryReader { proxy in
                        Color.clear
                            .onAppear { sheetHeight = proxy.size.height }
                            .onChange(of: proxy.size.height) { _, value in sheetHeight = value }
                    }
                )
                .presentationDetents([.height(sheetHeight)])
                .presentationDragIndicator(.visible)
        }
    }

    /// 시트는 상태가 정하고 화면은 따라가기만 합니다. 닫으면 상태도 같이 닫힙니다 —
    /// 안 그러면 아래로 쓸어 내린 뒤 버튼을 눌러도 두 번째부터 안 열립니다.
    private var sheetShown: Binding<Bool> {
        Binding(
            get: { store.state.sheet != .none },
            set: { shown in
                if !shown { Task { await store.send(.sheetDismissed) } }
            }
        )
    }

    /// 빈 화면에 글자만 남기지 않습니다 — "여기에 뭔가 쌓일 자리" 로 보여야 합니다.
    private func emptyScene(_ text: String) -> some View {
        VStack(spacing: MemorySpace.l) {
            HillScene()
                .aspectRatio(HillScene.ratio, contentMode: .fit)
                .frame(maxWidth: 220)
                .clipShape(RoundedRectangle(cornerRadius: MemoryRadius.card, style: .continuous))
            Text(text)
                .memoryBody()
                .foregroundStyle(MemoryColor.ink3)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, MemorySpace.xxl)
    }

    private func hint(_ text: String) -> some View {
        Text(text)
            .memoryBody()
            .foregroundStyle(MemoryColor.ink3)
            .frame(maxWidth: .infinity)
            .padding(.vertical, MemorySpace.xxxl)
    }

    /// 만들기·참여는 목록 아래 **평범한 줄**입니다. 자주 하는 일이 아니라 강조하지 않습니다.
    private func actionRow(
        _ label: String, system: String, tinted: Bool, action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: MemorySpace.l) {
                Image(systemName: system)
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(tinted ? MemoryColor.accent : MemoryColor.ink2)
                    .frame(width: 38, height: 38)
                    .background(
                        RoundedRectangle(cornerRadius: MemoryRadius.thumb, style: .continuous)
                            .fill(tinted ? MemoryColor.accentTint : MemoryColor.fill)
                    )
                Text(label).memoryBody().foregroundStyle(MemoryColor.ink)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MemoryColor.ink3)
            }
            .padding(.horizontal, MemorySpace.xl)
            .padding(.vertical, MemorySpace.m)
        }
        .buttonStyle(.plain)
    }
}

struct SpaceCardView: View {
    let space: Space
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .bottomLeading) {
                if let cover = space.coverPhotoURL, let url = URL(string: cover) {
                    RemotePhoto(url: url) { MemoryColor.fill }
                } else {
                    MemoryColor.fill
                }

                // 흰 글자가 밝은 사진 위에서도 읽히도록 아래쪽을 어둡게
                LinearGradient(
                    stops: [
                        .init(color: .clear, location: 0.34),
                        .init(color: MemoryColor.ink.opacity(0.62), location: 1),
                    ],
                    startPoint: .top, endPoint: .bottom
                )

                VStack(alignment: .leading, spacing: 3) {
                    Text(space.name).memoryTitle().foregroundStyle(.white)
                    Text(meta).memoryLabel().foregroundStyle(.white.opacity(0.88))
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 15)
            }
            .aspectRatio(16 / 9.6, contentMode: .fill)
            .clipShape(RoundedRectangle(cornerRadius: MemoryRadius.card, style: .continuous))
            .overlay(alignment: .topLeading) {
                if space.kind == .personal { onlyOnThisPhone }
            }
            .overlay(alignment: .topTrailing) {
                HStack(spacing: -7) {
                    ForEach(Array(avatarLabels.enumerated()), id: \.offset) { _, label in
                        Text(label)
                            .memoryMicro()
                            .foregroundStyle(.white)
                            .frame(width: 26, height: 26)
                            .background(Circle().fill(MemoryColor.accent))
                            .overlay(Circle().strokeBorder(.white.opacity(0.92), lineWidth: 2))
                    }
                }
                .padding(14)
            }
        }
        .buttonStyle(.plain)
    }

    /// 사진이 이 기기 안에만 있다는 표시 (`docs/app/design.html` 의 '공간 목록').
    ///
    /// **다는 쪽이 예외입니다** — 같이 쓰는 짜국에는 아무것도 달지 않습니다. 둘 다 달면
    /// 목록이 딱지투성이가 되고 어느 쪽이 특별한지도 알 수 없습니다.
    ///
    /// 감빛을 쓰지 않는 이유: 누르는 것이 아니라 그냥 알려 주는 것이라서요.
    private var onlyOnThisPhone: some View {
        Text("이 폰에만")
            .memoryMicro()
            .foregroundStyle(MemoryColor.ink2)
            .padding(.horizontal, 9)
            .padding(.vertical, 3)
            .background(Capsule().fill(.ultraThinMaterial))
            .padding(14)
    }

    /// 이름 첫 글자 원. **넷을 넘으면 마지막이 `+N`** 이 됩니다 — 그냥 잘라 내면
    /// 다섯째부터는 있는지조차 안 보입니다. 안드로이드 `MemberAvatars` 와 같은 규칙입니다.
    private var avatarLabels: [String] {
        let initials = space.members.map(\.initial)
        guard initials.count > 4 else { return initials }
        return Array(initials.prefix(3)) + ["+\(initials.count - 3)"]
    }

    private var meta: String {
        var parts = ["사진 \(space.photoCount)장", "지역 \(space.regionCount)곳"]
        if let last = space.lastPhotoOn { parts.append("\(last.month)월 \(last.day)일") }
        return parts.joined(separator: " · ")
    }
}
