package kr.surprise.memorymap.core.common

/**
 * 웹과 **똑같이** 유지해야 하는 값들입니다. 웹은 `assets/firebase.js` 와
 * `map/index.html` 에 같은 숫자가 있습니다. 한쪽만 바꾸면 두 기기가 다르게 동작합니다.
 */
object Limits {
    /** 목록 조회를 이 시간에서 끊습니다. 신호가 약한 곳에서 하염없이 기다리지 않게. */
    const val LIST_TIMEOUT_MS = 15_000L

    /** 업로드를 이 시간에서 끊습니다. 끊기면 기기 저장으로 넘어가 사진을 잃지 않습니다. */
    const val UPLOAD_TIMEOUT_MS = 25_000L

    /** 올리기 전에 줄이는 최대 변 길이 */
    const val MAX_EDGE_PX = 760

    /** JPEG 품질 */
    const val JPEG_QUALITY = 72

    /** 서버 규칙(storage.rules)이 막는 크기. 넘으면 올리기 전에 걸러 냅니다. */
    const val MAX_UPLOAD_BYTES = 5L * 1024 * 1024
}
