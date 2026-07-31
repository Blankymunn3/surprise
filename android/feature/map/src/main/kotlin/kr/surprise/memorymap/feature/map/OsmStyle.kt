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
            { "id": "background", "type": "background", "paint": { "background-color": "#DCEBE0" } },
            {
              "id": "osm",
              "type": "raster",
              "source": "osm",
              "paint": {
                "raster-saturation": -0.45,
                "raster-contrast": -0.12,
                "raster-brightness-min": 0.06,
                "raster-opacity": 0.92
              }
            },
            {
              "id": "paper",
              "type": "background",
              "paint": { "background-color": "#EFE3CB", "background-opacity": 0.2 }
            }
          ]
        }
    """.trimIndent()
}
