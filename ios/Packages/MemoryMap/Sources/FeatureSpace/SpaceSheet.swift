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
        .background(plasticTrial ? PlasticColor.body : MemoryColor.surface)
        .overlay(alignment: .top) {
            if plasticTrial {
                // 패미컴 스타일에서는 **몸통이 통째로 올라옵니다.** 화면(검정 판)만
                // 올라오면 기기에서 화면이 떨어져 나온 것처럼 보입니다. 그래서 손잡이도
                // 잉크 선이 아니라 몸통에 새긴 회색 홈 — 목록 위쪽 줄무늬와 같은 것입니다.
                Capsule()
                    .fill(PlasticColor.trim)
                    .frame(width: PlasticSize.grip, height: PlasticSize.stripe)
                    .padding(.top, MemorySpace.s)
            } else {
                // 시트 위쪽 2px 잉크 선. 지역 시트도 같은 선으로 시작합니다 —
                // 이 디자인에는 둥근 손잡이 막대가 들어갈 자리가 없습니다.
                MemoryColor.ink.frame(height: MemoryStroke.divider)
            }
        }
    }

    // MARK: - 공통 부품

    /**
     시트 **몸통 위** 글자색.

     시트는 패미컴 스타일에서 회색 플라스틱이 통째로 올라오는 것이라, 그 위의 글자는
     잉크가 아니라 플라스틱에 새긴 검정입니다. 자리마다 조건문을 쓰면 시트 넷이
     지저분해져서 여기 두 개로 모읍니다. 안드로이드 `bodyInk`/`bodyDim` 과 같습니다.
     */
    private var bodyInk: Color { plasticTrial ? PlasticColor.ink : MemoryColor.ink }

    /// 몸통 위의 흐린 글자 (설명·각주)
    private var bodyDim: Color { plasticTrial ? PlasticColor.trimLo : MemoryColor.ink2 }

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
            sheetTitle("새 짜국")

            VStack(alignment: .leading, spacing: 0) {
                sectionLabel("어떻게 쓸까요").padding(.bottom, MemorySpace.s)

                kindOption(
                    .personal,
                    title: "혼자",
                    detail: "사진이 이 폰에만 남아요 · 로그인 없이 바로 시작해요",
                    sub: "폰을 잃어버리면 사진도 함께 사라져요"
                )
                .padding(.bottom, 9)

                kindOption(
                    .shared,
                    title: "같이",
                    detail: "초대한 사람들과 같이 채우고 같이 봐요 · 로그인이 필요해요",
                    sub: "만들면 초대 코드가 바로 나와요"
                )

                sectionLabel("이름")
                    .padding(.top, MemorySpace.xl)
                    .padding(.bottom, MemorySpace.s)

                field(
                    placeholder: "예) 우리 여름, 제주 한 달",
                    text: Binding(
                        get: { store.state.pendingName },
                        set: { value in Task { await store.send(.nameTyped(value)) } }
                    )
                )
            }

            PrimaryButton(
                store.state.working ? "만드는 중…" : "만들기",
                enabled: store.state.canCreate
            ) {
                Task { await store.send(.createConfirmed) }
            }
            .padding(.top, MemorySpace.xl)

            if store.state.pendingKind == .personal {
                Text("로그인 없이 바로 만들어져요.")
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
            sheetTitle("초대 코드로 참여")

            VStack(alignment: .leading, spacing: MemorySpace.s) {
                sectionLabel("받은 코드")
                field(
                    placeholder: "예) K7QF2M",
                    text: Binding(
                        get: { store.state.pendingCode },
                        set: { value in Task { await store.send(.codeTyped(value.uppercased())) } }
                    ),
                    // 코드는 영문·숫자라 자동 대문자·자동 수정이 방해만 됩니다.
                    code: true
                )
                Text("코드를 보낸 사람의 짜국에 멤버로 들어가요.")
                    .memoryMicro()
                    .foregroundStyle(bodyDim)
            }

            PrimaryButton(
                store.state.working ? "찾는 중…" : "참여하기",
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
            Rectangle().fill(plasticTrial ? PlasticColor.red : MemoryColor.accent)
                .frame(width: 13, height: 13)
                .padding(.bottom, 14)

            // 시트에서는 25단이 너무 큽니다 — 제목만으로 시트가 반을 먹습니다.
            Text("같이 보려면 계정이 필요해요").memoryTitle().foregroundStyle(bodyInk)

            Text("로그인은 멤버가 아닌 사람이 우리 사진을 못 보게 막는 잠금장치예요. 계정 확인 말고 다른 데는 쓰지 않아요.")
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
                    Text(store.state.working ? "로그인 중…" : "Google로 계속하기")
                        .memoryHeadline()
                        .foregroundStyle(MemoryColor.ink)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, MemorySpace.l)
                .padding(.vertical, plasticTrial ? 13 : 14)
                .modifier(GoogleFace())
            }
            .buttonStyle(.plain)
            .disabled(store.state.working)
            .modifier(GoogleHousing())

            // 만들기에서 왔을 때만 빠져나갈 길을 둡니다. 참여로 왔으면 혼자로 갈 곳이
            // 없습니다 — 남의 짜국에 혼자 들어갈 수는 없으니까요.
            if next == .create {
                Button {
                    Task { await store.send(.signInGaveUp) }
                } label: {
                    Text("그냥 혼자 쓸래요 — 이 폰에만 저장할게요")
                        .memoryBody()
                        .foregroundStyle(bodyInk)
                        .underline()
                }
                .buttonStyle(.plain)
                .padding(.top, 14)
            }

            Text("지금은 Google 계정만 돼요.")
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
            Text("'\(space.name)' 짜국을 만들었어요").memoryTitle().foregroundStyle(bodyInk)

            Text("이 코드를 받은 사람이 들어올 수 있어요. 지금 보내 두면 나중에 다시 찾지 않아도 돼요.")
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
                .foregroundStyle(plasticTrial ? PlasticColor.onPlate : MemoryColor.ink)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, MemorySpace.xl)
                .padding(.vertical, 18)
                .modifier(CodePlate())
                .padding(.top, 18)

            HStack(spacing: MemorySpace.s) {
                SoftButton("코드 복사") {
                    #if os(iOS)
                    UIPasteboard.general.string = code
                    #endif
                }
                ShareLink(item: code) {
                    Text("공유하기")
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

            Text("위쪽 ⋯ 메뉴에서 언제든 다시 볼 수 있어요.")
                .memoryMicro()
                .foregroundStyle(bodyDim)
                .padding(.top, MemorySpace.m)

            PrimaryButton("짜국 열기") {
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
        if plasticTrial {
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
        }

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
                .foregroundStyle(plasticTrial ? PlasticColor.onPlateDim : MemoryColor.ink3)
        )
        .textFieldStyle(.plain)
        .memoryBody()
        .autocorrectionDisabled(code)
        .modifier(CodeInput(enabled: code))

        if plasticTrial {
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
        }

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
        if plasticTrial {
            content.background(MemoryColor.surface).clipShape(Capsule())
        } else {
            content
                .background(MemoryColor.surface)
                .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
        }
    }
}

/// 구글 버튼을 기기에 달린 것처럼 보이게 하는 하우징. 기준 스타일에서는 없습니다.
private struct GoogleHousing: ViewModifier {
    func body(content: Content) -> some View {
        if plasticTrial {
            content.padding(PlasticSize.buttonInset).raisedPlastic()
        } else {
            content
        }
    }
}

/// 초대 코드가 놓이는 면. 패미컴 스타일이면 끼운 화면, 아니면 흰 면에 잉크 선입니다.
private struct CodePlate: ViewModifier {
    func body(content: Content) -> some View {
        if plasticTrial {
            content.sunken(PlasticRadius.screen)
        } else {
            content
                .background(MemoryColor.surface)
                .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
        }
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
