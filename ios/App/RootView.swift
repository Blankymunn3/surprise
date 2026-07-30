import CoreModel
import DesignSystem
import FeatureSpace
import SwiftUI

/// 공간 목록에서 시작해, 공간을 고르면 `지도 | 달력` 으로 들어갑니다.
/// 안드로이드 `Navigation.kt` 와 같은 흐름입니다.
struct RootView: View {
    @State private var opened: SpaceId?

    var body: some View {
        NavigationStack {
            SpaceListView(store: AppContainer.shared.spaceListStore()) { spaceId in
                opened = spaceId
            }
            .background(MemoryColor.paper)
            .navigationDestination(item: $opened) { spaceId in
                SpaceDetailView(spaceId: spaceId)
            }
        }
    }
}

/// `navigationDestination(item:)` 은 `Identifiable` 을 요구합니다.
/// 공간 ID 는 그 자체가 유일한 값이라 그대로 씁니다.
extension SpaceId: @retroactive Identifiable {
    public var id: String { value }
}
