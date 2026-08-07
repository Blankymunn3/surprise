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

            // 패미컴 스타일에서는 몸통이 통째로 올라오고 내용은 끼운 검정 화면에 놓입니다.
            // 스위치는 DesignSystem 에 하나뿐입니다.
            PlasticSpaceMenu(store: store, onClose: onClose)
        }
        .task { await store.appeared() }
    }

    @ViewBuilder
    @ViewBuilder
}
