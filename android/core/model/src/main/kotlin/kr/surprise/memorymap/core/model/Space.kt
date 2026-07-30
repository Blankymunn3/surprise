package kr.surprise.memorymap.core.model

/** 지도 하나. 초대한 사람들과 함께 채웁니다. (`docs/app/SPACES.md`) */
data class Space(
    val id: SpaceId,
    val name: String,
    val members: List<Member>,
    val photoCount: Int,
    val regionCount: Int,
    val coverPhotoUrl: String?,
    val lastPhotoOn: java.time.LocalDate?,
)

data class Member(
    val uid: String,
    val displayName: String,
    val role: MemberRole,
) {
    /** 프로필 사진 대신 쓰는 이름 첫 글자. 둘~다섯 명짜리 앱에 사진 동기화는 과합니다. */
    val initial: String get() = displayName.take(1).ifBlank { "?" }
}

enum class MemberRole { Owner, Member }

/**
 * 초대 코드. 공간을 만들 때 바로 하나 생깁니다 —
 * 만들고 나서 "초대하기" 를 다시 찾게 하지 않으려는 것입니다.
 */
data class Invite(
    val code: String,
    val spaceId: SpaceId,
    val expiresAtEpochSeconds: Long,
    val maxUses: Int,
    val usedCount: Int,
) {
    fun isUsable(nowEpochSeconds: Long): Boolean =
        nowEpochSeconds < expiresAtEpochSeconds && usedCount < maxUses
}
