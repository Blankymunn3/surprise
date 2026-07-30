import Testing
@testable import DataRegion

@Suite("좌표가 지역 안인지")
struct PointInPolygonTests {
    let square: [(Double, Double)] = [(0, 0), (10, 0), (10, 10), (0, 10), (0, 0)]

    @Test("안에 있으면 안이다")
    func inside() {
        #expect(PointInPolygon.inRing(lon: 5, lat: 5, ring: square))
    }

    @Test("밖에 있으면 밖이다")
    func outside() {
        #expect(!PointInPolygon.inRing(lon: 15, lat: 5, ring: square))
        #expect(!PointInPolygon.inRing(lon: 5, lat: -1, ring: square))
    }

    @Test("구멍 안은 밖이다")
    func hole() {
        let hole: [(Double, Double)] = [(4, 4), (6, 4), (6, 6), (4, 6), (4, 4)]
        #expect(!PointInPolygon.inPolygon(lon: 5, lat: 5, rings: [square, hole]))
        #expect(PointInPolygon.inPolygon(lon: 2, lat: 2, rings: [square, hole]))
    }

    @Test("오목한 모양도 맞게 가른다")
    func concave() {
        let c: [(Double, Double)] = [
            (0, 0), (10, 0), (10, 3), (3, 3), (3, 7), (10, 7), (10, 10), (0, 10), (0, 0),
        ]
        #expect(PointInPolygon.inRing(lon: 1, lat: 5, ring: c))
        #expect(!PointInPolygon.inRing(lon: 7, lat: 5, ring: c))
    }

    @Test("동봉한 지역 이름표를 읽는다")
    func loadsBundledRegions() {
        let regions = AssetRegionCatalog.loadRegions()
        #expect(regions.count > 400)
        #expect(regions.contains { $0.code.value == "11140" })   // 서울 중구
        #expect(regions.contains { $0.code.value == "C-JPN" && $0.name == "일본" })
    }

    @Test("서울 좌표가 서울 시군구로 판정된다")
    func seoulResolves() async {
        let catalog = AssetRegionCatalog()
        let region = await catalog.regionAt(latitude: 37.5636, longitude: 126.9976)  // 서울 중구 근처
        #expect(region != nil)
        #expect(region?.parentName == "서울")
    }
}
