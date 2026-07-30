package kr.surprise.memorymap.core.common

/**
 * 도메인은 예외를 던지지 않고 이걸 돌려줍니다 (`docs/app/CONVENTIONS.md`).
 * 코틀린 기본 `Result` 를 안 쓰는 이유: 실패 이유를 [Failure] 로 좁혀 두면
 * 화면에서 `when` 이 빠짐없이 처리되는지 컴파일러가 봐 줍니다.
 */
sealed interface Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>
    data class Fail(val reason: Failure) : Outcome<Nothing>

    fun getOrNull(): T? = (this as? Ok)?.value
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Ok -> Outcome.Ok(transform(value))
    is Outcome.Fail -> this
}

enum class Failure {
    /** 연결 자체가 안 됨 */
    Network,

    /** 제한 시간을 넘김 — 목록 15초 / 업로드 25초 */
    Timeout,

    /** 서버가 없다고 함 */
    NotFound,

    /** 권한 없음 (멤버가 아니거나 로그인이 풀림) */
    Denied,

    /** 5MB 를 넘김 */
    TooLarge,

    Unknown,
}
