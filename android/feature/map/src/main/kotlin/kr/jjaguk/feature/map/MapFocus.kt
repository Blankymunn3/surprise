package kr.jjaguk.feature.map

/**
 * 지역을 골랐을 때 지도를 **어디에 맞출 것인가**.
 *
 * 배율 하나로 고정하면 안 됩니다 — 시군구에 맞춘 배율로 나라를 열면 나라 한복판만
 * 크게 보이고 정작 고른 곳이 어디까지인지는 안 보입니다. 경계가 있으면 **그 경계가
 * 통째로 들어오게** 맞춥니다.
 */
sealed interface MapFocus {

    /** 경계가 있는 지역 — 이 네모가 화면 안에 다 들어오게. */
    data class Area(
        val south: Double,
        val west: Double,
        val north: Double,
        val east: Double,
    ) : MapFocus

    /** 경계 없이 좌표만 있는 장소 — 맞출 넓이가 없어 배율을 정해 줍니다. */
    data class Spot(val latitude: Double, val longitude: Double) : MapFocus

    /**
     * 내가 지금 있는 자리. [Spot] 과 좌표만 있는 것은 같지만 **훨씬 바짝 당깁니다.**
     *
     * 지역을 고를 때는 그 지역이 어디쯤인지 알면 되지만, 내 위치는 "지금 여기가 어디냐"
     * 를 보는 것이라 동네가 보여야 합니다. 도 단위로 보이는 배율에서는 점만 찍히고
     * 정작 내가 어디 있는지는 알 수 없습니다.
     */
    data class Me(val latitude: Double, val longitude: Double) : MapFocus
}

/**
 * 경계를 감싸는 가장 작은 네모. 점은 GeoJSON 순서 `(경도, 위도)` 입니다.
 *
 * 경도는 **짧은 쪽으로** 감쌉니다. 러시아·피지처럼 날짜변경선을 넘는 나라는 경도가
 * -180 과 180 양쪽에 흩어져 있어서, 그냥 최솟값과 최댓값을 쓰면 지구 한 바퀴가 됩니다.
 * 대신 경도들 사이에서 **가장 넓게 빈 구간**을 찾아 그 반대쪽을 씁니다 — 아무 점도 없는
 * 그 구간이 곧 지역의 바깥입니다.
 *
 * 선을 넘는 경우 [MapFocus.Area.east] 가 180 을 넘어갑니다 (러시아는 서 19°, 동 191°).
 * 지도가 그대로 받아 씁니다 — 180 안으로 접으면 다시 지구 한 바퀴가 되니까요.
 */
internal fun boundsOf(polygons: List<List<List<DoubleArray>>>): MapFocus.Area? {
    var south = Double.MAX_VALUE
    var north = -Double.MAX_VALUE
    val longitudes = ArrayList<Double>()

    for (polygon in polygons) {
        for (ring in polygon) {
            for (point in ring) {
                val latitude = point[1]
                if (latitude < south) south = latitude
                if (latitude > north) north = latitude
                longitudes.add(point[0])
            }
        }
    }
    if (longitudes.isEmpty()) return null
    longitudes.sort()

    // 선을 넘지 않는 경우: 가장 넓게 빈 구간은 최댓값에서 최솟값으로 되돌아가는 바깥쪽입니다.
    var west = longitudes.first()
    var east = longitudes.last()
    var widest = longitudes.first() + FULL_TURN - longitudes.last()

    for (i in 1 until longitudes.size) {
        val hole = longitudes[i] - longitudes[i - 1]
        if (hole > widest) {
            widest = hole
            west = longitudes[i]
            east = longitudes[i - 1] + FULL_TURN
        }
    }

    // 한 점으로 뭉친 경계는 맞출 넓이가 없습니다. 배율을 정해 주는 쪽에 맡깁니다.
    if (north - south < HAIR && east - west < HAIR) return null

    return MapFocus.Area(south = south, west = west, north = north, east = east)
}

private const val FULL_TURN = 360.0

/** 100m 남짓. 이보다 좁은 네모는 넓이가 있다고 보지 않습니다. */
private const val HAIR = 0.001
