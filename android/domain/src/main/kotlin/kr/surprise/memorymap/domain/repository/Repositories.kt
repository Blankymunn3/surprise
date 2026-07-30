package kr.surprise.memorymap.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.Cover
import kr.surprise.memorymap.core.model.CoverKey
import kr.surprise.memorymap.core.model.Invite
import kr.surprise.memorymap.core.model.Photo
import kr.surprise.memorymap.core.model.PhotoId
import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.domain.model.NewPhoto

/**
 * 사진은 공간 단위로 한 번만 구독합니다. 지도 탭과 달력 탭은 같은 사진을
 * '어디' 와 '언제' 로 나눠 볼 뿐이라, 탭을 옮길 때 다시 받으면 안 됩니다.
 */
interface PhotoRepository {
    fun observePhotos(spaceId: SpaceId): Flow<List<Photo>>
    fun observeCovers(spaceId: SpaceId): Flow<List<Cover>>

    suspend fun refresh(spaceId: SpaceId): Outcome<Unit>
    suspend fun upload(spaceId: SpaceId, photos: List<NewPhoto>): Outcome<List<Photo>>
    suspend fun delete(spaceId: SpaceId, id: PhotoId): Outcome<Unit>
    suspend fun setCover(spaceId: SpaceId, key: CoverKey, id: PhotoId): Outcome<Unit>
}

interface SpaceRepository {
    fun observeSpaces(): Flow<List<Space>>
    suspend fun refresh(): Outcome<Unit>

    /** 이름을 정하는 순간 초대 코드도 같이 나옵니다. */
    suspend fun create(name: String): Outcome<Pair<Space, Invite>>
    suspend fun join(code: String): Outcome<Space>
    suspend fun newInvite(spaceId: SpaceId): Outcome<Invite>
    suspend fun rename(spaceId: SpaceId, name: String): Outcome<Unit>
}

/** 지역 이름과 경계. 좌표 → 지역 판정도 여기서 합니다 (기기 안에서). */
interface RegionCatalog {
    suspend fun all(): List<Region>
    suspend fun find(codeValue: String): Region?

    /**
     * 사진의 GPS 좌표가 어느 지역에 들어가는지 봅니다.
     * **네트워크를 쓰지 않습니다** — 좌표를 서버에 보내 "여기가 어디죠?" 하고 묻지 않으려는 것.
     */
    suspend fun regionAt(latitude: Double, longitude: Double): Region?
}
