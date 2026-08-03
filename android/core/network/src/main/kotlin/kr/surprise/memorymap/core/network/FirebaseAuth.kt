package kr.surprise.memorymap.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kr.surprise.memorymap.core.common.Failure
import kr.surprise.memorymap.core.common.Limits
import kr.surprise.memorymap.core.common.Outcome
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 구글 로그인으로 받은 ID 토큰을 **Firebase 토큰으로 바꿉니다.** REST 로만 씁니다.
 *
 * Firebase SDK 를 통째로 넣지 않는 이유는 [FirebaseStorage] 와 같습니다 —
 * 받을 것이 적고, 두 앱이 같은 방식으로 움직입니다 (`docs/app/AUTH.md`).
 *
 * ```
 * 구글 로그인 SDK  →  구글 ID 토큰
 *       ↓  signInWithIdp
 * Firebase ID 토큰(1시간) + refresh 토큰(안 만료)
 *       ↓  securetoken
 * 새 ID 토큰
 * ```
 *
 * `apiKey` 는 비밀이 아닙니다(`google-services.json` 에 들어 있는 그 값). 실제 보안은
 * 규칙이 합니다. 다만 **앱마다 값이 다릅니다** — 조립하는 곳에서 넣어 줍니다.
 */
class FirebaseAuth(
    private val apiKey: String,
    private val client: OkHttpClient = defaultClient(),
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** 로그인한 사람. `displayName` 은 구글 계정에 이름이 없으면 비어 옵니다. */
    data class Session(
        val uid: String,
        val email: String?,
        val displayName: String?,
        val tokens: Tokens,
    )

    /**
     * ID 토큰은 한 시간이면 만료됩니다. [expiresAtEpochSeconds] 를 들고 다니는 이유는
     * 요청을 보내기 **전에** 만료를 알아채기 위해서입니다 — 401 을 받고 나서 고치면
     * 사진 올리다 실패한 것처럼 보입니다.
     */
    data class Tokens(
        val idToken: String,
        val refreshToken: String,
        val expiresAtEpochSeconds: Long,
    ) {
        /**
         * 만료 **1분 전**부터 낡은 것으로 봅니다. 요청이 날아가는 동안 만료되면
         * 결국 401 이라, 여유를 두고 미리 바꿉니다.
         */
        fun isStale(nowEpochSeconds: Long): Boolean = nowEpochSeconds >= expiresAtEpochSeconds - 60
    }

    /**
     * 구글 ID 토큰 → Firebase 세션.
     *
     * `postBody` 가 폼 형식인 것은 이 API 가 원래 OAuth 응답을 그대로 받도록
     * 만들어져서입니다. `requestUri` 는 웹 리다이렉트용이라 앱에서는 아무 값이나 됩니다.
     */
    suspend fun signInWithGoogle(googleIdToken: String): Outcome<Session> =
        withContext(Dispatchers.IO) {
            val body = buildString {
                append("{\"postBody\":\"id_token=").append(enc(googleIdToken))
                append("&providerId=google.com\",")
                append("\"requestUri\":\"http://localhost\",")
                append("\"returnSecureToken\":true}")
            }

            val request = Request.Builder()
                .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=" + enc(apiKey))
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            call(request) { text ->
                val o = json.parseToJsonElement(text).jsonObject
                Session(
                    uid = o["localId"]?.jsonPrimitive?.content.orEmpty(),
                    email = o["email"]?.jsonPrimitive?.content,
                    displayName = o["displayName"]?.jsonPrimitive?.content?.ifBlank { null },
                    tokens = Tokens(
                        idToken = o["idToken"]?.jsonPrimitive?.content.orEmpty(),
                        refreshToken = o["refreshToken"]?.jsonPrimitive?.content.orEmpty(),
                        expiresAtEpochSeconds = now() + (o["expiresIn"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600),
                    ),
                )
            }
        }

    /**
     * 낡은 ID 토큰을 새로 받습니다. **주소가 다릅니다** — 이쪽은 `securetoken` 이고
     * 본문도 JSON 이 아니라 폼입니다.
     *
     * 응답의 `refresh_token` 은 보통 같은 값이지만 **바뀔 수도 있어서** 그대로 받아 씁니다.
     */
    suspend fun refresh(refreshToken: String): Outcome<Tokens> = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        val request = Request.Builder()
            .url("https://securetoken.googleapis.com/v1/token?key=" + enc(apiKey))
            .post(form)
            .build()

        call(request) { text ->
            val o = json.parseToJsonElement(text).jsonObject
            Tokens(
                idToken = o["id_token"]?.jsonPrimitive?.content.orEmpty(),
                refreshToken = o["refresh_token"]?.jsonPrimitive?.content ?: refreshToken,
                expiresAtEpochSeconds = now() + (o["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600),
            )
        }
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun <T> call(request: Request, parse: (String) -> T): Outcome<T> = try {
        client.newBuilder()
            .callTimeout(Limits.LIST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
            .newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> Outcome.Ok(parse(response.body?.string().orEmpty()))
                    // 400 은 토큰이 틀렸거나 만료된 것 — 다시 로그인해야 합니다.
                    response.code == 400 || response.code == 401 || response.code == 403 ->
                        Outcome.Fail(Failure.Denied)
                    else -> Outcome.Fail(Failure.Unknown)
                }
            }
    } catch (e: SocketTimeoutException) {
        Outcome.Fail(Failure.Timeout)
    } catch (e: java.io.InterruptedIOException) {
        Outcome.Fail(Failure.Timeout)
    } catch (e: IOException) {
        Outcome.Fail(Failure.Network)
    } catch (e: Exception) {
        // 응답이 JSON 이 아닐 때. 여기서 막지 않으면 로그인 화면이 그대로 멈춥니다.
        Outcome.Fail(Failure.Unknown)
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
