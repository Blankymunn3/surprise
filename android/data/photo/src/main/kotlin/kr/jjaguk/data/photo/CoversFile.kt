package kr.jjaguk.data.photo

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kr.jjaguk.core.model.Cover
import kr.jjaguk.core.model.CoverKey
import kr.jjaguk.core.model.PhotoId

/**
 * `covers.json` 의 모양. `{"region_11140":"a1b2c3", "day_2026-03-05":"d4e5f6"}`
 *
 * 서버에 두든 기기에 두든 **같은 형식**입니다. 혼자 쓰던 짜국을 나중에 같이로 바꿀 때
 * 파일을 그대로 올리면 되도록 하려는 것입니다 (`docs/app/AUTH.md`).
 */
internal object CoversFile {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<Cover> = try {
        json.parseToJsonElement(text).jsonObject.mapNotNull { (documentId, value) ->
            val photoId = (value as? JsonPrimitive)?.content ?: return@mapNotNull null
            CoverKey.of(documentId)?.let { Cover(it, PhotoId(photoId)) }
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun serialize(covers: List<Cover>): ByteArray = buildString {
        append('{')
        covers.joinTo(this, ",") { "\"" + it.key.documentId + "\":\"" + it.photoId.value + "\"" }
        append('}')
    }.toByteArray()

}
