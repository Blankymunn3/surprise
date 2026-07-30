package kr.surprise.memorymap.data.space

import kotlinx.serialization.Serializable

/** 저장소의 `spaces/<공간ID>/space.json`. 두 폰이 같은 이름·멤버를 보게 하려고 서버에 둡니다. */
@Serializable
internal data class SpaceDocument(
    val name: String,
    val members: List<MemberDocument> = emptyList(),
)

@Serializable
internal data class MemberDocument(
    val uid: String,
    val displayName: String,
    val owner: Boolean = false,
)

/** 기기에만 두는 것 — 내가 어느 공간에 들어가 있는지와 내 이름. */
@Serializable
internal data class LocalMembership(
    val uid: String,
    val displayName: String,
    val spaceIds: List<String> = emptyList(),
)
