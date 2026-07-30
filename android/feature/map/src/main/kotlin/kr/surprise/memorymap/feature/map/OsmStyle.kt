package kr.surprise.memorymap.feature.map

/**
 * MapLibre 스타일을 코드로 만듭니다. **웹과 같은 OSM 타일**을 씁니다 —
 * API 키도, 결제 계정도 필요 없고 두 화면이 같은 지도를 보여 줍니다.
 *
 * 스타일 파일을 따로 두지 않는 이유: 타일 주소 한 줄이 전부라, 파일로 빼면
 * 웹의 타일 주소와 어긋났을 때 알아채기 어려워집니다.
 */
internal object OsmStyle {

    const val TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

    fun json(): String = """
        {
          "version": 8,
          "sources": {
            "osm": {
              "type": "raster",
              "tiles": ["$TILE_URL"],
              "tileSize": 256,
              "maxzoom": 19,
              "attribution": "© OpenStreetMap"
            }
          },
          "layers": [
            { "id": "background", "type": "background", "paint": { "background-color": "#DEEAEF" } },
            { "id": "osm", "type": "raster", "source": "osm" }
          ]
        }
    """.trimIndent()
}
