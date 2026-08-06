import CoreModel
import DesignSystem
import SwiftUI

/**
 ⋯ 하나에 **멤버 · 초대 코드 · 이름**을 다 넣습니다.

 화면을 셋으로 나누지 않는 이유: 셋 다 어쩌다 한 번 하는 일입니다. 각각 화면을
 만들면 그 화면으로 가는 길을 또 만들어야 하고, 정작 자주 쓰는 지도·달력이 밀립니다.

 아래에서 올라오는 판입니다 — 짜국을 벗어나는 것이 아니라 그 위에 잠깐 얹는 것이라
 뒤가 보여야 어디에 있는지 알 수 있습니다.
 */
public struct SpaceMenu: View {
    @State private var store: SpaceMenuStore
    private let onClose: () -> Void

    public init(store: SpaceMenuStore, onClose: @escaping () -> Void) {
        self._store = State(initialValue: store)
        self.onClose = onClose
    }

    public var body: some View {
        ZStack(alignment: .bottom) {
            MemoryColor.scrim
                .ignoresSafeArea()
                .onTapGesture { onClose() }

            VStack(alignment: .leading, spacing: 0) {
                MemoryColor.ink.frame(height: MemoryStroke.divider)

                VStack(alignment: .leading, spacing: 0) {
                    if store.state.renaming {
                        renameBody
                    } else {
                        menuBody
                    }
                }
                .padding(.horizontal, MemorySpace.xl)
                .padding(.top, 18)
                .padding(.bottom, 44)
            }
            .frame(maxWidth: .infinity)
            .background(MemoryColor.surface)
        }
        .task { await store.appeared() }
    }

    @ViewBuilder
    private var menuBody: some View {
        HStack(spacing: MemorySpace.s) {
            Text(store.state.space?.name ?? "")
                .memoryHeadline()
                .lineLimit(1)
            Spacer(minLength: 0)
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(MemoryColor.ink)
                    .frame(width: 30, height: 30)
                    .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
            }
            .buttonStyle(.plain)
            .accessibilityLabel(localized("menu_close"))
        }

        // 혼자 쓰는 짜국에는 멤버도 초대 코드도 없습니다 — 있는 척하지 않습니다.
        if let space = store.state.space, space.kind == .shared {
            sectionLabel(localized("menu_members")).padding(.top, MemorySpace.m)

            ForEach(space.members, id: \.uid) { member in
                HStack(spacing: 10) {
                    Text(member.initial)
                        .memoryMicro()
                        .foregroundStyle(MemoryColor.onAccent)
                        .frame(width: 28, height: 28)
                        .background(MemoryColor.ink)
                    Text(member.displayName).memoryBody().lineLimit(1)
                    Spacer(minLength: 0)
                    if member.role == .owner {
                        Text(localized("menu_owner"))
                            .memoryMicro()
                            .foregroundStyle(MemoryColor.ink)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .overlay(
                                Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border)
                            )
                    }
                }
                .padding(.vertical, MemorySpace.s)

                MemoryColor.fill.frame(height: MemoryStroke.border)
            }

            sectionLabel(localized("menu_invite_code")).padding(.top, 14)
            HStack(spacing: 10) {
                Text(store.state.code ?? localized("menu_invite_code_making"))
                    .font(MemoryFont.font(20, .bold))
                    .tracking(store.state.code != nil ? 4 : 0)
                    .foregroundStyle(store.state.code != nil ? MemoryColor.ink : MemoryColor.ink3)
                Spacer(minLength: 0)
                if let code = store.state.code {
                    Button {
                        #if os(iOS)
                        UIPasteboard.general.string = code
                        #endif
                    } label: {
                        Text(localized("menu_copy"))
                            .memoryMicro()
                            .foregroundStyle(MemoryColor.ink)
                            .padding(.horizontal, MemorySpace.m)
                            .padding(.vertical, 7)
                            .background(MemoryColor.surface)
                            .overlay(
                                Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }

        MemoryColor.fill.frame(height: MemoryStroke.border).padding(.top, MemorySpace.m)

        Button { store.startRenaming() } label: {
            HStack {
                Text(localized("menu_rename")).memoryBody().foregroundStyle(MemoryColor.ink2)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(MemoryColor.ink3)
            }
            .contentShape(Rectangle())
            .padding(.top, MemorySpace.m)
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var renameBody: some View {
        Text(localized("menu_rename")).memoryHeadline()

        TextField(localized("menu_rename_placeholder"), text: Binding(
            get: { store.state.pendingName },
            set: { store.typeName($0) }
        ))
        .textFieldStyle(.plain)
        .memoryBody()
        .padding(.horizontal, 14)
        .padding(.vertical, 13)
        .background(MemoryColor.surface)
        .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
        .padding(.top, MemorySpace.m)

        PrimaryButton(
            localized(store.state.working ? "menu_rename_working" : "menu_rename_confirm"),
            enabled: !store.state.pendingName.trimmingCharacters(in: .whitespaces).isEmpty
                && !store.state.working
        ) {
            Task { _ = await store.confirmRename() }
        }
        .padding(.top, MemorySpace.m)
    }

    private func sectionLabel(_ text: String) -> some View {
        Text(text)
            .memoryMicro()
            .foregroundStyle(MemoryColor.ink2)
            .tracking(0.7)
            .padding(.bottom, MemorySpace.xs)
    }
}
