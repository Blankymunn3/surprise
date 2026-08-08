import Foundation
import Testing

@testable import CoreNetwork

/// Firestore 응답을 읽는 부분은 손으로 짠 곳이라 여기서 굳힙니다.
/// 안드로이드 `FirestoreDocumentTest` 와 **같은 예시**를 씁니다 — 두 앱이 같은 문서를 읽습니다.
@Suite("Firestore 문서 읽기")
struct FirestoreDocumentTests {

    private func parse(_ text: String) -> Firestore.Document {
        let raw = try! JSONSerialization.jsonObject(with: Data(text.utf8)) as! [String: Any]
        return Firestore.document(from: raw)
    }

    @Test("전체 경로에서 마지막 조각만 id 로 남는다")
    func idIsLastSegment() {
        let document = parse(
            #"{"name":"projects/our-surprise/databases/(default)/documents/spaces/K7QF2M"}"#
        )

        #expect(document.id == "K7QF2M")
    }

    @Test("타입이 붙은 값을 벗겨 낸다")
    func unwrapsTypedValues() {
        let document = parse("""
        {"name":"a/b/c",
         "fields":{
           "name":{"stringValue":"우리 추억 지도"},
           "uploadedAt":{"integerValue":"1740000000"},
           "owner":{"booleanValue":true}
         }}
        """)

        #expect(document.text("name") == "우리 추억 지도")
        #expect(document.number("uploadedAt") == 1_740_000_000)
        #expect(document.flag("owner") == true)
    }

    /// 숫자를 **문자열로** 준다는 것이 이 API 의 함정입니다. 문자열로 읽으면 안 됩니다.
    @Test("정수는 문자열로 오지만 숫자로 읽힌다")
    func integersArriveAsStrings() {
        let document = parse(#"{"name":"a","fields":{"n":{"integerValue":"42"}}}"#)

        #expect(document.number("n") == 42)
        #expect(document.text("n") == nil)
    }

    /// 우리가 안 쓰는 타입이 섞여 와도 앱이 죽지 않아야 합니다.
    @Test("모르는 타입은 건너뛰고 나머지는 읽는다")
    func skipsUnknownTypes() {
        let document = parse("""
        {"name":"a",
         "fields":{
           "keep":{"stringValue":"남는다"},
           "skip":{"arrayValue":{"values":[{"stringValue":"x"}]}},
           "also":{"timestampValue":"2026-03-05T00:00:00Z"}
         }}
        """)

        #expect(document.text("keep") == "남는다")
        #expect(document.fields.count == 1)
    }

    @Test("필드가 없는 문서도 읽힌다")
    func documentWithoutFields() {
        let document = parse(
            #"{"name":"projects/p/databases/(default)/documents/invites/AB12"}"#
        )

        #expect(document.id == "AB12")
        #expect(document.fields.isEmpty)
        #expect(document.text("아무거나") == nil)
    }

    @Test("보낼 때도 정수는 문자열로 싼다")
    func encodesIntegerAsString() {
        #expect(Firestore.encode(.number(7))["integerValue"] as? String == "7")
        #expect(Firestore.encode(.text("가"))["stringValue"] as? String == "가")
        #expect(Firestore.encode(.flag(true))["booleanValue"] as? Bool == true)
    }
}
