package kr.jjaguk.data.region

/**
 * 좌표가 다각형 안에 있는지. 광선 쏘기(ray casting).
 *
 * 사진 위치를 서버에 묻지 않고 **기기 안에서** 지역을 찾기 위한 것입니다.
 * 순수 함수라 테스트할 수 있고, 판정이 틀리면 사진이 엉뚱한 곳에 붙으므로 여기부터 테스트합니다.
 */
internal object PointInPolygon {

    /** [ring] 은 [경도, 위도] 쌍의 목록입니다 (GeoJSON 순서). */
    fun inRing(lon: Double, lat: Double, ring: List<DoubleArray>): Boolean {
        var inside = false
        var j = ring.size - 1
        for (i in ring.indices) {
            val xi = ring[i][0]; val yi = ring[i][1]
            val xj = ring[j][0]; val yj = ring[j][1]
            // 가로선이 변을 지나는지. 세로로 걸치는 구간에서만 교차를 셉니다.
            if ((yi > lat) != (yj > lat)) {
                val xCross = (xj - xi) * (lat - yi) / (yj - yi) + xi
                if (lon < xCross) inside = !inside
            }
            j = i
        }
        return inside
    }

    /** 첫 고리는 바깥, 나머지는 구멍입니다 (GeoJSON Polygon 규칙). */
    fun inPolygon(lon: Double, lat: Double, rings: List<List<DoubleArray>>): Boolean {
        if (rings.isEmpty()) return false
        if (!inRing(lon, lat, rings[0])) return false
        for (k in 1 until rings.size) {
            if (inRing(lon, lat, rings[k])) return false   // 구멍 안이면 밖입니다
        }
        return true
    }
}
