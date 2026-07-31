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
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(MemoryColor.surface)
    }

    // MARK: - 만들기

    @ViewBuilder
    private var create: some View {
        Text("새 공간 만들기").memoryTitle()
        Text("둘이 같이 채울 지도예요. 이름은 나중에 바꿀 수 있어요.")
            .memoryLabel()
            .foregroundStyle(MemoryColor.ink2)

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
        Text("상대가 알려 준 여섯 글자를 넣어 주세요.")
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
        Text("이 코드를 상대에게 알려 주면 같은 공간을 보게 돼요.")
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
