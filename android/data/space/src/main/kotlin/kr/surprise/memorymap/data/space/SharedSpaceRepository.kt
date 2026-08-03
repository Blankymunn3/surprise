package kr.surprise.memorymap.data.space

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kr.surprise.memorymap.core.common.Failure
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.Account
import kr.surprise.memorymap.core.model.Invite
import kr.surprise.memorymap.core.model.Member
import kr.surprise.memorymap.core.model.MemberRole
import kr.surprise.memorymap.core.model.PathSafe
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.model.SpaceKind
import kr.surprise.memorymap.core.network.Firestore
import kr.surprise.memorymap.domain.repository.AuthRepository
import kr.surprise.memorymap.domain.repository.SpaceRepository
import java.io.File
import java.util.UUID

/**
 * 짜국 목록.
 *
 * | 종류 | 어디에 |
 * |---|---|
 * | 혼자 | 기기 안 파일 (서버를 아예 안 씁니다) |
 * | 같이 | **Firestore** |
 *
 * Firestore 로 옮긴 이유는 규칙 때문입니다. Storage 규칙은 다른 파일의 내용을 못 읽어서
 * "이 사람이 멤버인가" 를 판단할 수 없습니다. 멤버 문서가 Firestore 에 있어야
 * `firestore.exists(...)` 로 물어볼 수 있습니다 (`docs/app/AUTH.md`).
 *
 * ```
 * spaces/{짜국ID}                 이름 · 주인
 * spaces/{짜국ID}/members/{uid}   누가 멤버인가  ← 규칙이 보는 곳
 * invites/{코드}                  코드 → 짜국ID
 * users/{uid}/spaces/{짜국ID}     내가 어느 짜국에 속하나 (기기를 바꿔도 따라옵니다)
 * ```
 *
 * **초대 코드가 더 이상 짜국 ID 가 아닙니다.** 코드를 알아도 경로를 모르고, 경로를 알아도
 * 멤버가 아니면 못 읽습니다.
 */
class SharedSpaceRepository(
    context: Context,
    private val firestore: Firestore,
    private val accounts: AuthRepository,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
    /** 짜국 ID 는 **초대 코드보다 깁니다** — 찍어서 맞힐 수 없어야 합니다. */
    private val newSpaceId: () -> String = { UUID.randomUUID().toString().replace("-", "") },
) : SpaceRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val localFile = File(context.filesDir, "spaces.json")
    private val lock = Mutex()

    private val spaces = MutableStateFlow<List<Space>>(emptyList())

    override fun observeSpaces(): Flow<List<Space>> = spaces.asStateFlow()

    /** 내 이름. 혼자 짜국에만 씁니다 — 같이 쓰는 쪽은 로그인한 계정 이름을 따릅니다. */
    suspend fun renameMe(displayName: String) = lock.withLock {
        writeLocal(readLocal().copy(displayName = displayName.trim().ifBlank { "나" }))
    }

    /** 혼자 짜국이 먼저입니다 — 네트워크를 기다리지 않고 바로 보여 줄 수 있습니다. */
    override suspend fun refresh(): Outcome<Unit> {
        val local = lock.withLock { readLocal() }
        val mine = local.personal.map { it.toSpace(local) }

        val account = accounts.account()
        if (account == null) {
            // 로그인 전에는 같이 쓰는 짜국을 볼 길이 없습니다. 혼자 것만 보여 줍니다.
            spaces.value = mine
            return Outcome.Ok(Unit)
        }

        val ids = when (val listed = firestore.list("users/${account.uid}/spaces")) {
            is Outcome.Fail -> {
                spaces.value = mine
                return listed
            }
            is Outcome.Ok -> listed.value.map { it.id }
        }

        spaces.value = mine + ids.mapNotNull { id -> fetchSpace(SpaceId(id)) }
        return Outcome.Ok(Unit)
    }

    override suspend fun create(name: String, kind: SpaceKind): Outcome<Pair<Space, Invite?>> {
        // 혼자 쓰는 짜국은 여기서 끝입니다. 서버에 아무것도 안 만듭니다.
        // 초대 코드도 없습니다 — 초대할 사람이 없으니까요.
        if (kind == SpaceKind.Personal) {
            val local = lock.withLock { readLocal() }
            val record = PersonalSpace(newSpaceId(), name)
            lock.withLock { writeLocal(local.copy(personal = local.personal + record)) }
            val space = record.toSpace(local)
            spaces.value = spaces.value + space
            return Outcome.Ok(space to null)
        }

        val account = accounts.account() ?: return Outcome.Fail(Failure.Denied)
        val id = SpaceId(newSpaceId())

        // 순서가 중요합니다: **멤버 문서를 먼저** 만들어야 그다음 쓰기가 규칙을 통과합니다.
        val space = mapOf(
            "name" to Firestore.Value.Text(name),
            "ownerUid" to Firestore.Value.Text(account.uid),
            "createdAt" to Firestore.Value.Number(now()),
        )
        firestore.set("spaces/${id.value}", space).orReturn { return it }
        firestore.set("spaces/${id.value}/members/${account.uid}", memberFields(account, owner = true))
            .orReturn { return it }

        // 코드는 **문 앞까지만** 데려다줍니다. 이 문서를 봐도 멤버가 되기 전에는 못 읽습니다.
        val code = InviteCode.generate()
        firestore.set("invites/$code", mapOf("spaceId" to Firestore.Value.Text(id.value)))
            .orReturn { return it }
        rememberMembership(account.uid, id)

        val made = Space(
            id = id,
            name = name,
            members = listOf(Member(account.uid, account.displayName, MemberRole.Owner)),
            photoCount = 0,
            regionCount = 0,
            coverPhotoUrl = null,
            lastPhotoOn = null,
            kind = SpaceKind.Shared,
        )
        spaces.value = spaces.value + made
        return Outcome.Ok(made to Invite(code, id, Long.MAX_VALUE, Int.MAX_VALUE, 0))
    }

    override suspend fun join(code: String): Outcome<Space> {
        val normalized = InviteCode.normalize(code) ?: return Outcome.Fail(Failure.NotFound)
        val account = accounts.account() ?: return Outcome.Fail(Failure.Denied)

        val invite = when (val found = firestore.get("invites/$normalized")) {
            is Outcome.Fail -> return found
            is Outcome.Ok -> found.value ?: return Outcome.Fail(Failure.NotFound)
        }
        val id = SpaceId(invite.text("spaceId") ?: return Outcome.Fail(Failure.NotFound))

        // 나를 멤버로 넣습니다. 규칙이 **자기 자신만** 넣게 해 둬서 남을 끌어들일 수 없습니다.
        firestore.set("spaces/${id.value}/members/${account.uid}", memberFields(account, owner = false))
            .orReturn { return it }
        rememberMembership(account.uid, id)

        val space = fetchSpace(id) ?: return Outcome.Fail(Failure.NotFound)
        spaces.value = spaces.value.filterNot { it.id == id } + space
        return Outcome.Ok(space)
    }

    /** 코드는 만들 때 한 번 나옵니다. 다시 찾으려면 저장해 둔 것을 봐야 합니다. */
    override suspend fun newInvite(spaceId: SpaceId): Outcome<Invite> {
        if (isPersonal(spaceId)) return Outcome.Fail(Failure.NotFound)
        val code = InviteCode.generate()
        return when (val written = firestore.set(
            "invites/$code",
            mapOf("spaceId" to Firestore.Value.Text(spaceId.value)),
        )) {
            is Outcome.Fail -> written
            is Outcome.Ok -> Outcome.Ok(Invite(code, spaceId, Long.MAX_VALUE, Int.MAX_VALUE, 0))
        }
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

        val current = when (val found = firestore.get("spaces/${spaceId.value}")) {
            is Outcome.Fail -> return found
            is Outcome.Ok -> found.value ?: return Outcome.Fail(Failure.NotFound)
        }
        val fields = current.fields + ("name" to Firestore.Value.Text(name))
        return when (val written = firestore.set("spaces/${spaceId.value}", fields)) {
            is Outcome.Fail -> written
            is Outcome.Ok -> {
                spaces.value = spaces.value.map { if (it.id == spaceId) it.copy(name = name) else it }
                Outcome.Ok(Unit)
            }
        }
    }

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

    private suspend fun rememberMembership(uid: String, id: SpaceId) {
        firestore.set(
            "users/$uid/spaces/${id.value}",
            mapOf("joinedAt" to Firestore.Value.Number(now())),
        )
    }

    private fun memberFields(account: Account, owner: Boolean) = mapOf(
        "displayName" to Firestore.Value.Text(account.displayName),
        "owner" to Firestore.Value.Flag(owner),
    )

    private suspend fun fetchSpace(id: SpaceId): Space? {
        PathSafe.require(id.value, "짜국 ID")
        val document = when (val found = firestore.get("spaces/${id.value}")) {
            is Outcome.Fail -> return null
            is Outcome.Ok -> found.value ?: return null
        }
        val members = when (val listed = firestore.list("spaces/${id.value}/members")) {
            is Outcome.Fail -> emptyList()
            is Outcome.Ok -> listed.value.map {
                Member(
                    uid = it.id,
                    displayName = it.text("displayName").orEmpty().ifBlank { "?" },
                    role = if (it.flag("owner") == true) MemberRole.Owner else MemberRole.Member,
                )
            }
        }

        return Space(
            id = id,
            name = document.text("name").orEmpty(),
            members = members,
            photoCount = 0,
            regionCount = 0,
            coverPhotoUrl = null,
            lastPhotoOn = null,
            kind = SpaceKind.Shared,
        )
    }

    private suspend fun isPersonal(spaceId: SpaceId): Boolean =
        lock.withLock { readLocal() }.personal.any { it.id == spaceId.value }

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

/** 실패면 그대로 빠져나가는 짧은 길. 쓰기를 줄줄이 이을 때 `when` 이 겹겹이 쌓이는 걸 막습니다. */
private inline fun Outcome<Unit>.orReturn(bail: (Outcome.Fail) -> Nothing) {
    if (this is Outcome.Fail) bail(this)
}

/** 사진 쪽에서 계산해 넘겨 주는 요약 */
data class SpaceSummary(
    val photoCount: Int,
    val regionCount: Int,
    val coverPhotoUrl: String?,
    val lastPhotoOn: java.time.LocalDate?,
)
