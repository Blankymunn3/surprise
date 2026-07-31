package kr.surprise.memorymap.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * 실제 지도. MapLibre 를 쓰는 이유는 **API 키도 결제 계정도 필요 없고**,
 * 웹에서 쓰는 OSM 타일을 그대로 쓸 수 있기 때문입니다 (`docs/app/ARCHITECTURE.md`).
 *
 * 지도를 누르면 좌표만 위로 올려보냅니다. 그 좌표가 어느 지역인지는
 * 도메인이 경계 데이터로 판정합니다 — 지도가 지역을 알 필요는 없습니다.
 */
@Composable
internal fun MapCanvas(
    pins: List<RegionPin>,
    focus: DoubleArray?,
    outline: RegionOutline?,
    fills: List<RegionFill>,
    onTap: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    // 대표사진을 받아 둡니다. 지도 스타일에 넣으려면 주소가 아니라 **그림 자체**가
    // 있어야 해서, 화면 쪽에서 미리 받아 놓고 넘깁니다.
    var covers by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    LaunchedEffect(fills) {
        val loader = ImageLoader(context)
        val loaded = LinkedHashMap<String, Bitmap>(covers)
        for (fill in fills) {
            if (loaded.containsKey(fill.coverUrl)) continue
            val request = ImageRequest.Builder(context)
                .data(fill.coverUrl)
                .size(COVER_PX, COVER_PX)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                loaded[fill.coverUrl] = result.image.toBitmap(COVER_PX, COVER_PX)
            }
        }
        if (loaded.size != covers.size) covers = loaded
    }

    // MapView 는 액티비티 생명주기를 직접 받아야 합니다. 안 하면 화면을 벗어날 때 샙니다.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { map ->
                map.setStyle(Style.Builder().fromJson(OsmStyle.json())) { style ->
                    paintRegions(style, fills, covers)
                    drawOutline(style, outline)
                }
                map.addOnMapClickListener { point ->
                    onTap(point.latitude, point.longitude)
                    true
                }
                focus?.let {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it[0], it[1]), 9.0))
                }
            }
        },
    )
}

/** 지도에 넣을 대표사진 크기. 크게 넣어 봐야 지역 안에서는 티가 안 나고 메모리만 먹습니다. */
private const val COVER_PX = 256

private const val FILL_SOURCE = "region-fill"
private const val FILL_LAYER = "region-fill-area"

private const val OUTLINE_SOURCE = "region-outline"
private const val OUTLINE_LAYER = "region-outline-line"

/**
 * 고른 지역에 **테두리**를 그립니다. 웹과 같은 표시입니다 —
 * "지금 이 지역을 보고 있다" 를 지도 위에서 알 수 있어야 합니다.
 *
 * 선은 GeoJSON 으로 넣습니다. 점을 하나씩 그리는 것보다 훨씬 적게 손대고,
 * 저장된 경계 데이터가 이미 GeoJSON 순서 `(경도, 위도)` 라 뒤집을 것도 없습니다.
 */
private fun drawOutline(style: Style, outline: RegionOutline?) {
    // 고른 지역이 없으면 지웁니다. 남겨 두면 다른 지역을 눌러도 옛 테두리가 남습니다.
    style.getLayer(OUTLINE_LAYER)?.let { style.removeLayer(it) }
    style.getSource(OUTLINE_SOURCE)?.let { style.removeSource(it) }
    if (outline == null || outline.polygons.isEmpty()) return

    style.addSource(GeoJsonSource(OUTLINE_SOURCE, outline.toGeoJson()))
    style.addLayer(
        LineLayer(OUTLINE_LAYER, OUTLINE_SOURCE).withProperties(
            PropertyFactory.lineColor(OUTLINE_COLOR),
            PropertyFactory.lineWidth(3f),
            PropertyFactory.lineJoin("round"),
            PropertyFactory.lineCap("round"),
        )
    )
}

/** `design.html` 의 강조색. 지도 위에서도 같은 색이어야 같은 앱으로 보입니다. */
private const val OUTLINE_COLOR = "#E0764F"

private fun RegionOutline.toGeoJson(): String =
    feature("MultiLineString", "[" + polygons.flatten().joinToString(",") { it.ring() } + "]")

/**
 * 다녀온 지역을 **그 지역의 대표사진으로 칠합니다.**
 *
 * 사진을 지도 스타일에 이미지로 등록하고, 지역 면을 그 이미지로 채웁니다
 * (`fill-pattern`). 사진은 타일처럼 반복되는데, 시군구 하나가 화면에서 그리 크지 않아
 * 대개 한 장으로 보입니다.
 *
 * 살짝 비치게(85%) 두는 이유: 완전히 덮으면 그 지역의 길·지명이 사라져서 어디인지
 * 알 수 없게 됩니다. 사진은 "다녀왔다" 는 표시이지 지도를 대신하는 것이 아닙니다.
 */
private fun paintRegions(style: Style, fills: List<RegionFill>, covers: Map<String, Bitmap>) {
    style.getLayer(FILL_LAYER)?.let { style.removeLayer(it) }
    style.getSource(FILL_SOURCE)?.let { style.removeSource(it) }

    val paintable = fills.filter { covers.containsKey(it.coverUrl) }
    if (paintable.isEmpty()) return

    paintable.forEach { fill ->
        covers[fill.coverUrl]?.let { style.addImage(fill.code, it) }
    }

    val features = paintable.joinToString(",") { fill ->
        feature("MultiPolygon", fill.polygons.joinToString(",", "[", "]") { polygon ->
            polygon.joinToString(",", "[", "]") { it.ring() }
        }, "\"pattern\":\"" + fill.code + "\"")
    }
    style.addSource(GeoJsonSource(FILL_SOURCE, """{"type":"FeatureCollection","features":[$features]}"""))

    style.addLayer(
        FillLayer(FILL_LAYER, FILL_SOURCE).withProperties(
            PropertyFactory.fillPattern(Expression.get("pattern")),
            PropertyFactory.fillOpacity(0.85f),
        )
    )
}

private fun feature(type: String, coordinates: String, properties: String = ""): String =
    """{"type":"Feature","properties":{$properties},"geometry":{"type":"$type","coordinates":$coordinates}}"""

private fun List<DoubleArray>.ring(): String =
    joinToString(",", "[", "]") { """[${it[0]},${it[1]}]""" }

