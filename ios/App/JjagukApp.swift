import DesignSystem
import FirebaseCore
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
    init() {
        // 관측(크래시·사용 통계·성능)만 Firebase SDK 를 씁니다 — 크래시 캡처와
        // 세션 관리는 REST 로 대신할 수 없는 일입니다. 데이터 경로(Firestore·
        // Storage·Auth)는 여전히 REST 직접 호출입니다. dev 빌드는 prod 의
        // GoogleService-Info.plist 로 붙습니다 — 번들 불일치 경고가 나지만
        // 동작하고, dev 프로젝트가 생기면 plist 를 갈아 끼웁니다.
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                // 다크 모드는 만들지 않았습니다. 기기 설정이 어두워도 밝은 화면 그대로 씁니다.
                .preferredColorScheme(.light)
                .tint(MemoryColor.accent)
        }
    }
}
