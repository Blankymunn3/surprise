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

/**
 * 기기에만 두는 것 — 내가 어느 짜국에 들어가 있는지와 내 이름.
 *
 * [spaceIds] 는 **같이 쓰는 짜국**만 담습니다. 이 값이 생기기 전의 옛 데이터는 전부
 * 서버에 문서를 만들며 들어온 것이라 그대로 두면 맞습니다 — 옛 ID 를 혼자로 읽으면
 * 이미 서버에 있는 사진이 앱에서 사라집니다.
 */
@Serializable
internal data class LocalMembership(
    val uid: String,
    val displayName: String,
    val spaceIds: List<String> = emptyList(),
    /** 혼자 쓰는 짜국. 서버에 문서가 없어 **이름도 여기에** 둡니다. */
    val personal: List<PersonalSpace> = emptyList(),
)

@Serializable
internal data class PersonalSpace(
    val id: String,
    val name: String,
)
