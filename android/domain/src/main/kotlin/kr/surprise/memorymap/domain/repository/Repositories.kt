package kr.surprise.memorymap.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.Account
import kr.surprise.memorymap.core.model.Cover
import kr.surprise.memorymap.core.model.CoverKey
import kr.surprise.memorymap.core.model.Invite
import kr.surprise.memorymap.core.model.Photo
import kr.surprise.memorymap.core.model.PhotoId
import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.core.model.RegionCode
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.model.SpaceKind
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

    /**
     * 이름을 정하는 순간 초대 코드도 같이 나옵니다 — **같이 쓰는 짜국만**.
     * 혼자 쓰는 짜국은 초대할 사람이 없어 코드가 `null` 입니다.
     */
    suspend fun create(name: String, kind: SpaceKind): Outcome<Pair<Space, Invite?>>
    suspend fun join(code: String): Outcome<Space>
    suspend fun newInvite(spaceId: SpaceId): Outcome<Invite>
    suspend fun rename(spaceId: SpaceId, name: String): Outcome<Unit>
}

/**
 * 로그인. **같이 쓰는 짜국에서만** 필요합니다 (`docs/app/AUTH.md`).
 *
 * 구글 로그인 SDK 는 이 뒤에 숨어 있습니다 — 도메인은 "구글 ID 토큰을 받아 왔다" 까지만
 * 알고, 그것을 Firebase 토큰으로 바꾸는 일은 데이터 계층이 합니다.
 */
interface AuthRepository {
    /** 지금 로그인한 사람. 로그인 전에는 `null` 이 흐릅니다. */
    fun observeAccount(): Flow<Account?>

    /** 구글 로그인 SDK 가 받아 온 ID 토큰으로 Firebase 세션을 엽니다. */
    suspend fun signInWithGoogle(googleIdToken: String): Outcome<Account>

    suspend fun signOut()

    /**
     * 요청 헤더에 얹을 Firebase ID 토큰. 낡았으면 **여기서 알아서 새로 받습니다.**
     * 로그인 전이거나 갱신이 실패하면 `null` — 부르는 쪽은 헤더를 빼고 보냅니다.
     */
    suspend fun idToken(): String?
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

    /** 지도에 표시할 자리 (위도, 경도). 경계가 없는 지역은 null. */
    suspend fun centerOf(code: RegionCode): DoubleArray?

    /**
     * 지역의 **면**. 테두리를 그릴 때도, 사진으로 칠할 때도 이걸 씁니다.
     *
     * `폴리곤 → 고리 → 점` 세 겹이고 점은 `(경도, 위도)` 순서입니다 — GeoJSON 과 같은
     * 구조라 저장된 값을 뒤집지 않습니다. 섬이 많은 지역은 폴리곤이 여러 개,
     * 안이 뚫린 지역은 한 폴리곤에 고리가 여러 개 나옵니다.
     *
     * 선만 필요하면 고리를 평평하게 펴서 쓰면 됩니다. 반대로 선에서 면을 되만들 수는
     * 없어서(어느 고리가 구멍인지 잃어버립니다) 면으로 내줍니다.
     */
    suspend fun shapeOf(code: RegionCode): List<List<List<DoubleArray>>>
}
