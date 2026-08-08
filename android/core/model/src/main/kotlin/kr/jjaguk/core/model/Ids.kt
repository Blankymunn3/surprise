package kr.jjaguk.core.model

/** 공간 하나. 사진이 이 단위로 모입니다. */
@JvmInline
value class SpaceId(val value: String)

/** 사진 하나. 파일 이름이기도 합니다 — `spaces/<공간ID>/photos/<사진ID>.jpg` */
@JvmInline
value class PhotoId(val value: String)

/**
 * 경로에 쓰이는 값이라 `[A-Za-z0-9_-]` 만 허용합니다.
 * 그 외 문자가 오면 상위 디렉터리로 빠져나갈 수 있습니다.
 * 웹의 `assets/firebase.js` 와 같은 규칙입니다.
 */
object PathSafe {
    private val ALLOWED = Regex("^[A-Za-z0-9_-]+$")

    fun isSafe(value: String): Boolean = value.isNotEmpty() && ALLOWED.matches(value)

    fun require(value: String, what: String): String {
        require(isSafe(value)) { "$what 에 쓸 수 없는 문자가 있습니다: $value" }
        return value
    }
}
