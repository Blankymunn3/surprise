import CoreModel
import DesignSystem
import Foundation
import SwiftUI

/**
 새 짜국 · 초대 코드로 참여 · 로그인 · 만든 뒤 초대 코드.

 넷 다 **아래에서 올라오는 시트**입니다. 목록을 잠깐 가리고 끝내는 일이라
 화면을 통째로 갈아 끼우지 않습니다. 어느 것을 띄울지는 **상태가 정하고**
 (`SpaceListSheet`) 화면은 그대로 따라갑니다. 안드로이드 `SpaceListScreen` 과 같습니다.
 */
struct SpaceSheet: View {
    let store: SpaceListStore
    /// 만든 짜국으로 바로 들어가는 길. 목록 화면이 쥐고 있습니다.
    let onOpen: (Space) -> Void

    var body: some View {
        Group {
            switch store.state.sheet {
            case .create: create
            case .join: join
            case .invited(let space, let code): invited(space: space, code: code)
            case .signIn(let next): signIn(next: next)
            case .none: EmptyView()
            }
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .padding(.horizontal, MemorySpace.xl)
        .padding(.top, MemorySpace.l)
        // 아래쪽은 넉넉히 — 홈 인디케이터 자리와 손가락이 시트를 잡는 자리를
        // 버튼과 겹치지 않게 하려는 것입니다.
        .padding(.bottom, MemorySpace.xxxl)
        .background(MemoryColor.surface)
        // 시트 위쪽 2px 잉크 선. 지역 시트도 같은 선으로 시작합니다 —
        // 이 디자인에는 둥근 손잡이 막대가 들어갈 자리가 없습니다.
        .overlay(alignment: .top) {
            MemoryColor.ink.frame(height: MemoryStroke.divider)
        }
    }

    // MARK: - 공통 부품

    /// 시트 제목. 뒤로 버튼을 두지 않습니다 — 시트는 끌어 내려 닫습니다.
    private func sheetTitle(_ title: String) -> some View {
        Text(title).memoryTitle().padding(.bottom, MemorySpace.m)
    }

    /// "어떻게 쓸까요" 같은 구역 이름표.
    private func sectionLabel(_ text: String) -> some View {
        Text(text)
            .memoryMicro()
            .foregroundStyle(MemoryColor.ink2)
            .tracking(0.7)
    }

    // MARK: - 새 짜국 (시안 2a)

    private var create: some View {
        VStack(alignment: .leading, spacing: 0) {
            sheetTitle(localized("create_title"))

            VStack(alignment: .leading, spacing: 0) {
                sectionLabel(localized("create_kind_label")).padding(.bottom, MemorySpace.s)

                kindOption(
                    .personal,
                    title: localized("create_kind_solo"),
                    detail: localized("create_kind_solo_detail"),
                    sub: localized("create_kind_solo_sub")
                )
                .padding(.bottom, 9)

                kindOption(
                    .shared,
                    title: localized("create_kind_shared"),
                    detail: localized("create_kind_shared_detail"),
                    sub: localized("create_kind_shared_sub")
                )

                sectionLabel(localized("create_name_label"))
                    .padding(.top, MemorySpace.xl)
                    .padding(.bottom, MemorySpace.s)

                field(
                    placeholder: localized("create_name_placeholder"),
                    text: Binding(
                        get: { store.state.pendingName },
                        set: { value in Task { await store.send(.nameTyped(value)) } }
                    )
                )
            }

            PrimaryButton(
                localized(store.state.working ? "create_working" : "create_confirm"),
                enabled: store.state.canCreate
            ) {
                Task { await store.send(.createConfirmed) }
            }
            .padding(.top, MemorySpace.xl)

            if store.state.pendingKind == .personal {
                Text(localized("create_solo_note"))
                    .memoryMicro()
                    .foregroundStyle(MemoryColor.ink2)
                    .padding(.top, MemorySpace.s)
            }
        }
    }

    // MARK: - 참여

    /// 시안에 따로 그려져 있지 않아 '새 짜국' 과 같은 뼈대로 갑니다.
    private var join: some View {
        VStack(alignment: .leading, spacing: 0) {
            sheetTitle(localized("join_title"))

            VStack(alignment: .leading, spacing: MemorySpace.s) {
                sectionLabel(localized("join_code_label"))
                field(
                    placeholder: localized("join_code_placeholder"),
                    text: Binding(
                        get: { store.state.pendingCode },
                        set: { value in Task { await store.send(.codeTyped(value.uppercased())) } }
                    ),
                    // 코드는 영문·숫자라 자동 대문자·자동 수정이 방해만 됩니다.
                    code: true
                )
                Text(localized("join_note"))
                    .memoryMicro()
                    .foregroundStyle(MemoryColor.ink2)
            }

            PrimaryButton(
                localized(store.state.working ? "join_working" : "join_confirm"),
                enabled: store.state.canJoin
            ) {
                Task { await store.send(.joinConfirmed) }
            }
            .padding(.top, MemorySpace.xl)
        }
    }

    // MARK: - 로그인 (시안 3a)

    private func signIn(next: SignInNext) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Rectangle().fill(MemoryColor.accent).frame(width: 13, height: 13)
                .padding(.bottom, 14)

            // 시트에서는 25단이 너무 큽니다 — 제목만으로 시트가 반을 먹습니다.
            Text(localized("signin_title")).memoryTitle()

            Text(localized("signin_why"))
                .memoryLabel()
                .foregroundStyle(MemoryColor.ink2)
                .lineSpacing(4)
                .padding(.top, MemorySpace.s)
                .padding(.bottom, MemorySpace.xl)

            // 구글 버튼만 **레드가 아닙니다.** 남의 서비스로 넘어가는 문이라 앱의
            // 강조색을 입히면 우리가 하는 일처럼 보입니다. 구글이 권하는 모양이기도 합니다.
            Button {
                Task { await store.send(.signInTapped) }
            } label: {
                HStack(spacing: 11) {
                    GoogleMark().frame(width: 19, height: 19)
                    Text(localized(store.state.working ? "signin_working" : "signin_google"))
                        .memoryHeadline()
                        .foregroundStyle(MemoryColor.ink)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, MemorySpace.l)
                .padding(.vertical, 14)
                .background(MemoryColor.surface)
                .overlay(
                    Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border)
                )
            }
            .buttonStyle(.plain)
            .disabled(store.state.working)

            // 만들기에서 왔을 때만 빠져나갈 길을 둡니다. 참여로 왔으면 혼자로 갈 곳이
            // 없습니다 — 남의 짜국에 혼자 들어갈 수는 없으니까요.
            if next == .create {
                Button {
                    Task { await store.send(.signInGaveUp) }
                } label: {
                    Text(localized("signin_give_up"))
                        .memoryBody()
                        .foregroundStyle(MemoryColor.ink)
                        .underline()
                }
                .buttonStyle(.plain)
                .padding(.top, 14)
            }

            Text(localized("signin_only_google"))
                .memoryMicro()
                .foregroundStyle(MemoryColor.ink2)
                .padding(.top, MemorySpace.l)
        }
    }

    // MARK: - 만든 직후 (시안 2b)

    /**
     뒤로 대신 아래 '짜국 열기' 로 나갑니다.

     시안의 "7일 동안 쓸 수 있어요" 는 넣지 않았습니다. 코드에 만료가 **없어서**
     사실이 아닙니다. 만료를 만들게 되면 그때 같이 넣습니다.
     */
    private func invited(space: Space, code: String) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(localized("invited_title", space.name)).memoryTitle()

            Text(localized("invited_note"))
                .memoryLabel()
                .foregroundStyle(MemoryColor.ink2)
                .lineSpacing(3)
                .padding(.top, 10)

            // 코드는 글자 단 밖입니다 — UI 글이 아니라 화면의 주인공(콘텐츠)이라서요.
            // 자간을 넓게 벌려 한 글자씩 옮겨 적기 쉽게 합니다.
            Text(code)
                .font(MemoryFont.font(32, .bold))
                .tracking(8)
                .foregroundStyle(MemoryColor.ink)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, MemorySpace.xl)
                .padding(.vertical, 18)
                .background(MemoryColor.surface)
                .overlay(
                    Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border)
                )
                .padding(.top, 18)

            HStack(spacing: MemorySpace.s) {
                SoftButton(localized("invited_copy")) {
                    #if os(iOS)
                    UIPasteboard.general.string = code
                    #endif
                }
                ShareLink(item: code) {
                    Text(localized("invited_share"))
                        .memoryBody()
                        .foregroundStyle(MemoryColor.ink)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 13)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(MemoryColor.surface)
                        .overlay(
                            Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border)
                        )
                }
                .buttonStyle(.plain)
            }
            .padding(.top, 10)

            Text(localized("invited_menu_hint"))
                .memoryMicro()
                .foregroundStyle(MemoryColor.ink2)
                .padding(.top, MemorySpace.m)

            PrimaryButton(localized("invited_open")) {
                Task { await store.send(.sheetDismissed) }
                onOpen(space)
            }
            .padding(.top, MemorySpace.l)
        }
    }

    // MARK: - 혼자 / 같이

    /**
     **세로로 쌓는 이유**: 줄마다 설명이 두 줄씩 붙습니다. `혼자|같이` 알약에는 설명이
     안 들어가고, 설명 없이 두면 사진이 폰 밖으로 나가는지 모르고 고르게 됩니다.

     고른 칸은 **2px 레드 테두리**, 아닌 칸은 1px 잉크 40%. 라디오도 원이 아니라
     **네모**입니다 — 이 디자인에 둥근 것은 없습니다.
     */
    private func kindOption(
        _ kind: SpaceKind, title: String, detail: String, sub: String
    ) -> some View {
        let checked = store.state.pendingKind == kind
        return Button {
            Task { await store.send(.kindSelected(kind)) }
        } label: {
            HStack(alignment: .top, spacing: MemorySpace.m) {
                ZStack {
                    Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border)
                    if checked {
                        Rectangle().fill(MemoryColor.accent).frame(width: 8, height: 8)
                    }
                }
                .frame(width: 16, height: 16)
                .padding(.top, 2)

                VStack(alignment: .leading, spacing: 0) {
                    Text(title).memoryHeadline()
                    Text(detail)
                        .memoryLabel()
                        .foregroundStyle(MemoryColor.ink2)
                        .padding(.top, 3)
                    Text(sub).memoryLabel().foregroundStyle(MemoryColor.ink3)
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 15)
            .padding(.vertical, 14)
            .background(MemoryColor.surface)
            .overlay(
                Rectangle().strokeBorder(
                    checked ? MemoryColor.accent : MemoryColor.line2,
                    lineWidth: checked ? 2 : MemoryStroke.border
                )
            )
        }
        .buttonStyle(.plain)
        .foregroundStyle(MemoryColor.ink)
        .accessibilityAddTraits(checked ? [.isButton, .isSelected] : .isButton)
    }

    // MARK: - 공통

    /// 글자칸. 흰 면에 1px 잉크 선 — 회색 면을 쓰지 않습니다.
    private func field(placeholder: String, text: Binding<String>, code: Bool = false) -> some View {
        TextField(placeholder, text: text)
            .textFieldStyle(.plain)
            .memoryBody()
            .autocorrectionDisabled(code)
            .modifier(CodeInput(enabled: code))
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .background(MemoryColor.surface)
            .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
    }
}

/// 자동 대문자는 iOS 에만 있습니다. 패키지가 맥에서도 빌드돼야 해서 여기서 갈라 둡니다.
private struct CodeInput: ViewModifier {
    let enabled: Bool

    func body(content: Content) -> some View {
        #if os(iOS)
        content.textInputAutocapitalization(enabled ? .characters : .sentences)
        #else
        content
        #endif
    }
}
