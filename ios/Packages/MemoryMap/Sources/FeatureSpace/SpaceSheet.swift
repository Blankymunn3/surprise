import CoreModel
import DesignSystem
import Foundation
import SwiftUI

/**
 공간 만들기 · 초대 코드로 참여 · 만든 뒤 초대 코드 보여 주기.

 셋 다 같은 시트 자리에서 뜹니다. 어느 것을 띄울지는 **상태가 정하고**
 (`SpaceListSheet`) 화면은 그대로 따라갑니다. 안드로이드 `SpaceListScreen` 과 같습니다.
 */
struct SpaceSheet: View {
    let store: SpaceListStore

    var body: some View {
        VStack(alignment: .leading, spacing: MemorySpace.l) {
            switch store.state.sheet {
            case .create: create
            case .join: join
            case .invited(let name, let code): invited(name: name, code: code)
            case .none: EmptyView()
            }
        }
        .padding(.horizontal, MemorySpace.xl)
        .padding(.top, MemorySpace.xxl)
        // 아래 여백은 홈 인디케이터를 피할 만큼만. `maxHeight: .infinity` 를 주면
        // 시트가 화면을 채우려 들어서, 높이를 재 봐야 늘 같은 값이 나옵니다.
        .padding(.bottom, MemorySpace.xxxl)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MemoryColor.surface)
    }

    // MARK: - 만들기

    @ViewBuilder
    private var create: some View {
        Text("새 짜국 만들기").memoryTitle()
        Text("사진을 어디에 둘지 먼저 고릅니다")
            .memoryLabel()
            .foregroundStyle(MemoryColor.ink2)

        kindPicker

        field(
            placeholder: "우리 추억 지도",
            text: Binding(
                get: { store.state.pendingName },
                set: { value in Task { await store.send(.nameTyped(value)) } }
            )
        )

        PrimaryButton(store.state.working ? "만드는 중…" : "만들기", enabled: store.state.canCreate) {
            Task { await store.send(.createConfirmed) }
        }
    }

    // MARK: - 참여

    @ViewBuilder
    private var join: some View {
        Text("초대 코드로 참여").memoryTitle()
        Text("받은 여섯 글자를 넣어 주세요.")
            .memoryLabel()
            .foregroundStyle(MemoryColor.ink2)

        field(
            placeholder: "K7QF2M",
            text: Binding(
                get: { store.state.pendingCode },
                set: { value in Task { await store.send(.codeTyped(value.uppercased())) } }
            ),
            // 코드는 영문·숫자라 자동 대문자·자동 수정이 방해만 됩니다.
            code: true
        )

        PrimaryButton(store.state.working ? "찾는 중…" : "참여하기", enabled: store.state.canJoin) {
            Task { await store.send(.joinConfirmed) }
        }
    }

    // MARK: - 만든 직후

    @ViewBuilder
    private func invited(name: String, code: String) -> some View {
        Text("\(name) 을(를) 만들었어요").memoryTitle()
        Text("이 코드를 보내면 같은 짜국을 같이 보게 돼요.")
            .memoryLabel()
            .foregroundStyle(MemoryColor.ink2)

        // 코드가 곧 공간이라 **크게** 보여 줍니다. 다시 찾게 하지 않으려고요.
        HStack {
            Text(code)
                .font(.system(size: 30, weight: .bold, design: .monospaced))
                .foregroundStyle(MemoryColor.accent)
            Spacer()
            ShareLink(item: code) {
                Image(systemName: "square.and.arrow.up")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(MemoryColor.ink2)
            }
        }
        .padding(.horizontal, MemorySpace.l)
        .padding(.vertical, MemorySpace.m)
        .background(
            RoundedRectangle(cornerRadius: MemoryRadius.button, style: .continuous)
                .fill(MemoryColor.accentTint)
        )

        PrimaryButton("닫기") {
            Task { await store.send(.sheetDismissed) }
        }
    }

    // MARK: - 혼자 / 같이

    /// **세로로 쌓는 이유**: 줄마다 설명이 한 줄씩 붙습니다. `지도|달력` 같은 알약에는
    /// 설명이 안 들어가고, 설명 없이 두면 사진이 폰 밖으로 나가는지 모르고 고르게 됩니다.
    /// (`docs/app/design.html` 의 '짜국 만들기')
    private var kindPicker: some View {
        VStack(spacing: MemorySpace.s) {
            kindOption(
                .personal,
                title: "혼자 쓸래요",
                detail: "사진이 이 폰에만 있어요 · 로그인 없이 바로"
            )
            kindOption(
                .shared,
                title: "같이 볼래요",
                detail: "초대한 사람들과 같이 봐요 · 로그인이 필요해요"
            )
        }
    }

    private func kindOption(_ kind: SpaceKind, title: String, detail: String) -> some View {
        let checked = store.state.pendingKind == kind
        return Button {
            Task { await store.send(.kindSelected(kind)) }
        } label: {
            HStack(alignment: .top, spacing: MemorySpace.m) {
                ZStack {
                    Circle().fill(MemoryColor.surface)
                    Circle().strokeBorder(checked ? MemoryColor.accent : MemoryColor.line, lineWidth: 1.5)
                    if checked {
                        Circle().fill(MemoryColor.accent).frame(width: 7, height: 7)
                    }
                }
                .frame(width: 16, height: 16)
                .padding(.top, 2)

                VStack(alignment: .leading, spacing: 1) {
                    Text(title).memoryHeadline()
                    Text(detail).memoryLabel().foregroundStyle(MemoryColor.ink2)
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, MemorySpace.l)
            .padding(.vertical, MemorySpace.m)
            .background(
                RoundedRectangle(cornerRadius: MemoryRadius.button, style: .continuous)
                    .fill(checked ? MemoryColor.accentTint : MemoryColor.fill)
            )
            // 테두리를 **안쪽에** 그립니다. 바깥에 두면 고를 때마다 칸이 커졌다 작아져
            // 두 줄이 흔들립니다.
            .overlay(
                RoundedRectangle(cornerRadius: MemoryRadius.button, style: .continuous)
                    .strokeBorder(checked ? MemoryColor.accent : .clear, lineWidth: 1.5)
            )
        }
        .buttonStyle(.plain)
        .foregroundStyle(MemoryColor.ink)
        .accessibilityAddTraits(checked ? [.isButton, .isSelected] : .isButton)
    }

    // MARK: - 공통

    private func field(placeholder: String, text: Binding<String>, code: Bool = false) -> some View {
        TextField(placeholder, text: text)
            .textFieldStyle(.plain)
            .memoryBody()
            .autocorrectionDisabled(code)
            .modifier(CodeInput(enabled: code))
            .padding(.horizontal, MemorySpace.l)
            .padding(.vertical, MemorySpace.m)
            .background(
                RoundedRectangle(cornerRadius: MemoryRadius.button, style: .continuous)
                    .fill(MemoryColor.fill)
            )
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
