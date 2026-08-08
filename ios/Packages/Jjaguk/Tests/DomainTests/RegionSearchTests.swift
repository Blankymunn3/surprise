import CoreModel
import Foundation
import Testing
@testable import Domain

@Suite("지역 검색")
struct RegionSearchTests {
    let jungguSeoul = Region(code: RegionCode("11140"), name: "중구", parentName: "서울")
    let jungguBusan = Region(code: RegionCode("21110"), name: "중구", parentName: "부산")
    let japan = Region(code: RegionCode("C-JPN"), name: "일본", parentName: nil)
    let osaka = Region(code: RegionCode("P-JPN-27"), name: "오사카부", parentName: "일본")

    var all: [Region] { [osaka, japan, jungguBusan, jungguSeoul] }

    @Test("이름이 정확히 같은 것이 먼저 나온다")
    func exactFirst() {
        #expect(RegionSearch.rank(query: "일본", regions: all).first == japan)
    }

    @Test("국내 시군구가 해외보다 먼저 나온다")
    func domesticFirst() {
        let hits = RegionSearch.rank(query: "중구", regions: all)
        #expect(hits.prefix(2).allSatisfy { $0.name == "중구" })
    }

    @Test("상위 이름으로도 찾힌다")
    func byParent() {
        #expect(RegionSearch.rank(query: "서울", regions: all) == [jungguSeoul])
    }

    @Test("빈 검색어는 아무것도 내지 않는다")
    func emptyQuery() {
        #expect(RegionSearch.rank(query: "   ", regions: all).isEmpty)
    }

    @Test("지역 코드에서 나라를 뽑아낸다")
    func countryFromCode() {
        #expect(RegionCode("P-JPN-27").countryISO3 == "JPN")
        #expect(RegionCode("C-JPN").countryISO3 == "JPN")
        #expect(RegionCode("11140").countryISO3 == nil)
    }

    @Test("코드 모양으로 종류를 가른다")
    func kinds() {
        #expect(RegionCode("11140").kind == .koreanDistrict)
        #expect(RegionCode("C-JPN").kind == .country)
        #expect(RegionCode("P-JPN-27").kind == .subdivision)
        #expect(RegionCode("bali").kind == .place)
    }
}
