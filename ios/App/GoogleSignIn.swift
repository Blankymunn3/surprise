import CoreCommon
import GoogleSignIn
import UIKit

/// 구글 계정을 골라 **ID 토큰만** 받아 옵니다. 그 토큰을 Firebase 토큰으로 바꾸는 일은
/// `FirebaseAuthRepository` 가 합니다 (`docs/app/AUTH.md`).
///
/// **앱 껍데기에 있는 이유**: 로그인 창을 띄우려면 `UIViewController` 가 필요합니다.
/// `Packages/MemoryMap` 은 맥에서도 빌드돼야 해서 UIKit 을 쓰지 않습니다 — 창을 띄우는
/// 여기까지만 앱이 맡고, 아래로는 문자열 하나만 내려보냅니다.
/// 안드로이드 `GoogleSignIn.kt` 와 같은 역할입니다.
enum GoogleSignInBridge {

    /// 사용자가 창을 닫으면 실패가 아니라 **`cancelled`** 입니다 —
    /// 스스로 그만둔 것을 "실패했어요" 라고 띄우면 안 되기 때문입니다.
    enum Result {
        case token(String)
        case cancelled
        case failed(Failure)
    }

    /// `clientID` 는 `GoogleService-Info.plist` 의 `CLIENT_ID` 입니다.
    /// 안드로이드와 달리 iOS 는 **자기 클라이언트 ID** 를 씁니다(web 클라이언트가 아닙니다).
    @MainActor
    static func idToken(clientID: String) async -> Result {
        guard let presenter = topViewController() else { return .failed(.unknown) }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)

        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
            guard let token = result.user.idToken?.tokenString else { return .failed(.unknown) }
            return .token(token)
        } catch let error as NSError where error.code == GIDSignInError.canceled.rawValue {
            return .cancelled
        } catch {
            return .failed(.network)
        }
    }

    /// 로그인 창을 띄울 자리. 시트가 이미 떠 있으면 **그 위에** 띄워야 합니다 —
    /// 루트에 띄우면 시트에 가려 아무 일도 안 일어난 것처럼 보입니다.
    @MainActor
    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }

        var top = scene?.windows.first { $0.isKeyWindow }?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
