package kr.jjaguk

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * 무슨 일이 있었는지 남기는 자리 — Analytics(GA4) 로 흘러갑니다.
 *
 * 화면·뷰모델은 Firebase 를 모릅니다. `(이벤트, 값들) -> Unit` 클로저만 받고,
 * 그것이 어디로 가는지는 **조립부가 정합니다** — 서버 값들과 같은 규칙입니다.
 *
 * 이벤트 이름이 `_failed` 로 끝나면 Crashlytics 에도 비치명(non-fatal)으로
 * 남깁니다 — 사용자는 조용히 지나친 실패를 대시보드에서 셀 수 있어야 합니다.
 */
class FirebaseTracker(context: Context) {

    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun track(event: String, params: Map<String, String>) {
        val bundle = Bundle()
        params.forEach { (key, value) -> bundle.putString(key, value) }
        analytics.logEvent(event, bundle)

        if (event.endsWith("_failed")) {
            crashlytics.log("$event $params")
            crashlytics.recordException(TrackedFailure(event))
        }
    }
}

/** 크래시가 아니라 **조용한 실패**를 세는 표식. 스택은 의미 없고 이름이 전부입니다. */
class TrackedFailure(event: String) : Exception(event)
