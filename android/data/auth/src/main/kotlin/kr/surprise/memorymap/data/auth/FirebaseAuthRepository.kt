package kr.surprise.memorymap.data.auth

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.Account
import kr.surprise.memorymap.core.network.FirebaseAuth
import kr.surprise.memorymap.domain.repository.AuthRepository
import java.io.File

/**
 * 로그인 상태를 들고 있는 곳. 토큰을 **앱 전용 폴더**에 두고, 낡으면 알아서 새로 받습니다.
 *
 * **왜 암호화 저장소를 안 쓰나**: 안드로이드 앱 전용 폴더는 다른 앱이 못 읽습니다.
 * Firebase 공식 SDK 도 토큰을 같은 수준(앱 전용 SharedPreferences)에 둡니다.
 * `security-crypto` 를 더하면 의존성이 하나 늘고, 웹에서 도는 Claude 쪽 컨테이너는
 * `dl.google.com` 이 막혀 있어 받지도 못합니다 (`libs.versions.toml` 의 주석).
 * 기기가 루팅되면 어차피 둘 다 열립니다.
 *
 * iOS 는 키체인을 씁니다 — 그쪽은 의존성 없이 OS 가 주는 표준 자리라서요.
 */
class FirebaseAuthRepository(
    context: Context,
    private val auth: FirebaseAuth,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) : AuthRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val file = File(context.filesDir, "account.json")
    private val lock = Mutex()

    private val account = MutableStateFlow<Account?>(null)

    /** 앱을 켜면 저장된 것을 한 번 읽어 둡니다. 읽기 전에는 로그아웃으로 보입니다. */
    suspend fun restore() {
        val saved = lock.withLock { read() } ?: return
        account.value = saved.account()
    }

    override fun observeAccount(): Flow<Account?> = account.asStateFlow()

    override suspend fun account(): Account? {
        account.value?.let { return it }
        // 아직 안 읽었을 수 있습니다 — 저장된 것을 한 번 봅니다.
        val saved = lock.withLock { read() }?.account()
        if (saved != null) account.value = saved
        return saved
    }

    override suspend fun signInWithGoogle(googleIdToken: String): Outcome<Account> =
        when (val result = auth.signInWithGoogle(googleIdToken)) {
            is Outcome.Fail -> result
            is Outcome.Ok -> {
                val session = result.value
                val stored = Stored(
                    uid = session.uid,
                    // 구글 계정에 이름이 없으면 이메일 앞부분, 그것도 없으면 '나'.
                    // 멤버 목록에 빈칸이 뜨면 누구인지 알 수 없습니다.
                    displayName = session.displayName
                        ?: session.email?.substringBefore('@')
                        ?: "나",
                    email = session.email,
                    idToken = session.tokens.idToken,
                    refreshToken = session.tokens.refreshToken,
                    expiresAt = session.tokens.expiresAtEpochSeconds,
                )
                lock.withLock { write(stored) }
                val value = stored.account()
                account.value = value
                Outcome.Ok(value)
            }
        }

    override suspend fun signOut() {
        lock.withLock { withContext(Dispatchers.IO) { file.delete() } }
        account.value = null
    }

    /**
     * 낡았으면 새로 받아서 돌려줍니다. 갱신이 **거부되면 로그아웃**시킵니다 —
     * 못 쓰는 토큰을 들고 계속 실패하는 것보다 다시 로그인하는 편이 빠릅니다.
     */
    override suspend fun idToken(): String? {
        val stored = lock.withLock { read() } ?: return null
        if (now() < stored.expiresAt - 60) return stored.idToken

        return when (val refreshed = auth.refresh(stored.refreshToken)) {
            is Outcome.Fail -> {
                signOut()
                null
            }
            is Outcome.Ok -> {
                val next = stored.copy(
                    idToken = refreshed.value.idToken,
                    refreshToken = refreshed.value.refreshToken,
                    expiresAt = refreshed.value.expiresAtEpochSeconds,
                )
                lock.withLock { write(next) }
                account.value = next.account()
                next.idToken
            }
        }
    }

    private fun read(): Stored? {
        if (!file.exists()) return null
        return try {
            json.decodeFromString<Stored>(file.readText())
        } catch (e: Exception) {
            null   // 형식이 바뀌었거나 깨졌으면 로그아웃으로 봅니다
        }
    }

    private fun write(value: Stored) {
        file.writeText(json.encodeToString(value))
    }

    @Serializable
    internal data class Stored(
        val uid: String,
        val displayName: String,
        val email: String? = null,
        val idToken: String,
        val refreshToken: String,
        val expiresAt: Long,
    ) {
        fun account() = Account(uid = uid, displayName = displayName, email = email)
    }
}
