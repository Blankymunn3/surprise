package kr.surprise.memorymap.core.model

/**
 * 짜국을 **혼자** 쓰는지 **같이** 쓰는지.
 *
 * 혼자 쓰는 짜국은 사진이 기기 안에만 있습니다. 서버도, 로그인도 안 씁니다.
 * (`docs/app/AUTH.md` 의 '혼자 쓰는 짜국은 서버에 안 올립니다')
 *
 * [Shared] 는 **두 명 전용이 아닙니다** — 초대 코드를 받은 사람은 몇이든 들어옵니다.
 */
enum class SpaceKind { Personal, Shared }

/** 지도 하나. 같이 쓰는 짜국은 초대한 사람들과 함께 채웁니다. (`docs/app/SPACES.md`) */
data class Space(
    val id: SpaceId,
    val name: String,
    val members: List<Member>,
    val photoCount: Int,
    val regionCount: Int,
    val coverPhotoUrl: String?,
    val lastPhotoOn: java.time.LocalDate?,
    /**
     * 기본이 [SpaceKind.Personal] 인 이유: 이 값이 없는 옛 데이터를 읽었을 때
     * **서버로 나가지 않는 쪽**이 안전합니다. 반대로 두면 옛 짜국이 조용히 공유로
     * 취급됩니다.
     */
    val kind: SpaceKind = SpaceKind.Personal,
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
