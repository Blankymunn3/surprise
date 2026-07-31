package kr.surprise.memorymap.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
    onTap: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
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
    if (outline == null || outline.rings.isEmpty()) return

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
private const val OUTLINE_COLOR = "#E11D5B"

private fun RegionOutline.toGeoJson(): String = buildString {
    append("""{"type":"Feature","properties":{},"geometry":""")
    append("""{"type":"MultiLineString","coordinates":[""")
    rings.forEachIndexed { ringIndex, ring ->
        if (ringIndex > 0) append(',')
        append('[')
        ring.forEachIndexed { pointIndex, point ->
            if (pointIndex > 0) append(',')
            append('[').append(point[0]).append(',').append(point[1]).append(']')
        }
        append(']')
    }
    append("]}}")
}

/*
 * 다녀온 지역을 **대표사진으로 칠하는** 것은 런타임 이미지를 `fill-pattern` 으로
 * 등록해야 해서 다음 단계로 미뤘습니다 (`docs/app/STATUS.md`).
 */
