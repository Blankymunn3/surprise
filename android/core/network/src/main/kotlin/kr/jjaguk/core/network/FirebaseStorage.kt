package kr.jjaguk.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kr.jjaguk.core.common.Failure
import kr.jjaguk.core.common.Limits
import kr.jjaguk.core.common.Outcome
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Firebase Storage 를 **REST 로** 씁니다. 웹(`assets/firebase.js`)과 같은 방식입니다.
 * SDK 를 안 쓰는 이유: 웹과 규칙을 한 벌로 유지하기 위해서이고, 로그인이 붙기 전까지는
 * SDK 가 해 주는 일이 없습니다.
 *
 * ⚠️ 목록 조회에 **`delimiter=/` 를 반드시 붙여야** 합니다.
 * 없으면 게시된 규칙에서 403 이 납니다 (웹에서 겪은 문제 — `FIREBASE.md`).
 */
class FirebaseStorage(
    private val bucket: String,
    private val client: OkHttpClient = defaultClient(),
    /**
     * 요청에 얹을 Firebase ID 토큰. **로그인 전에는 `null`** 이고 그때는 헤더를 안 붙입니다 —
     * 규칙이 아직 로그인을 요구하지 않는 경로(`regions/`)가 있어서, 붙이지 않는 쪽이
     * 지금까지처럼 동작합니다.
     *
     * 매 요청마다 부릅니다. 낡은 토큰을 새로 받는 일은 부르는 쪽(`AuthRepository`)이 합니다.
     */
    private val token: suspend () -> String? = { null },
    /** App Check 토큰. `Firestore` 와 같은 규칙 — 없으면 없이 보냅니다. */
    private val appCheck: suspend () -> String? = { null },
) {
    private val json = Json { ignoreUnknownKeys = true }

    data class Item(val fullPath: String, val name: String)

    /** 토큰이 있으면 헤더를 얹습니다. 없으면 그대로 보냅니다. */
    private suspend fun Request.Builder.authorized(): Request.Builder = apply {
        token()?.let { header("Authorization", "Bearer $it") }
        appCheck()?.let { header("X-Firebase-AppCheck", it) }
    }

    suspend fun list(prefix: String): Outcome<List<Item>> = withContext(Dispatchers.IO) {
        val url = base() + "?prefix=" + enc(prefix) +
            "&delimiter=" + enc("/") + "&maxResults=1000"
        call(Request.Builder().url(url).get().authorized().build(), Limits.LIST_TIMEOUT_MS) { body ->
            val items = json.parseToJsonElement(body).jsonObject["items"]?.jsonArray.orEmpty()
            items.mapNotNull { element ->
                val path = element.jsonObject["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                Item(fullPath = path, name = path.substringAfterLast('/'))
            }
        }
    }

    suspend fun upload(path: String, bytes: ByteArray, contentType: String): Outcome<Unit> =
        withContext(Dispatchers.IO) {
            if (bytes.size > Limits.MAX_UPLOAD_BYTES) return@withContext Outcome.Fail(Failure.TooLarge)
            val request = Request.Builder()
                // 웹(assets/firebase.js)과 **같은 주소**여야 합니다.
                // uploadType 은 GCS JSON API 용 파라미터라 넣지 않습니다.
                .url(base() + "?name=" + enc(path))
                .post(bytes.toRequestBody(contentType.toMediaType()))
                .authorized()
                .build()
            call(request, Limits.UPLOAD_TIMEOUT_MS) { }
        }

    suspend fun delete(path: String): Outcome<Unit> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(base() + "/" + enc(path)).delete().authorized().build()
        call(request, Limits.LIST_TIMEOUT_MS) { }
    }

    suspend fun download(path: String): Outcome<ByteArray> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(downloadUrl(path)).get().authorized().build()
        callBytes(request, Limits.LIST_TIMEOUT_MS)
    }

    /**
     * 사진을 그릴 때 이미지 로더에 넘기는 주소.
     *
     * ⚠️ **이 주소로 나가는 요청에는 위 헤더가 안 실립니다** — 이미지 로더가 자기 방식으로
     * 받아 오기 때문입니다. 규칙이 로그인을 요구하게 되면 로더 쪽에도 토큰을 얹어야 합니다
     * (안드로이드는 Coil 의 OkHttp 에 인터셉터, iOS 는 URLSession 으로 직접).
     */
    fun downloadUrl(path: String): String = base() + "/" + enc(path) + "?alt=media"

    private fun base() = "https://firebasestorage.googleapis.com/v0/b/$bucket/o"

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun <T> call(request: Request, timeoutMs: Long, parse: (String) -> T): Outcome<T> =
        try {
            client.newBuilder()
                .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build()
                .newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> Outcome.Ok(parse(response.body?.string().orEmpty()))
                        response.code == 404 -> Outcome.Fail(Failure.NotFound)
                        response.code == 401 || response.code == 403 -> Outcome.Fail(Failure.Denied)
                        else -> Outcome.Fail(Failure.Unknown)
                    }
                }
        } catch (e: SocketTimeoutException) {
            Outcome.Fail(Failure.Timeout)
        } catch (e: java.io.InterruptedIOException) {
            // OkHttp 의 callTimeout 은 이 예외로 옵니다
            Outcome.Fail(Failure.Timeout)
        } catch (e: IOException) {
            Outcome.Fail(Failure.Network)
        }

    private fun callBytes(request: Request, timeoutMs: Long): Outcome<ByteArray> =
        try {
            client.newBuilder()
                .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build()
                .newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Outcome.Ok(response.body?.bytes() ?: ByteArray(0))
                    } else if (response.code == 404) {
                        Outcome.Fail(Failure.NotFound)
                    } else {
                        Outcome.Fail(Failure.Unknown)
                    }
                }
        } catch (e: java.io.InterruptedIOException) {
            Outcome.Fail(Failure.Timeout)
        } catch (e: IOException) {
            Outcome.Fail(Failure.Network)
        }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
