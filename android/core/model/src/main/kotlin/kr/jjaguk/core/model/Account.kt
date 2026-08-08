package kr.jjaguk.core.model

/**
 * 로그인한 사람. **같이 쓰는 짜국에서만** 필요합니다 —
 * 혼자 쓰는 짜국은 로그인 없이 돌아갑니다 (`docs/app/AUTH.md`).
 *
 * [uid] 는 Firebase 가 준 값이고, 규칙이 "이 사람이 멤버인가" 를 볼 때 쓰는 열쇠입니다.
 * 지금까지 기기에서 만들어 쓰던 임시 uid 를 이것으로 갈아 끼웁니다.
 */
data class Account(
    val uid: String,
    val displayName: String,
    val email: String?,
) {
    /** 프로필 사진 대신 쓰는 이름 첫 글자. [Member] 와 같은 규칙입니다. */
    val initial: String get() = displayName.take(1).ifBlank { "?" }
}
