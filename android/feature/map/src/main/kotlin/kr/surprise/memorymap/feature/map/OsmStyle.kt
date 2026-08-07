package kr.surprise.memorymap.feature.map

/**
 * MapLibre 스타일을 코드로 만듭니다. **웹과 같은 OSM 타일**을 씁니다 —
 * API 키도, 결제 계정도 필요 없고 두 화면이 같은 지도를 보여 줍니다.
 *
 * 스타일 파일을 따로 두지 않는 이유: 타일 주소 한 줄이 전부라, 파일로 빼면
 * 웹의 타일 주소와 어긋났을 때 알아채기 어려워집니다.
 *
 * **색을 눕히고 종이를 한 겹 덮습니다.** OSM 타일은 그림 파일이라 색을 직접 못 바꾸는데,
 * 채도를 낮추고 위에 종이색을 옅게 깔면 앱의 다른 화면과 같은 결이 됩니다
 * (`docs/app/design.html`). 지도 자체를 새로 그리려면 벡터 타일이 필요하고,
 * 그건 서버가 있어야 합니다.
 */
internal object OsmStyle {

    const val TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

    /**
     * **어두운 지도**입니다. 지도는 검정 판에 끼운 화면이라, 그 안에서 하얀 지도가
     * 혼자 빛나면 화면이 아니라 구멍처럼 보입니다.
     *
     * 그림 타일의 색을 바꾸는 방법이 하나뿐입니다 — **밝기를 뒤집는 것**
     * (`brightness-min: 1`, `max: 0`). 흰 종이에 검은 길이던 것이 검은 판에 흰 길이
     * 됩니다. 글자도 같이 뒤집혀 어두운 바탕에 밝은 글씨가 되므로 그대로 읽힙니다.
     *
     * 뒤집으면 색상이 보색으로 돌아갑니다(초록 공원 → 붉은 자국). 그래서 채도를
     * 거의 다 뺍니다 — 어차피 이 앱의 화면은 잿빛 한 벌입니다.
     */
    fun json(): String = """
        {
          "version": 8,
          "sources": { ${source()} },
          "layers": [
            { "id": "background", "type": "background", "paint": { "background-color": "#2A2A2A" } },
            {
              "id": "osm",
              "type": "raster",
              "source": "osm",
              "paint": {
                "raster-saturation": -0.92,
                "raster-contrast": -0.18,
                "raster-brightness-min": 1,
                "raster-brightness-max": 0,
                "raster-opacity": 0.9
              }
            },
            {
              "id": "paper",
              "type": "background",
              "paint": { "background-color": "#262626", "background-opacity": 0.22 }
            }
          ]
        }
    """.trimIndent()

    private fun source(): String = """
        "osm": {
          "type": "raster",
          "tiles": ["$TILE_URL"],
          "tileSize": 256,
          "maxzoom": 19,
          "attribution": "© OpenStreetMap"
        }
    """.trimIndent()
}
