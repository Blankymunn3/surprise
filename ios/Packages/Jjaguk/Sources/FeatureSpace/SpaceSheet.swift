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
        .background(PlasticColor.body)
        .overlay(alignment: .top) {
            // 패미컴 스타일에서는 **몸통이 통째로 올라옵니다.** 화면(검정 판)만
            // 올라오면 기기에서 화면이 떨어져 나온 것처럼 보입니다. 그래서 손잡이도
            // 잉크 선이 아니라 몸통에 새긴 회색 홈 — 목록 위쪽 줄무늬와 같은 것입니다.
            PlasticGrip()
        }
    }

    // MARK: - 공통 부품

    /**
     시트 **몸통 위** 글자색.

     시트는 패미컴 스타일에서 회색 플라스틱이 통째로 올라오는 것이라, 그 위의 글자는
     잉크가 아니라 플라스틱에 새긴 검정입니다. 자리마다 조건문을 쓰면 시트 넷이
     지저분해져서 여기 두 개로 모읍니다. 안드로이드 `bodyInk`/`bodyDim` 과 같습니다.
     */
    private var bodyInk: Color { PlasticColor.ink }

    /// 몸통 위의 흐린 글자 (설명·각주)
    private var bodyDim: Color { PlasticColor.trimLo }

    /// 시트 제목. 뒤로 버튼을 두지 않습니다 — 시트는 끌어 내려 닫습니다.
    private func sheetTitle(_ title: String) -> some View {
        Text(title)
            .memoryTitle()
            .foregroundStyle(bodyInk)
            .padding(.bottom, MemorySpace.m)
    }

    /// "어떻게 쓸까요" 같은 구역 이름표.
    private func sectionLabel(_ text: String) -> some View {
        Text(text)
            .memoryMicro()
            .foregroundStyle(bodyDim)
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
                    .foregroundStyle(bodyDim)
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
                    .foregroundStyle(bodyDim)
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
            Rectangle().fill(PlasticColor.red)
                .frame(width: 13, height: 13)
                .padding(.bottom, 14)

            // 시트에서는 25단이 너무 큽니다 — 제목만으로 시트가 반을 먹습니다.
            Text(localized("signin_title")).memoryTitle().foregroundStyle(bodyInk)

            Text(localized("signin_why"))
                .memoryLabel()
                .foregroundStyle(bodyDim)
                .lineSpacing(4)
                .padding(.top, MemorySpace.s)
                .padding(.bottom, MemorySpace.xl)

            // 구글 버튼만 **레드가 아닙니다.** 남의 서비스로 넘어가는 문이라 앱의
            // 강조색을 입히면 우리가 하는 일처럼 보입니다. 구글이 권하는 모양이기도 합니다.
            //
            // 패미컴 스타일에서도 **흰 면 그대로 둡니다.** 다른 버튼은 다 고무·플라스틱이
            // 됐지만 이 버튼만은 구글이 정한 모양을 지켜야 합니다. 하우징에 앉혀 기기에
            // 달린 것처럼 보이게 하되, 버튼 얼굴 자체는 손대지 않습니다.
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
                .padding(.vertical, 13)
                .modifier(GoogleFace())
            }
            .buttonStyle(.plain)
            .disabled(store.state.working)
            .modifier(GoogleHousing())

            // 애플 버튼. 스토어 심사 요건입니다 — 남의 로그인(구글)을 두면 애플
            // 로그인도 있어야 합니다(지침 4.8). 애플이 정한 검정 얼굴을 지키고,
            // 구글과 같은 하우징에 앉혀 **같은 급의 문 둘**로 보이게 합니다.
            Button {
                Task { await store.send(.appleSignInTapped) }
            } label: {
                HStack(spacing: 11) {
                    Image(systemName: "apple.logo")
                        .font(.system(size: 17, weight: .medium))
                        .foregroundStyle(.white)
                        .frame(width: 19, height: 19)
                    Text(localized(store.state.working ? "signin_working" : "signin_apple"))
                        .memoryHeadline()
                        .foregroundStyle(.white)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, MemorySpace.l)
                .padding(.vertical, 13)
                .background(Color.black)
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .disabled(store.state.working)
            .modifier(GoogleHousing())
            .padding(.top, MemorySpace.s)

            // 만들기에서 왔을 때만 빠져나갈 길을 둡니다. 참여로 왔으면 혼자로 갈 곳이
            // 없습니다 — 남의 짜국에 혼자 들어갈 수는 없으니까요.
            if next == .create {
                Button {
                    Task { await store.send(.signInGaveUp) }
                } label: {
                    Text(localized("signin_give_up"))
                        .memoryBody()
                        .foregroundStyle(bodyInk)
                        .underline()
                }
                .buttonStyle(.plain)
                .padding(.top, 14)
            }

            Text(localized("signin_providers"))
                .memoryMicro()
                .foregroundStyle(bodyDim)
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
            Text(localized("invited_title", space.name)).memoryTitle().foregroundStyle(bodyInk)

            Text(localized("invited_note"))
                .memoryLabel()
                .foregroundStyle(bodyDim)
                .lineSpacing(3)
                .padding(.top, 10)

            // 코드는 글자 단 밖입니다 — UI 글이 아니라 화면의 주인공(콘텐츠)이라서요.
            // 자간을 넓게 벌려 한 글자씩 옮겨 적기 쉽게 합니다.
            // 패미컴 스타일에서는 코드를 **화면에 띄웁니다.** 검정 판은 "기기가
            // 보여 주는 것" 이고, 이 코드야말로 기기가 방금 만들어 낸 값입니다.
            Text(code)
                .font(MemoryFont.font(32, .bold))
                .tracking(8)
                .foregroundStyle(PlasticColor.onPlate)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, MemorySpace.xl)
                .padding(.vertical, 18)
                .modifier(CodePlate())
                .padding(.top, 18)

            HStack(spacing: MemorySpace.s) {
                SoftButton(localized("invited_copy")) {
                    #if os(iOS)
                    UIPasteboard.general.string = code
                    #endif
                }
                // 옆의 '복사'(SoftButton)와 **같은 고무 알약**이어야 합니다 —
                // ShareLink 는 자체 뷰라 컴포넌트를 못 쓰고 얼굴만 같게 그립니다.
                ShareLink(item: code) {
                    Text(localized("invited_share"))
                        .font(MemoryFont.font(15, .bold))
                        .foregroundStyle(PlasticColor.onRubber)
                        .frame(maxWidth: .infinity)
                        .frame(height: PlasticSize.button)
                        .background(Capsule().fill(PlasticColor.rubber))
                }
                .buttonStyle(.plasticPress)
                .padding(PlasticSize.buttonInset)
                .raisedPlastic()
            }
            .padding(.top, 10)

            Text(localized("invited_menu_hint"))
                .memoryMicro()
                .foregroundStyle(bodyDim)
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

        // 패미컴 스타일에서는 **화면 안에 끼운 칸**입니다. 고른 칸만 왼쪽에 빨간 막대가
        // 서고 바닥이 밝아집니다 — 달력의 '고른 날' 과 같은 규칙입니다. 라디오 네모는
        // 뺐습니다: 표식이 셋이 되면 무엇을 봐야 할지 흐려집니다.
        return AnyView(
            Button {
                Task { await store.send(.kindSelected(kind)) }
            } label: {
                HStack(alignment: .top, spacing: MemorySpace.m) {
                    Rectangle()
                        .fill(checked ? PlasticColor.red : Color.clear)
                        .frame(width: 3)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(title).memoryHeadline().foregroundStyle(PlasticColor.onPlate)
                        Text(detail)
                            .memoryLabel()
                            .foregroundStyle(PlasticColor.onPlateDim)
                            .padding(.top, 3)
                        Text(sub).memoryLabel().foregroundStyle(PlasticColor.onPlateDim)
                    }
                    Spacer(minLength: 0)
                }
                .fixedSize(horizontal: false, vertical: true)
                .padding(.trailing, 15)
                .padding(.vertical, 13)
                .background(checked ? PlasticColor.plateHi : PlasticColor.plate)
                .clipShape(
                    RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                )
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(checked ? [.isButton, .isSelected] : .isButton)
        )

        return AnyView(
            Button {
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
                            .foregroundStyle(bodyDim)
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
        )
    }

    // MARK: - 공통

    /**
     글자칸. 흰 면에 1px 잉크 선 — 회색 면을 쓰지 않습니다.

     패미컴 스타일에서는 **카트리지 슬롯**입니다. 지도 검색칸·올리기의 어디·언제와
     같은 모양인데, 셋 다 "값을 꽂아 넣는 자리" 라서 같아야 합니다.
     */
    private func field(placeholder: String, text: Binding<String>, code: Bool = false) -> some View {
        let input = TextField(
            "",
            text: text,
            prompt: Text(placeholder)
                .foregroundStyle(PlasticColor.onPlateDim)
        )
        .textFieldStyle(.plain)
        .memoryBody()
        .autocorrectionDisabled(code)
        .modifier(CodeInput(enabled: code))

        return AnyView(
            HStack(spacing: MemorySpace.s) {
                RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                    .fill(PlasticColor.ink)
                    .frame(width: 3, height: 14)
                input
                    .foregroundStyle(PlasticColor.onPlate)
                    .tint(PlasticColor.red)
            }
            .padding(.horizontal, MemorySpace.m)
            .padding(.vertical, 12)
            .sunken(PlasticRadius.chip, face: PlasticColor.plateLo)
        )

        return AnyView(
            input
                .padding(.horizontal, 14)
                .padding(.vertical, 13)
                .background(MemoryColor.surface)
                .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
        )
    }
}

/// 구글 버튼의 얼굴. 흰 면은 두 스타일에서 같고, 테두리 대신 알약으로 잘립니다.
private struct GoogleFace: ViewModifier {
    func body(content: Content) -> some View {
        content.background(MemoryColor.surface).clipShape(Capsule())
    }
}

/// 구글 버튼을 기기에 달린 것처럼 보이게 하는 하우징. 기준 스타일에서는 없습니다.
private struct GoogleHousing: ViewModifier {
    func body(content: Content) -> some View {
        content.padding(PlasticSize.buttonInset).raisedPlastic()
    }
}

/// 초대 코드가 놓이는 면. 패미컴 스타일이면 끼운 화면, 아니면 흰 면에 잉크 선입니다.
private struct CodePlate: ViewModifier {
    func body(content: Content) -> some View {
        content.sunken(PlasticRadius.screen)
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
