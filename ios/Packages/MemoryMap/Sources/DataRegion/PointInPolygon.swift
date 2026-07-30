import Foundation

/// 좌표가 다각형 안에 있는지. 광선 쏘기(ray casting).
/// 사진 위치를 서버에 묻지 않고 **기기 안에서** 지역을 찾기 위한 것입니다.
public enum PointInPolygon {

    /// `ring` 은 (경도, 위도) 쌍의 목록입니다 (GeoJSON 순서).
    public static func inRing(lon: Double, lat: Double, ring: [(Double, Double)]) -> Bool {
        var inside = false
        var j = ring.count - 1
        for i in ring.indices {
            let (xi, yi) = ring[i]
            let (xj, yj) = ring[j]
            if (yi > lat) != (yj > lat) {
                let xCross = (xj - xi) * (lat - yi) / (yj - yi) + xi
                if lon < xCross { inside.toggle() }
            }
            j = i
        }
        return inside
    }

    /// 첫 고리는 바깥, 나머지는 구멍입니다 (GeoJSON Polygon 규칙).
    public static func inPolygon(lon: Double, lat: Double, rings: [[(Double, Double)]]) -> Bool {
        guard let outer = rings.first else { return false }
        guard inRing(lon: lon, lat: lat, ring: outer) else { return false }
        for hole in rings.dropFirst() where inRing(lon: lon, lat: lat, ring: hole) {
            return false
        }
        return true
    }
}
