package kr.jjaguk

import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kr.jjaguk.core.network.Firestore
import kr.jjaguk.domain.repository.AuthRepository

/**
 * "알림은 이 기기로" 를 서버에 알립니다. 토큰은 SDK 가 주지만 **저장은 우리 REST 로**
 * 합니다(`users/{uid}/fcmTokens/{토큰}`) — 데이터 경로는 SDK 를 안 쓴다는 경계를
 * 여기서도 지킵니다. 발송은 서버(`functions/index.js` 의 notifyPhoto)가 합니다.
 *
 * 문서 ID 가 곧 토큰이라 몇 번을 불러도 문서 하나입니다. 죽은 토큰 청소는
 * 발송이 실패할 때 서버가 합니다 — 앱은 등록만 알면 됩니다.
 */
class PushTokens(
    private val firestore: Firestore,
    private val accounts: AuthRepository,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) {
    /** 로그인 전이면 조용히 물러납니다 — 어느 문서에 적을지 모르니까요. */
    suspend fun register() {
        val account = accounts.account() ?: return
        val token = currentToken() ?: return
        firestore.set(
            "users/${account.uid}/fcmTokens/$token",
            mapOf(
                "platform" to Firestore.Value.Text("android"),
                "updatedAt" to Firestore.Value.Number(now()),
            ),
        )
    }

    private suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            cont.resume(if (task.isSuccessful) task.result else null)
        }
    }
}
