import CoreModel
import Foundation
import Testing
@testable import FeatureMap

@Suite("지도를 어디에 맞출까")
struct MapFocusTests {

    @Test("경계를 다 감싼다")
    func wrapsTheWholeShape() {
        #expect(
            boundsOf(box(west: 126.7, south: 37.4, east: 127.2, north: 37.7))
                == .area(south: 37.4, west: 126.7, north: 37.7, east: 127.2)
        )
    }

    /// 나라를 골랐을 때 시군구만 한 배율로 서던 것을 고친 자리입니다.
    @Test("나라는 나라만큼 넓게 잡는다")
    func aCountryIsCountrySized() {
        #expect(
            boundsOf(box(west: 129.4, south: 31.0, east: 145.5, north: 45.6))
                == .area(south: 31.0, west: 129.4, north: 45.6, east: 145.5)
        )
    }

    /// 날짜변경선을 넘는 나라. 그냥 최솟값·최댓값을 쓰면 -180..180 이 되어 **지구 한 바퀴**가
    /// 나옵니다. 피지는 5° 남짓입니다. 동쪽 끝은 180 을 넘어간 채로 둡니다 — 접으면 다시
    /// 한 바퀴가 됩니다.
    @Test("날짜변경선을 넘어도 짧은 쪽으로 감싼다")
    func acrossTheDateLine() {
        #expect(
            boundsOf([
                ring(west: 177.0, south: -18.0, east: 180.0, north: -17.0),
                ring(west: -180.0, south: -18.0, east: -178.0, north: -16.0),
            ]) == .area(south: -18.0, west: 177.0, north: -16.0, east: 182.0)
        )
    }

    /// 러시아는 선을 넘지만 실제로도 넓습니다 — 넓은 것과 한 바퀴는 다릅니다.
    @Test("선을 넘는 넓은 나라도 제 너비만큼만 잡는다")
    func wideButNotTheWholeGlobe() {
        #expect(
            boundsOf([
                ring(west: 19.6, south: 41.2, east: 180.0, north: 81.3),
                ring(west: -180.0, south: 64.0, east: -169.0, north: 71.0),
            ]) == .area(south: 41.2, west: 19.6, north: 81.3, east: 191.0)
        )
    }

    @Test("아메리카처럼 서쪽에만 있어도 그대로 잡는다")
    func allInTheWest() {
        #expect(
            boundsOf(box(west: -171.8, south: 18.9, east: -67.0, north: 71.4))
                == .area(south: 18.9, west: -171.8, north: 71.4, east: -67.0)
        )
    }

    @Test("경계가 없으면 맞출 것도 없다")
    func nothingToFit() {
        #expect(boundsOf([]) == nil)
        #expect(boundsOf([[[]]]) == nil)
    }

    /// 한 점으로 뭉친 경계는 넓이가 없습니다. 배율을 정해 주는 쪽에 넘깁니다.
    @Test("한 점짜리 경계는 넓이로 보지 않는다")
    func aSinglePointHasNoArea() {
        #expect(boundsOf([[[GeoPoint(latitude: 37.5, longitude: 127.0)]]]) == nil)
    }

    private func box(west: Double, south: Double, east: Double, north: Double) -> [[[GeoPoint]]] {
        [ring(west: west, south: south, east: east, north: north)]
    }

    private func ring(west: Double, south: Double, east: Double, north: Double) -> [[GeoPoint]] {
        [[
            GeoPoint(latitude: south, longitude: west),
            GeoPoint(latitude: south, longitude: east),
            GeoPoint(latitude: north, longitude: east),
            GeoPoint(latitude: north, longitude: west),
            GeoPoint(latitude: south, longitude: west),
        ]]
    }
}
