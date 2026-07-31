package kr.surprise.memorymap.data.space

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kr.surprise.memorymap.core.common.Failure
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.Invite
import kr.surprise.memorymap.core.model.Member
import kr.surprise.memorymap.core.model.MemberRole
import kr.surprise.memorymap.core.model.PathSafe
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.model.SpaceKind
import kr.surprise.memorymap.core.network.FirebaseStorage
import kr.surprise.memorymap.domain.repository.SpaceRepository
import java.io.File
import java.util.UUID

/**
 * 공간 목록. **내가 속한 공간 목록은 기기에**, **공간의 이름·멤버는 저장소에** 둡니다.
 *
 * 로그인이 없어 "누가 어느 공간의 멤버인가" 를 서버가 판단할 수 없기 때문입니다.
 * 로그인 + Firestore 가 붙으면 통째로 갈아 끼웁니다 — 화면과 도메인은 그대로입니다.
 *
 * **혼자 쓰는 짜국은 서버를 아예 안 씁니다** — 이름까지 기기 안 파일에 둡니다.
 * 그래서 로그인도, 인터넷도 없이 만들어집니다 (`docs/app/AUTH.md`).
 */
class SharedSpaceRepository(
    context: Context,
    private val storage: FirebaseStorage,
) : SpaceRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val localFile = File(context.filesDir, "spaces.json")
    private val lock = Mutex()

    private val spaces = MutableStateFlow<List<Space>>(emptyList())

    override fun observeSpaces(): Flow<List<Space>> = spaces.asStateFlow()

    /**
     * 내 uid. 로그인이 없으니 기기에서 한 번 만들어 두고 계속 씁니다.
     * `LocalMembership` 이 모듈 안쪽 타입이라 이 함수도 internal 입니다 —
     * 밖에서 필요해지면 그때 도메인 모델로 올립니다.
     */
    internal suspend fun me(): LocalMembership = lock.withLock { readLocal() }

    suspend fun renameMe(displayName: String) = lock.withLock {
        writeLocal(readLocal().copy(displayName = displayName.trim().ifBlank { "나" }))
    }

    /** 혼자 짜국이 먼저입니다 — 네트워크를 기다리지 않고 바로 보여 줄 수 있습니다. */
    override suspend fun refresh(): Outcome<Unit> {
        val local = lock.withLock { readLocal() }
        val mine = local.personal.map { it.toSpace(local) }
        val loaded = local.spaceIds.mapNotNull { id -> fetchSpace(SpaceId(id)) }
        spaces.value = mine + loaded
        return Outcome.Ok(Unit)
    }

    override suspend fun create(name: String, kind: SpaceKind): Outcome<Pair<Space, Invite?>> {
        val local = lock.withLock { readLocal() }
        val id = SpaceId(InviteCode.generate())

        // 혼자 쓰는 짜국은 여기서 끝입니다. 서버에 아무것도 안 만듭니다.
        // 초대 코드도 없습니다 — 초대할 상대가 없으니까요.
        if (kind == SpaceKind.Personal) {
            val record = PersonalSpace(id.value, name)
            lock.withLock { writeLocal(local.copy(personal = local.personal + record)) }
            val space = record.toSpace(local)
            spaces.value = spaces.value + space
            return Outcome.Ok(space to null)
        }

        val document = SpaceDocument(
            name = name,
            members = listOf(MemberDocument(local.uid, local.displayName, owner = true)),
        )
        when (val written = writeSpace(id, document)) {
            is Outcome.Fail -> return written
            is Outcome.Ok -> Unit
        }

        lock.withLock { writeLocal(local.copy(spaceIds = local.spaceIds + id.value)) }
        val space = document.toSpace(id)
        spaces.value = spaces.value + space

        // 이름을 정하는 순간 초대 코드가 함께 나옵니다. 코드가 곧 공간 ID 입니다.
        return Outcome.Ok(space to Invite(id.value, id, Long.MAX_VALUE, Int.MAX_VALUE, 0))
    }

    override suspend fun join(code: String): Outcome<Space> {
        val normalized = InviteCode.normalize(code) ?: return Outcome.Fail(Failure.NotFound)
        val id = SpaceId(normalized)

        val document = readSpace(id) ?: return Outcome.Fail(Failure.NotFound)
        val local = lock.withLock { readLocal() }

        if (local.spaceIds.contains(id.value)) {
            return Outcome.Ok(document.toSpace(id))   // 이미 들어가 있는 공간
        }

        val withMe = document.copy(
            members = document.members.filterNot { it.uid == local.uid } +
                MemberDocument(local.uid, local.displayName, owner = false),
        )
        when (val written = writeSpace(id, withMe)) {
            is Outcome.Fail -> return written
            is Outcome.Ok -> Unit
        }

        lock.withLock { writeLocal(local.copy(spaceIds = local.spaceIds + id.value)) }
        val space = withMe.toSpace(id)
        spaces.value = spaces.value + space
        return Outcome.Ok(space)
    }

    /** 코드가 곧 공간 ID 라 새 코드를 따로 만들지 않습니다. 있는 코드를 다시 알려 줍니다. */
    override suspend fun newInvite(spaceId: SpaceId): Outcome<Invite> {
        // 혼자 쓰는 짜국에는 초대 코드가 없습니다.
        if (isPersonal(spaceId)) return Outcome.Fail(Failure.NotFound)
        return Outcome.Ok(Invite(spaceId.value, spaceId, Long.MAX_VALUE, Int.MAX_VALUE, 0))
    }

    override suspend fun rename(spaceId: SpaceId, name: String): Outcome<Unit> {
        // 혼자 짜국의 이름은 기기 안 파일에만 있습니다.
        if (isPersonal(spaceId)) {
            lock.withLock {
                val current = readLocal()
                writeLocal(current.copy(personal = current.personal.map {
                    if (it.id == spaceId.value) it.copy(name = name) else it
                }))
            }
            spaces.value = spaces.value.map { if (it.id == spaceId) it.copy(name = name) else it }
            return Outcome.Ok(Unit)
        }

        val document = readSpace(spaceId) ?: return Outcome.Fail(Failure.NotFound)
        return when (val written = writeSpace(spaceId, document.copy(name = name))) {
            is Outcome.Fail -> written
            is Outcome.Ok -> {
                spaces.value = spaces.value.map { if (it.id == spaceId) it.copy(name = name) else it }
                Outcome.Ok(Unit)
            }
        }
    }

    private suspend fun isPersonal(spaceId: SpaceId): Boolean =
        lock.withLock { readLocal() }.personal.any { it.id == spaceId.value }

    /** 사진 수·지역 수·표지는 사진 저장소가 알고 있어 화면에서 채웁니다. */
    fun applySummaries(summaries: Map<String, SpaceSummary>) {
        spaces.value = spaces.value.map { space ->
            summaries[space.id.value]?.let {
                space.copy(
                    photoCount = it.photoCount,
                    regionCount = it.regionCount,
                    coverPhotoUrl = it.coverPhotoUrl,
                    lastPhotoOn = it.lastPhotoOn,
                )
            } ?: space
        }
    }

    private suspend fun fetchSpace(id: SpaceId): Space? = readSpace(id)?.toSpace(id)

    private suspend fun readSpace(id: SpaceId): SpaceDocument? {
        PathSafe.require(id.value, "공간 ID")
        return when (val downloaded = storage.download(spacePath(id))) {
            is Outcome.Fail -> null
            is Outcome.Ok -> try {
                json.decodeFromString<SpaceDocument>(String(downloaded.value))
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun writeSpace(id: SpaceId, document: SpaceDocument): Outcome<Unit> =
        storage.upload(spacePath(id), json.encodeToString(document).toByteArray(), "application/json")

    private fun spacePath(id: SpaceId) = "spaces/${id.value}/space.json"

    private fun readLocal(): LocalMembership {
        if (!localFile.exists()) {
            val fresh = LocalMembership(uid = UUID.randomUUID().toString().take(12), displayName = "나")
            writeLocal(fresh)
            return fresh
        }
        return try {
            json.decodeFromString<LocalMembership>(localFile.readText())
        } catch (e: Exception) {
            LocalMembership(uid = UUID.randomUUID().toString().take(12), displayName = "나")
        }
    }

    private fun writeLocal(value: LocalMembership) {
        localFile.writeText(json.encodeToString(value))
    }

    private fun SpaceDocument.toSpace(id: SpaceId) = Space(
        id = id,
        name = name,
        members = members.map {
            Member(it.uid, it.displayName, if (it.owner) MemberRole.Owner else MemberRole.Member)
        },
        photoCount = 0,
        regionCount = 0,
        coverPhotoUrl = null,
        lastPhotoOn = null,
        kind = SpaceKind.Shared,
    )

    /** 혼자 짜국의 멤버는 늘 나 하나입니다. 서버에 문서가 없어 여기서 만들어 줍니다. */
    private fun PersonalSpace.toSpace(owner: LocalMembership) = Space(
        id = SpaceId(id),
        name = name,
        members = listOf(Member(owner.uid, owner.displayName, MemberRole.Owner)),
        photoCount = 0,
        regionCount = 0,
        coverPhotoUrl = null,
        lastPhotoOn = null,
        kind = SpaceKind.Personal,
    )
}

/** 사진 쪽에서 계산해 넘겨 주는 요약 */
data class SpaceSummary(
    val photoCount: Int,
    val regionCount: Int,
    val coverPhotoUrl: String?,
    val lastPhotoOn: java.time.LocalDate?,
)
