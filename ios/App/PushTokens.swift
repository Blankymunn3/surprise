import CoreNetwork
import DataAuth
import Foundation

/**
 "알림은 이 기기로" 를 서버에 알립니다. 토큰은 SDK(`PushDelegate`)가 주지만
 **저장은 우리 REST 로** 합니다(`users/{uid}/fcmTokens/{토큰}`) — 데이터 경로는
 SDK 를 안 쓴다는 경계를 여기서도 지킵니다. 발송은 서버(`functions/index.js` 의
 notifyPhoto)가 합니다. 안드로이드 `PushTokens` 와 같은 자리.

 문서 ID 가 곧 토큰이라 몇 번을 불러도 문서 하나입니다. 죽은 토큰 청소는
 발송이 실패할 때 서버가 합니다 — 앱은 등록만 알면 됩니다.
 */
struct PushTokens: Sendable {
    let firestore: Firestore
    let accounts: FirebaseAuthRepository

    /// 로그인 전이면 조용히 물러납니다 — 어느 문서에 적을지 모르니까요.
    func register(token: String) async {
        guard let account = await accounts.account() else { return }
        _ = await firestore.set(
            "users/\(account.uid)/fcmTokens/\(token)",
            fields: [
                "platform": .text("ios"),
                "updatedAt": .number(Int(Date().timeIntervalSince1970)),
            ]
        )
    }
}
