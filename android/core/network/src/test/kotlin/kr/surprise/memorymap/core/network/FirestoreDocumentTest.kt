package kr.surprise.memorymap.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Firestore 응답을 읽는 부분은 손으로 짠 곳이라 여기서 굳힙니다.
 * iOS `FirestoreDocumentTests` 와 **같은 예시**를 씁니다 — 두 앱이 같은 문서를 읽습니다.
 */
class FirestoreDocumentTest {

    private fun parse(text: String) =
        Firestore.parseDocument(Json.parseToJsonElement(text).jsonObject)

    @Test
    fun `전체 경로에서 마지막 조각만 id 로 남는다`() {
        val document = parse(
            """{"name":"projects/our-surprise/databases/(default)/documents/spaces/K7QF2M"}"""
        )

        assertEquals("K7QF2M", document.id)
    }

    @Test
    fun `타입이 붙은 값을 벗겨 낸다`() {
        val document = parse(
            """
            {"name":"a/b/c",
             "fields":{
               "name":{"stringValue":"우리 추억 지도"},
               "uploadedAt":{"integerValue":"1740000000"},
               "owner":{"booleanValue":true}
             }}
            """.trimIndent()
        )

        assertEquals("우리 추억 지도", document.text("name"))
        assertEquals(1_740_000_000L, document.number("uploadedAt"))
        assertEquals(true, document.flag("owner"))
    }

    /** 숫자를 **문자열로** 준다는 것이 이 API 의 함정입니다. 문자열로 읽으면 안 됩니다. */
    @Test
    fun `정수는 문자열로 오지만 숫자로 읽힌다`() {
        val document = parse("""{"name":"a","fields":{"n":{"integerValue":"42"}}}""")

        assertEquals(42L, document.number("n"))
        assertNull(document.text("n"))
    }

    /** 우리가 안 쓰는 타입이 섞여 와도 앱이 죽지 않아야 합니다. */
    @Test
    fun `모르는 타입은 건너뛰고 나머지는 읽는다`() {
        val document = parse(
            """
            {"name":"a",
             "fields":{
               "keep":{"stringValue":"남는다"},
               "skip":{"arrayValue":{"values":[{"stringValue":"x"}]}},
               "also":{"timestampValue":"2026-03-05T00:00:00Z"}
             }}
            """.trimIndent()
        )

        assertEquals("남는다", document.text("keep"))
        assertEquals(1, document.fields.size)
    }

    @Test
    fun `필드가 없는 문서도 읽힌다`() {
        val document = parse("""{"name":"projects/p/databases/(default)/documents/invites/AB12"}""")

        assertEquals("AB12", document.id)
        assertEquals(0, document.fields.size)
        assertNull(document.text("아무거나"))
    }
}
