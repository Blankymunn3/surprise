package kr.jjaguk.data.space

import kotlin.random.Random

/**
 * 초대 코드가 곧 **공간 ID** 입니다.
 *
 * 지금은 로그인 서버가 없어서, 같은 코드를 아는 두 폰이 같은 저장소 경로를 보게 하는
 * 것이 공간을 공유하는 가장 단순한 방법입니다. 로그인이 붙으면 코드와 ID 를 분리하고
 * `invites/{code}` 문서로 옮깁니다 (`docs/app/SPACES.md`).
 *
 * ⚠️ 이 방식은 **코드를 아는 사람은 누구나** 그 공간의 사진을 보고 넣을 수 있다는 뜻입니다.
 * 지금 웹과 같은 수준의 약점이고, 로그인이 붙어야 해결됩니다.
 */
object InviteCode {

    // 0/O, 1/I 처럼 헷갈리는 글자는 뺍니다. 코드를 말로 불러 줄 일이 있습니다.
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private const val LENGTH = 6

    fun generate(random: Random = Random.Default): String =
        buildString(LENGTH) { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /** 사용자가 손으로 친 코드를 정리합니다. 소문자·공백·하이픈을 받아 줍니다. */
    fun normalize(raw: String): String? {
        val cleaned = raw.trim().uppercase().filter { it in ALPHABET }
        return if (cleaned.length == LENGTH) cleaned else null
    }
}
