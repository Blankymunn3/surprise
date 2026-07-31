import CoreModel
import DesignSystem
import Foundation
import SwiftUI

/// 앱의 메인. **공간이 하나뿐이어도 여기서 시작합니다.**
/// 카드는 사진이 전부입니다 — 공간을 알아보는 건 이름이 아니라 사진이라서요.
public struct SpaceListView: View {
    @State private var store: SpaceListStore
    private let onOpen: (SpaceId) -> Void

    public init(store: SpaceListStore, onOpen: @escaping (SpaceId) -> Void) {
        self._store = State(initialValue: store)
        self.onOpen = onOpen
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MemorySpace.m) {
                HStack {
                    Text("공간").memoryDisplay()
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
                        hint("아직 공간이 없어요. 하나 만들어 볼까요?")
                    }
                    ForEach(items) { space in
                        SpaceCardView(space: space) { onOpen(space.spaceId) }
                            .padding(.horizontal, MemorySpace.xl)
                    }
                }

                actionRow("새 공간 만들기", system: "plus", tinted: true) {
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
                .presentationDetents([.medium])
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
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        MemoryColor.fill
                    }
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
            .overlay(alignment: .topTrailing) {
                HStack(spacing: -7) {
                    ForEach(space.members.prefix(4)) { member in
                        Text(member.initial)
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

    private var meta: String {
        var parts = ["사진 \(space.photoCount)장", "지역 \(space.regionCount)곳"]
        if let last = space.lastPhotoOn { parts.append("\(last.month)월 \(last.day)일") }
        return parts.joined(separator: " · ")
    }
}
