import AuthenticationServices
import CryptoKit
import FeatureSpace
import Foundation
import UIKit

/// 애플 계정을 골라 **ID 토큰만** 받아 옵니다. 그 토큰을 Firebase 토큰으로 바꾸는 일은
/// `FirebaseAuthRepository` 가 합니다 — `GoogleSignInBridge` 와 같은 역할, 같은 자리입니다.
///
/// 시스템 프레임워크(AuthenticationServices)라 받을 것이 없습니다 — 구글처럼
/// 로그인 SDK 를 얹지 않습니다.
///
/// **nonce 를 여기서 만듭니다.** 원문을 만들어 SHA-256 만 애플에 보내고, 토큰이
/// 돌아오면 원문을 서버(signInWithIdp)에 같이 냅니다 — 서버가 둘을 맞춰 보고
/// 다른 데서 가로챈 토큰이 아님을 확인합니다. 원문이 이 파일 밖으로 나가는 길은
/// 그 한 번뿐입니다.
enum AppleSignInBridge {

    /// 창을 닫으면 실패가 아니라 **`cancelled`** 입니다 — 구글 다리와 같은 규칙.
    enum Result {
        case payload(AppleSignInPayload)
        case cancelled
        case failed
    }

    @MainActor
    static func payload() async -> Result {
        let nonce = randomNonce()
        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        // 이름은 **첫 로그인에만** 옵니다. 여기서 안 받아 두면 멤버 목록에
        // 이메일 조각(가림 주소일 수도 있는)이 뜹니다.
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)

        return await withCheckedContinuation { continuation in
            let delegate = Delegate(nonce: nonce) { result in
                continuation.resume(returning: result)
            }
            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = delegate
            controller.presentationContextProvider = delegate
            // 델리게이트는 컨트롤러가 약하게만 잡습니다 — 어딘가 세게 잡아 두지 않으면
            // 창이 뜨기도 전에 사라져 콜백이 영영 안 옵니다.
            holder = delegate
            controller.performRequests()
        }
    }

    /// 진행 중인 로그인의 델리게이트. 한 번에 하나만 뜹니다.
    @MainActor private static var holder: Delegate?

    private final class Delegate: NSObject, ASAuthorizationControllerDelegate,
        ASAuthorizationControllerPresentationContextProviding {

        private let nonce: String
        private let finish: (Result) -> Void

        init(nonce: String, finish: @escaping (Result) -> Void) {
            self.nonce = nonce
            self.finish = finish
        }

        func authorizationController(
            controller: ASAuthorizationController,
            didCompleteWithAuthorization authorization: ASAuthorization
        ) {
            defer { Task { @MainActor in AppleSignInBridge.holder = nil } }
            guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                  let data = credential.identityToken,
                  let token = String(data: data, encoding: .utf8)
            else {
                finish(.failed)
                return
            }
            // 성·이름 붙이는 순서는 로캘이 압니다 — 한국은 '김건우', 서양은 사이 띄움.
            let name = credential.fullName
                .map { PersonNameComponentsFormatter.localizedString(from: $0, style: .default) }
                ?? ""
            finish(.payload(AppleSignInPayload(
                idToken: token,
                nonce: nonce,
                displayName: name.isEmpty ? nil : name
            )))
        }

        func authorizationController(
            controller: ASAuthorizationController, didCompleteWithError error: Error
        ) {
            defer { Task { @MainActor in AppleSignInBridge.holder = nil } }
            if let reason = error as? ASAuthorizationError, reason.code == .canceled {
                finish(.cancelled)
            } else {
                finish(.failed)
            }
        }

        func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
            UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .first { $0.activationState == .foregroundActive }?
                .windows.first { $0.isKeyWindow } ?? ASPresentationAnchor()
        }
    }

    /// 32바이트 난수를 16진수로. 애플 문서의 표준 재료입니다.
    private static func randomNonce() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        return bytes.map { String(format: "%02x", $0) }.joined()
    }

    private static func sha256(_ input: String) -> String {
        SHA256.hash(data: Data(input.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
    }
}
