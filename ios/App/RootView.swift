import CoreModel
import DesignSystem
import FeatureSpace
import SwiftUI

/// 공간 목록에서 시작해, 공간을 고르면 `지도 | 달력` 으로 들어갑니다.
/// 안드로이드 `Navigation.kt` 와 같은 흐름입니다.
struct RootView: View {
    /// 짜국을 통째로 들고 있습니다 — 들어간 화면이 **종류**를 알아야 기기 안 사진을 볼지
    /// 서버 사진을 볼지 정할 수 있습니다. `Space` 자체가 `Identifiable` 이라 그대로 씁니다.
    @State private var opened: Space?

    var body: some View {
        NavigationStack {
            SpaceListView(store: AppContainer.shared.spaceListStore()) { space in
                opened = space
            }
            .background(MemoryColor.paper)
            .navigationDestination(item: $opened) { space in
                SpaceDetailView(spaceId: space.spaceId, kind: space.kind)
            }
        }
    }
}
