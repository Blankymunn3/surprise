package kr.jjaguk

import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.runBlocking

/**
 * 토큰이 새로 돌 때(재설치·기기 복원) 서버에 다시 알립니다.
 *
 * 알림을 **그리는 코드는 여기 없습니다** — 서버가 notification 페이로드로 보내서
 * 앱이 꺼져 있어도 시스템이 그대로 띄웁니다. 채널은 `JjagukApp` 이 만들어 둡니다.
 */
class JjagukMessagingService : FirebaseMessagingService() {

    /** 이 콜백은 백그라운드 스레드라 잠깐 기다려도 됩니다. */
    override fun onNewToken(token: String) {
        val container = (application as JjagukApp).container
        runBlocking { container.pushTokens.register() }
    }
}
