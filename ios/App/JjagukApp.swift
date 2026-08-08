import DesignSystem
import FirebaseAppCheck
import FirebaseCore
import FirebaseMessaging
import SwiftUI
import UserNotifications

/**
 앱의 시작점.

 여기는 **껍데기**입니다 — 화면과 규칙은 전부 `Packages/Jjaguk` 안에 있고,
 이 타깃이 하는 일은 셋뿐입니다: 조립(`AppContainer`), 첫 화면(`RootView`),
 그리고 iOS 에서만 되는 것들(폰트 등록·사진 고르기)을 챙기는 것.

 안드로이드의 `app/` 모듈과 같은 자리입니다.
 */
@main
struct JjagukApp: App {
    @UIApplicationDelegateAdaptor(PushDelegate.self) private var pushDelegate

    init() {
        // App Check 제공자는 **configure 보다 먼저** 심어야 합니다. 디버그 빌드는
        // 기기 인증(DeviceCheck)을 못 쓰는 자리(시뮬레이터)가 있어 디버그 제공자
        // (콘솔에 등록한 토큰으로 통과)를 씁니다 — 안드로이드와 같은 갈래.
        #if DEBUG
        AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
        #else
        AppCheck.setAppCheckProviderFactory(DeviceCheckProviderFactory())
        #endif

        // 관측(크래시·사용 통계·성능)만 Firebase SDK 를 씁니다 — 크래시 캡처와
        // 세션 관리는 REST 로 대신할 수 없는 일입니다. 데이터 경로(Firestore·
        // Storage·Auth)는 여전히 REST 직접 호출입니다. dev 빌드는 prod 의
        // GoogleService-Info.plist 로 붙습니다 — 번들 불일치 경고가 나지만
        // 동작하고, dev 프로젝트가 생기면 plist 를 갈아 끼웁니다.
        FirebaseApp.configure()
        RemoteTuning.start()
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

/**
 푸시(새 사진 알림)의 배선. SwiftUI 앱이라 AppDelegate 가 없어서 이 어댑터가
 그 자리를 맡습니다 — APNs 등록과 FCM 토큰 수신뿐, 알림 내용은 서버가 만듭니다
 (`functions/index.js` 의 notifyPhoto).

 APNs 토큰 → Firebase 전달은 SDK 의 기본 프록시(swizzling)가 합니다.
 여기서 받는 것은 그 결과인 **FCM 등록 토큰**이고, 서버 문서로 적는 일은
 `PushTokens` 가 합니다.
 */
final class PushDelegate: NSObject, UIApplicationDelegate, MessagingDelegate,
    UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self

        // 답이 무엇이든 앱은 그대로 돕니다 — 알림만 안 뜰 뿐입니다.
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
        application.registerForRemoteNotifications()
        return true
    }

    /// 토큰이 처음 나오거나 돌 때. 로그인 전이면 `PushTokens` 가 조용히 물러납니다.
    /// `nonisolated`: 이 프로토콜은 어느 스레드에서 부를지 약속하지 않아
    /// MainActor 클래스에 그냥 두면 Swift 6 가 막습니다 — 안에서 홉합니다.
    nonisolated func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        Task { await AppContainer.shared.pushTokens.register(token: fcmToken) }
    }

    /// 앱을 **보고 있는 중**에도 알림을 띄웁니다 — 기본은 조용히 삼켜집니다.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}
