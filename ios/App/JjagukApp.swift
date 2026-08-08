import DesignSystem
import SwiftUI

/**
 앱의 시작점.

 여기는 **껍데기**입니다 — 화면과 규칙은 전부 `Packages/Jjaguk` 안에 있고,
 이 타깃이 하는 일은 셋뿐입니다: 조립(`AppContainer`), 첫 화면(`RootView`),
 그리고 iOS 에서만 되는 것들(폰트 등록·사진 고르기)을 챙기는 것.

 안드로이드의 `app/` 모듈과 같은 자리입니다.
 */
@main
struct JjagukApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
                // 다크 모드는 만들지 않았습니다. 기기 설정이 어두워도 밝은 화면 그대로 씁니다.
                .preferredColorScheme(.light)
                .tint(MemoryColor.accent)
        }
    }
}
