package kr.surprise.memorymap.data.region

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** 지역 하나의 경계. Polygon 이면 하나, MultiPolygon 이면 여럿입니다. */
internal class GeoShape(
    val code: String,
    val polygons: List<List<List<DoubleArray>>>,
    /** 1차로 빠르게 걸러내는 사각형 (minLon, minLat, maxLon, maxLat) */
    val bounds: DoubleArray,
) {
    fun mightContain(lon: Double, lat: Double): Boolean =
        lon >= bounds[0] && lon <= bounds[2] && lat >= bounds[1] && lat <= bounds[3]

    fun contains(lon: Double, lat: Double): Boolean =
        mightContain(lon, lat) && polygons.any { PointInPolygon.inPolygon(lon, lat, it) }

    companion object {
        fun parse(code: String, geometry: JsonObject): GeoShape? {
            val type = geometry["type"]?.jsonPrimitive?.content ?: return null
            val coords = geometry["coordinates"]?.jsonArray ?: return null

            val polygons: List<List<List<DoubleArray>>> = when (type) {
                "Polygon" -> listOf(rings(coords))
                "MultiPolygon" -> coords.map { rings(it.jsonArray) }
                else -> return null
            }
            if (polygons.isEmpty()) return null

            var minLon = Double.MAX_VALUE
            var minLat = Double.MAX_VALUE
            var maxLon = -Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            for (poly in polygons) for (ring in poly) for (p in ring) {
                if (p[0] < minLon) minLon = p[0]
                if (p[0] > maxLon) maxLon = p[0]
                if (p[1] < minLat) minLat = p[1]
                if (p[1] > maxLat) maxLat = p[1]
            }
            return GeoShape(code, polygons, doubleArrayOf(minLon, minLat, maxLon, maxLat))
        }

        private fun rings(polygon: JsonArray): List<List<DoubleArray>> =
            polygon.map { ring ->
                ring.jsonArray.map { point ->
                    val xy = point.jsonArray
                    doubleArrayOf(
                        xy[0].jsonPrimitive.content.toDouble(),
                        xy[1].jsonPrimitive.content.toDouble(),
                    )
                }
            }
    }
}
