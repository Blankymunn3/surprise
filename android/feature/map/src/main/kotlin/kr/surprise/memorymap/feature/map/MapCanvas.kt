package kr.surprise.memorymap.feature.map

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdate
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
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
    focus: MapFocus?,
    focusCount: Int,
    outline: RegionOutline?,
    fills: List<RegionFill>,
    onTap: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    // 대표사진을 받아 둡니다. 지도 스타일에 넣으려면 주소가 아니라 **그림 자체**가
    // 있어야 해서, 화면 쪽에서 미리 받아 놓고 넘깁니다.
    var covers by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    LaunchedEffect(fills) {
        // 앱이 만들어 둔 **싱글턴 로더**를 씁니다. 여기서 새로 만들면 토큰을 다는
        // 인터셉터가 빠져서 대표사진만 조용히 안 뜹니다 (`docs/app/AUTH.md`).
        val loader = SingletonImageLoader.get(context)
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

    // 탭을 듣는 것은 **한 번만** 답니다. 그래도 최신 콜백을 부르도록 여기서 붙잡아 둡니다.
    val currentOnTap by rememberUpdatedState(onTap)

    /**
     * 지도의 실제 크기. **화면에 놓이기 전에는 0 입니다.**
     *
     * `AndroidView` 의 `update` 는 크기가 잡히기 전에 먼저 불릴 수 있고, 그 뒤로 다시
     * 불린다는 보장이 없습니다. 그래서 크기가 바뀌는 것을 직접 듣고 상태로 들고 있습니다 —
     * 이 값이 바뀌면 `update` 가 다시 돌아 그때 카메라를 맞춥니다.
     */
    var mapSize by remember { mutableStateOf(0 to 0) }
    DisposableEffect(mapView) {
        val listener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val next = view.width to view.height
            if (next != mapSize) mapSize = next
        }
        mapView.addOnLayoutChangeListener(listener)
        onDispose { mapView.removeOnLayoutChangeListener(listener) }
    }

    // 이미 맞춘 화면을 기억합니다. `update` 는 다시 그릴 때마다 불리는데, 그때마다 지도를
    // 움직이면 시트를 만지기만 해도 지도가 도로 튕겨 갑니다.
    //
    // **몇 번째 맞춤인지**·**시트가 덮는 높이**·**지도 크기**를 함께 봅니다. 같은 지역을
    // 다시 골라도 횟수가 늘어 다시 맞추고, 시트 높이나 지도 크기를 뒤늦게 알게 되면
    // 그 값으로 다시 맞춥니다.
    var applied by remember { mutableStateOf<Focused?>(null) }
    var listening by remember { mutableStateOf(false) }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            val (width, height) = mapSize
            // 가려지는 자리는 **지도를 놓을 때 이미 빼 뒀습니다**(`MapScreen`).
            // 여기서는 테두리가 가장자리에 딱 붙지 않을 만큼만 띄웁니다.
            val side = with(density) { EDGE.roundToPx() }
            val vertical = with(density) { EDGE_VERTICAL.roundToPx() }

            view.getMapAsync { map ->
                map.setStyle(Style.Builder().fromJson(OsmStyle.json())) { style ->
                    paintRegions(style, fills, covers)
                    drawOutline(style, outline)
                }

                // `update` 마다 달면 한 번 눌러도 여러 번 눌린 것이 됩니다.
                if (!listening) {
                    listening = true
                    map.addOnMapClickListener { point ->
                        currentOnTap(point.latitude, point.longitude)
                        true
                    }
                }

                // **크기를 알기 전에는 맞추지 않습니다.** 0 인 채로 맞추면 지도가 고른
                // 지역보다 훨씬 크게 확대됩니다 — 넣을 화면이 없으니 배율이 끝까지 올라갑니다.
                if (focus != null && width > 0 && height > 0) {
                    val next = Focused(focusCount, width, height)
                    if (applied != next) {
                        applied = next
                        map.animateCamera(focus.toUpdate(side, vertical))
                    }
                }
            }
        },
    )
}

/**
 * 어떤 조건으로 화면을 맞췄는지. 하나라도 달라지면 다시 맞춥니다.
 *
 * **지도 크기**가 들어 있는 것이 중요합니다 — 시트가 뜨고 지면 지도가 줄었다 늘고,
 * 그때마다 다시 맞춰야 고른 지역이 계속 가운데에 있습니다.
 */
private data class Focused(val count: Int, val width: Int, val height: Int)

/**
 * 고른 지역에 화면을 맞춥니다. 지도는 이미 가려지는 만큼 줄여 놓았으므로
 * 여기서는 **사방으로 조금씩만** 띄웁니다.
 */
private fun MapFocus.toUpdate(side: Int, vertical: Int): CameraUpdate = when (this) {
    is MapFocus.Spot ->
        CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), SPOT_ZOOM)

    is MapFocus.Area -> CameraUpdateFactory.newLatLngBounds(
        LatLngBounds.from(north, east, south, west),
        side, vertical, side, vertical,
    )
}

/** 테두리가 화면 끝에 딱 붙으면 잘린 것처럼 보입니다. 좌우에 이만큼 여유를 둡니다. */
private val EDGE = 28.dp

/**
 * 위아래는 좌우보다 **더 띄웁니다.** 위에는 바가, 아래에는 시트가 붙어 있어서
 * 같은 여백을 주면 지역이 그 사이에 낀 것처럼 답답해 보입니다.
 */
private val EDGE_VERTICAL = 40.dp

/** 경계가 없는 장소의 배율. 맞출 넓이가 없어 정해 둡니다 — 전에 쓰던 값 그대로입니다. */
private const val SPOT_ZOOM = 9.0

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

