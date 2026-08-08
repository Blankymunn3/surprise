package kr.surprise.memorymap.feature.map

/**
 * 지도 스타일을 코드로 만듭니다. **두 겹**입니다:
 *
 * ```
 * 라벨   ← CARTO light_only_labels, 원본 그대로 (도로명·지명이 또렷해야 한다)
 * 바탕   ← CARTO light_nolabels 를 TileProxy 가 픽셀화한 것
 * ```
 *
 * 라벨이 그림 안에 구워진 타일(OSM 기본)을 쓰면 픽셀화할 때 글자도 같이
 * 뭉개집니다. CARTO 는 바탕과 라벨을 따로 주기 때문에 **바탕만 픽셀**이 됩니다 —
 * 2026-08-08 검수된 시안("라이트 96")이 이 구성입니다.
 *
 * 스타일 파일을 따로 두지 않는 이유: 주소 몇 줄이 전부라, 파일로 빼면
 * iOS(`PhotoMap`)와 어긋났을 때 알아채기 어려워집니다.
 *
 * CARTO 무료 타일은 **출처 표기가 조건**입니다 — attribution 을 지우지 마세요.
 */
internal object OsmStyle {

    const val LABELS_LAYER = "labels"

    /** [proxyPort] 는 바탕 타일을 픽셀화해 주는 [TileProxy] 의 문. */
    fun json(proxyPort: Int): String = """
        {
          "version": 8,
          "sources": {
            "base": {
              "type": "raster",
              "tiles": ["http://127.0.0.1:$proxyPort/{z}/{x}/{y}"],
              "tileSize": 256,
              "maxzoom": 19,
              "attribution": "© OpenStreetMap contributors © CARTO"
            },
            "labels": {
              "type": "raster",
              "tiles": ["https://basemaps.cartocdn.com/light_only_labels/{z}/{x}/{y}.png"],
              "tileSize": 256,
              "maxzoom": 19
            }
          },
          "layers": [
            { "id": "background", "type": "background", "paint": { "background-color": "#EAE8E4" } },
            { "id": "base", "type": "raster", "source": "base" },
            { "id": "$LABELS_LAYER", "type": "raster", "source": "labels" }
          ]
        }
    """.trimIndent()
}
