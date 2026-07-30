package kr.surprise.memorymap.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kr.surprise.memorymap.core.common.Failure
import kr.surprise.memorymap.core.common.Limits
import kr.surprise.memorymap.core.common.Outcome
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
) {
    private val json = Json { ignoreUnknownKeys = true }

    data class Item(val fullPath: String, val name: String)

    suspend fun list(prefix: String): Outcome<List<Item>> = withContext(Dispatchers.IO) {
        val url = base() + "?prefix=" + enc(prefix) +
            "&delimiter=" + enc("/") + "&maxResults=1000"
        call(Request.Builder().url(url).get().build(), Limits.LIST_TIMEOUT_MS) { body ->
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
                .url(base() + "?uploadType=media&name=" + enc(path))
                .post(bytes.toRequestBody(contentType.toMediaType()))
                .build()
            call(request, Limits.UPLOAD_TIMEOUT_MS) { }
        }

    suspend fun delete(path: String): Outcome<Unit> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(base() + "/" + enc(path)).delete().build()
        call(request, Limits.LIST_TIMEOUT_MS) { }
    }

    suspend fun download(path: String): Outcome<ByteArray> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(downloadUrl(path)).get().build()
        callBytes(request, Limits.LIST_TIMEOUT_MS)
    }

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
