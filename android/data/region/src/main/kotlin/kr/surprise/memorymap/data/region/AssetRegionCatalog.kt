package kr.surprise.memorymap.data.region

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.core.model.RegionCode
import kr.surprise.memorymap.domain.repository.RegionCatalog

@Serializable
private data class RegionRow(val code: String, val name: String, val parent: String? = null)

/**
 * 지역 이름과 경계를 앱에 동봉한 파일에서 읽습니다.
 *
 * **웹과 같은 데이터**입니다 — southkorea-maps 2013 시군구 + world.geo.json.
 * 코드가 같아야 웹에서 넣은 사진이 앱에서도 보입니다 (`docs/app/CONVENTIONS.md`).
 *
 * 경계 파일이 580KB 라 처음 쓸 때 한 번만 읽고 메모리에 들고 있습니다.
 */
class AssetRegionCatalog(private val context: Context) : RegionCatalog {

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()

    private var regions: List<Region>? = null
    private var shapesKorea: List<GeoShape>? = null
    private var shapesWorld: List<GeoShape>? = null

    override suspend fun all(): List<Region> = lock.withLock {
        regions ?: loadRegions().also { regions = it }
    }

    override suspend fun find(codeValue: String): Region? =
        all().firstOrNull { it.code.value == codeValue }

    /**
     * 좌표 → 지역. 국내 시군구를 먼저 보고 없으면 나라로 내려갑니다.
     * 한국에서 찍은 사진이 "대한민국" 이 아니라 "서울 중구" 로 붙어야 하기 때문입니다.
     */
    override suspend fun regionAt(latitude: Double, longitude: Double): Region? {
        val korea = lock.withLock {
            shapesKorea ?: loadShapes("boundaries_kr.json").also { shapesKorea = it }
        }
        korea.firstOrNull { it.contains(longitude, latitude) }?.let { return find(it.code) }

        val world = lock.withLock {
            shapesWorld ?: loadShapes("boundaries_world.json").also { shapesWorld = it }
        }
        return world.firstOrNull { it.contains(longitude, latitude) }?.let { find(it.code) }
    }

    /** `C-` 로 시작하면 나라, 아니면 국내 시군구입니다. */
    private suspend fun shapesFor(code: RegionCode): List<GeoShape> =
        if (code.value.startsWith("C-")) {
            lock.withLock { shapesWorld ?: loadShapes("boundaries_world.json").also { shapesWorld = it } }
        } else {
            lock.withLock { shapesKorea ?: loadShapes("boundaries_kr.json").also { shapesKorea = it } }
        }

    override suspend fun shapeOf(code: RegionCode): List<List<List<DoubleArray>>> =
        shapesFor(code).firstOrNull { it.code == code.value }?.polygons.orEmpty()

    override suspend fun centerOf(code: RegionCode): DoubleArray? {
        val shape = shapesFor(code).firstOrNull { it.code == code.value } ?: return null
        // 경계 상자의 가운데. 정확한 무게중심은 필요 없고 "그 지역 어딘가" 면 됩니다.
        return doubleArrayOf(
            (shape.bounds[1] + shape.bounds[3]) / 2,
            (shape.bounds[0] + shape.bounds[2]) / 2,
        )
    }

    private suspend fun loadRegions(): List<Region> = withContext(Dispatchers.IO) {
        val text = context.assets.open("regions.json").bufferedReader().use { it.readText() }
        json.decodeFromString<List<RegionRow>>(text)
            .map { Region(RegionCode(it.code), it.name, it.parent) }
    }

    private suspend fun loadShapes(assetName: String): List<GeoShape> = withContext(Dispatchers.IO) {
        val text = context.assets.open(assetName).bufferedReader().use { it.readText() }
        json.parseToJsonElement(text).jsonArray.mapNotNull { element ->
            val row = element.jsonObject
            val code = row["c"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val geometry = row["g"] as? JsonObject ?: return@mapNotNull null
            GeoShape.parse(code, geometry)
        }
    }
}
