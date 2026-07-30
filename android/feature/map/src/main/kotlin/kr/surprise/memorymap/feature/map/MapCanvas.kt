package kr.surprise.memorymap.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

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
                map.setStyle(Style.Builder().fromJson(OsmStyle.json())) {
                    applyPins(map, pins)
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

/**
 * 다녀온 지역에 표시를 찍습니다.
 *
 * 디자인은 지역을 **대표사진으로 칠하는** 것이지만, 그건 런타임 이미지를
 * `fill-pattern` 으로 등록해야 해서 다음 단계로 미룹니다. 지금은 자리와 개수만
 * 보여 주고, 누르면 시트에서 사진을 봅니다.
 */
private fun applyPins(map: MapLibreMap, pins: List<RegionPin>) {
    map.markers.forEach { map.removeMarker(it) }
    pins.forEach { pin ->
        map.addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(LatLng(pin.latitude, pin.longitude))
                .title(pin.region.displayName)
                .snippet("사진 ${pin.photoCount}장")
        )
    }
}
