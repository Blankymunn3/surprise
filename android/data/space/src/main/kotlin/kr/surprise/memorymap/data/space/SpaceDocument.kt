package kr.surprise.memorymap.data.space

import kotlinx.serialization.Serializable

/**
 * 기기에만 두는 것 — **혼자 쓰는 짜국**과 그때 쓸 내 이름.
 *
 * 같이 쓰는 짜국은 여기 없습니다. Firestore 의 `users/{uid}/spaces` 가 그 목록이라
 * 기기를 바꿔도 따라옵니다 (`SharedSpaceRepository`).
 */
@Serializable
internal data class LocalMembership(
    val uid: String,
    val displayName: String,
    /** 혼자 쓰는 짜국. 서버에 문서가 없어 **이름도 여기에** 둡니다. */
    val personal: List<PersonalSpace> = emptyList(),
)

@Serializable
internal data class PersonalSpace(
    val id: String,
    val name: String,
)
