package kr.jjaguk.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kr.jjaguk.core.common.Failure
import kr.jjaguk.core.common.Outcome
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Cloud Functions callable 호출. SDK 없이 규약대로 보냅니다 —
 * `POST {origin}/{이름}` 에 `{"data": ...}`, 응답은 `{"result": ...}`.
 *
 * 지금 쓰는 함수는 `joinSpace` 하나입니다. 참여 검증을 클라이언트에 맡기면
 * 코드 없이도 들어와져서, 그 한 걸음만 서버가 합니다 (`functions/index.js`).
 *
 * 값은 [Firestore] 처럼 **문자열만** 받고 돌려줍니다 — 우리가 주고받는 것이
 * 코드·ID 뿐이라 타입 지도를 만들 이유가 없습니다.
 */
class Functions(
    /** `https://asia-northeast3-{프로젝트}.cloudfunctions.net` */
    private val origin: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build(),
    /** 함수가 `request.auth` 로 받는 ID 토큰. 없으면 UNAUTHENTICATED 로 거절됩니다. */
    private val token: suspend () -> String? = { null },
    private val appCheck: suspend () -> String? = { null },
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun call(name: String, data: Map<String, String>): Outcome<Map<String, String>> =
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                putJsonObject("data") { data.forEach { (key, value) -> put(key, value) } }
            }
            val builder = Request.Builder()
                .url("$origin/$name")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
            token()?.let { builder.header("Authorization", "Bearer $it") }
            appCheck()?.let { builder.header("X-Firebase-AppCheck", it) }

            try {
                client.newCall(builder.build()).execute().use { response ->
                    when {
                        response.isSuccessful -> {
                            val result = json.parseToJsonElement(response.body?.string().orEmpty())
                                .jsonObject["result"]?.jsonObject.orEmpty()
                            Outcome.Ok(result.mapNotNull { (key, value) ->
                                (value as? kotlinx.serialization.json.JsonPrimitive)
                                    ?.let { key to it.content }
                            }.toMap())
                        }
                        // callable 의 NOT_FOUND(틀린 코드)가 404 로 옵니다.
                        response.code == 404 -> Outcome.Fail(Failure.NotFound)
                        response.code == 401 || response.code == 403 -> Outcome.Fail(Failure.Denied)
                        else -> Outcome.Fail(Failure.Unknown)
                    }
                }
            } catch (e: SocketTimeoutException) {
                Outcome.Fail(Failure.Timeout)
            } catch (e: IOException) {
                Outcome.Fail(Failure.Network)
            }
        }
}
