package kr.jjaguk.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
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
 * Firestore 를 **REST 로** 씁니다. SDK 를 안 넣는 이유는 [FirebaseStorage] 와 같습니다 —
 * 받을 것이 적고, 두 앱이 같은 방식으로 움직입니다 (`docs/app/AUTH.md`).
 *
 * **여기가 "이 사람이 이 짜국의 멤버인가" 를 답하는 자리입니다.** Storage 규칙이
 * `firestore.exists(...)` 로 이 문서들을 건너다봅니다 (`firestore.rules`).
 *
 * Firestore 의 값에는 **타입 이름이 붙어 옵니다** (`{"stringValue":"우리 지도"}`).
 * 그 모양을 화면까지 들고 가지 않으려고 [Value] 로 감싸 둡니다.
 */
class Firestore(
    private val projectId: String,
    private val client: OkHttpClient = defaultClient(),
    private val token: suspend () -> String? = { null },
    /**
     * App Check 토큰. "진짜 우리 앱에서 온 요청인가" 를 서버가 가릴 수 있게 얹습니다.
     * `null` 이면 없이 보냅니다 — 못 받았다고 데이터 길이 막히면 안 됩니다
     * (콘솔에서 강제를 켜기 전까지는 지표만 쌓입니다).
     */
    private val appCheck: suspend () -> String? = { null },
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** 우리가 쓰는 값은 이 셋뿐입니다. 필요해지면 그때 늘립니다. */
    sealed interface Value {
        data class Text(val value: String) : Value
        data class Number(val value: Long) : Value
        data class Flag(val value: Boolean) : Value
    }

    /**
     * 문서 하나. [id] 는 경로의 마지막 조각입니다 — Firestore 가 돌려주는 `name` 은
     * `projects/../documents/spaces/ABC` 처럼 전체 경로라 그대로 쓰면 길기만 합니다.
     */
    data class Document(val id: String, val fields: Map<String, Value>) {
        fun text(key: String): String? = (fields[key] as? Value.Text)?.value
        fun number(key: String): Long? = (fields[key] as? Value.Number)?.value
        fun flag(key: String): Boolean? = (fields[key] as? Value.Flag)?.value
    }

    /** 없는 문서는 실패가 아니라 `null` 입니다 — 처음 들어가는 짜국이 그렇습니다. */
    suspend fun get(path: String): Outcome<Document?> = withContext(Dispatchers.IO) {
        when (val result = call(Request.Builder().url(url(path)).get())) {
            is Outcome.Fail ->
                if (result.reason == Failure.NotFound) Outcome.Ok(null) else result
            is Outcome.Ok -> try {
                Outcome.Ok(parseDocument(json.parseToJsonElement(result.value).jsonObject))
            } catch (e: Exception) {
                Outcome.Fail(Failure.Unknown)
            }
        }
    }

    /**
     * 문서를 통째로 씁니다. 없으면 만들고 있으면 덮습니다.
     *
     * `updateMask` 를 안 붙이는 이유: 우리 문서는 필드가 몇 개뿐이라 통째로 쓰는 편이
     * 단순하고, 일부만 고치다 옛 필드가 남는 일도 없습니다.
     */
    suspend fun set(path: String, fields: Map<String, Value>): Outcome<Unit> =
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                putJsonObject("fields") {
                    fields.forEach { (key, value) -> put(key, encode(value)) }
                }
            }
            val request = Request.Builder()
                .url(url(path))
                .patch(body.toString().toRequestBody("application/json".toMediaType()))
            when (val result = call(request)) {
                is Outcome.Fail -> result
                is Outcome.Ok -> Outcome.Ok(Unit)
            }
        }

    suspend fun delete(path: String): Outcome<Unit> = withContext(Dispatchers.IO) {
        when (val result = call(Request.Builder().url(url(path)).delete())) {
            is Outcome.Fail -> result
            is Outcome.Ok -> Outcome.Ok(Unit)
        }
    }

    /** 컬렉션 안의 문서들. 비어 있으면 빈 목록입니다. */
    suspend fun list(collection: String): Outcome<List<Document>> = withContext(Dispatchers.IO) {
        when (val result = call(Request.Builder().url(url(collection) + "?pageSize=300").get())) {
            is Outcome.Fail ->
                if (result.reason == Failure.NotFound) Outcome.Ok(emptyList()) else result
            is Outcome.Ok -> try {
                val documents = json.parseToJsonElement(result.value)
                    .jsonObject["documents"]?.jsonArray.orEmpty()
                Outcome.Ok(documents.map { parseDocument(it.jsonObject) })
            } catch (e: Exception) {
                Outcome.Fail(Failure.Unknown)
            }
        }
    }

    private fun url(path: String) =
        "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/" +
            path.split('/').joinToString("/") { enc(it) }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun encode(value: Value): JsonObject = buildJsonObject {
        when (value) {
            is Value.Text -> put("stringValue", value.value)
            // integerValue 는 **문자열로** 보냅니다. Firestore 가 그렇게 받습니다.
            is Value.Number -> put("integerValue", value.value.toString())
            is Value.Flag -> put("booleanValue", value.value)
        }
    }

    private suspend fun call(builder: Request.Builder): Outcome<String> {
        token()?.let { builder.header("Authorization", "Bearer $it") }
        appCheck()?.let { builder.header("X-Firebase-AppCheck", it) }
        return try {
            client.newBuilder()
                .callTimeout(Limits.LIST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
                .newCall(builder.build()).execute().use { response ->
                    when {
                        response.isSuccessful -> Outcome.Ok(response.body?.string().orEmpty())
                        response.code == 404 -> Outcome.Fail(Failure.NotFound)
                        response.code == 401 || response.code == 403 -> Outcome.Fail(Failure.Denied)
                        else -> Outcome.Fail(Failure.Unknown)
                    }
                }
        } catch (e: SocketTimeoutException) {
            Outcome.Fail(Failure.Timeout)
        } catch (e: java.io.InterruptedIOException) {
            Outcome.Fail(Failure.Timeout)
        } catch (e: IOException) {
            Outcome.Fail(Failure.Network)
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        /** 응답을 [Document] 로. 테스트에서도 쓰려고 밖에 둡니다. */
        internal fun parseDocument(raw: JsonObject): Document {
            val name = raw["name"]?.jsonPrimitive?.content.orEmpty()
            val fields = raw["fields"]?.jsonObject.orEmpty().mapNotNull { (key, element) ->
                val holder = element.jsonObject
                val value = when {
                    holder.containsKey("stringValue") ->
                        Value.Text(holder.getValue("stringValue").jsonPrimitive.content)
                    holder.containsKey("integerValue") ->
                        holder.getValue("integerValue").jsonPrimitive.content.toLongOrNull()
                            ?.let { Value.Number(it) }
                    holder.containsKey("booleanValue") ->
                        Value.Flag(holder.getValue("booleanValue").jsonPrimitive.content.toBoolean())
                    // 우리가 안 쓰는 타입(지도·배열 등)은 조용히 건너뜁니다.
                    else -> null
                }
                value?.let { key to it }
            }.toMap()

            return Document(id = name.substringAfterLast('/'), fields = fields)
        }
    }
}
