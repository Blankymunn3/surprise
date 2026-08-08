package kr.jjaguk.feature.map

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
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
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
import org.maplibre.android.geometry.LatLngQuad
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.ImageSource
import kotlin.math.pow

/**
 * 실제 지도. MapLibre 를 쓰는 이유는 **API 키도 결제 계정도 필요 없고**,
 * 웹에서 쓰는 OSM 타일을 그대로 쓸 수 있기 때문입니다 (`docs/app/ARCHITECTURE.md`).
 *
 * 지도를 누르면 좌표만 위로 올려보냅니다. 그 좌표가 어느 지역인지는
 * 도메인이 경계 데이터로 판정합니다 — 지도가 지역을 알 필요는 없습니다.
 */
@Composable
internal fun MapCanvas(
    focus: MapFocus?,
    focusCount: Int,
    outline: RegionOutline?,
    fills: List<RegionFill>,
    myLocation: MyPin?,
    onTap: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
    zoom: ZoomNudge = ZoomNudge(),
    pan: PanNudge = PanNudge(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    // 밤(19~07시)에는 어두운 지도. 지도를 **여는 순간** 정합니다 — 보고 있는
    // 중에 갈아엎으면 타일이 통째로 다시 오느라 화면이 껌뻑입니다.
    val night = remember { isNight() }
    // 바탕 타일을 픽셀화해 주는 서버. 지도가 사는 동안만 살립니다.
    val tileProxy = remember { TileProxy(dark = night) }
    DisposableEffect(tileProxy) {
        onDispose { tileProxy.close() }
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
                // **하드웨어 비트맵을 받으면 안 됩니다.** 화면에 그냥 그릴 때는 그쪽이
                // 빠르지만, 이 그림은 지도 스타일에 넣으려고 픽셀을 다시 읽습니다.
                // 하드웨어 비트맵은 픽셀이 GPU 에만 있어서 읽는 순간 앱이 죽습니다
                // (`can't create mutable bitmap with Config.HARDWARE`).
                .allowHardware(false)
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
    // 확대·축소도 **몇 번째 눌렀는지**로 셉니다. 방향만 보면 ＋ 를 연달아 눌러도
    // 값이 그대로라 두 번째부터 아무 일이 없습니다.
    var appliedZoom by remember { mutableStateOf(0) }
    var appliedPan by remember { mutableStateOf(0) }

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
                // **픽셀 지도**입니다 — 바탕은 픽셀, 라벨은 원본(2026-08-08 검수 시안).
                map.setStyle(Style.Builder().fromJson(OsmStyle.json(context, tileProxy.port, night))) { style ->
                    paintRegions(style, fills, covers)
                    drawOutline(style, outline)
                    // 내 자리는 칠보다 위입니다 — "지금 여기" 는 무엇에도 가리면 안 됩니다.
                    drawMyLocation(style, myLocation)
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

                if (zoom.serial != appliedZoom) {
                    appliedZoom = zoom.serial
                    map.animateCamera(CameraUpdateFactory.zoomBy(zoom.delta))
                }

                if (pan.serial != appliedPan) {
                    appliedPan = pan.serial
                    // **가운데를 옮겨서** 밉니다. `CameraUpdateFactory.scrollBy` 는
                    // 이 MapLibre 판에 없습니다.
                    //
                    // 한 화면이 덮는 경도는 배율이 1 오를 때마다 절반이 되므로
                    // `360 / 2^배율` 입니다. 그 비율만큼 옮기면 어느 배율에서든
                    // "화면의 1/3" 이 똑같이 느껴집니다.
                    val here = map.cameraPosition
                    here.target?.let { target ->
                        val perScreen = 360.0 / 2.0.pow(here.zoom)
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    // 위도는 ±85 를 넘으면 지도가 뒤집힙니다.
                                    (target.latitude - perScreen * pan.dy).coerceIn(-85.0, 85.0),
                                    target.longitude + perScreen * pan.dx,
                                ),
                                here.zoom,
                            )
                        )
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
 * 확대·축소 요청.
 *
 * [serial] 이 늘 때만 [delta] 만큼 배율을 옮깁니다. 방향만 들고 다니면 ＋ 를 연달아
 * 눌렀을 때 값이 그대로라 두 번째부터 아무 일도 일어나지 않습니다 — 지역 맞춤에
 * `focusCount` 를 두는 것과 같은 이유입니다.
 */
internal data class ZoomNudge(val serial: Int = 0, val delta: Double = 0.0)

/**
 * 지도 밀기 요청. [ZoomNudge] 와 같은 방식으로, [serial] 이 늘 때만 움직입니다.
 *
 * **십자키(패미컴 스타일) 때문에 생긴 것입니다.** 십자키는 팔이 넷인데 확대·축소만
 * 있으면 좌·우가 눌러도 아무 일이 없는 죽은 팔이 됩니다. 지도에서 십자키 좌·우가
 * 할 일은 미는 것 말고 없으니 그렇게 했습니다.
 *
 * [dx]·[dy] 는 **지금 보이는 넓이에 대한 비율**입니다 (0.33 이면 화면의 1/3).
 * 고정 도수로 밀면 확대했을 때는 화면 밖으로 날아가고 축소했을 때는 꿈쩍도 안 합니다.
 */
internal data class PanNudge(val serial: Int = 0, val dx: Float = 0f, val dy: Float = 0f)

/**
 * 고른 지역에 화면을 맞춥니다. 지도는 이미 가려지는 만큼 줄여 놓았으므로
 * 여기서는 **사방으로 조금씩만** 띄웁니다.
 */
private fun MapFocus.toUpdate(side: Int, vertical: Int): CameraUpdate = when (this) {
    is MapFocus.Spot ->
        CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), SPOT_ZOOM)

    is MapFocus.Me ->
        CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), ME_ZOOM)

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

/**
 * 내 위치의 배율. **동네가 보이는 정도**입니다 — 큰길과 동 이름이 읽힙니다.
 *
 * [SPOT_ZOOM] 보다 5단 높습니다(한 단마다 넓이가 절반이므로 32배 가깝습니다).
 * 지역을 고를 때는 "어디쯤" 이면 되지만 내 위치는 "지금 여기" 를 보는 것이라
 * 도 단위로 보이면 점만 찍히고 정작 내가 어디 있는지는 알 수 없습니다.
 */
private const val ME_ZOOM = 14.0

/** 지도에 넣을 대표사진 크기. 크게 넣어 봐야 지역 안에서는 티가 안 나고 메모리만 먹습니다. */
private const val COVER_PX = 512

/** 지역을 채우는 그림의 긴 변. 화면에서 시군구 하나만 한 크기라 넉넉해야 합니다. */
private const val FILL_PX = 512

private const val FILL_SOURCE = "region-fill"
private const val FILL_LAYER = "region-fill-area"

/**
 * 지역을 칠한 사진의 불투명도. iOS 와 **같은 85%** 입니다 — 라벨이 별도 타일이
 * 되면서 사진 **위**에 그려지므로, 더 비치게 할 이유가 사라졌습니다.
 */
private const val FILL_OPACITY = 0.85f

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

/**
 * 고른 지역 테두리의 색. 지도 위에서도 앱과 같은 색이어야 같은 앱으로 보입니다.
 *
 * 두 스타일이 쓰는 빨강이 다릅니다 — 기준 디자인은 `MemoryColors.Accent`,
 * 패미컴은 컨트롤러의 빨강입니다. 어두운 지도에서 이 선이 더 눈에 띕니다.
 */
private const val OUTLINE_COLOR = "#D8342A"

private fun RegionOutline.toGeoJson(): String =
    feature("MultiLineString", "[" + polygons.flatten().joinToString(",") { it.ring() } + "]")

/**
 * 다녀온 지역을 **그 지역의 대표사진 한 장으로 칠합니다.**
 *
 * 예전에는 `fill-pattern` 이었습니다. 그건 사진을 **타일처럼 반복**해서, 강남구 하나에
 * 같은 사진이 예닐곱 장 깔렸습니다. 사진이 무늬가 되어 버려서 무엇을 찍은 것인지
 * 알아볼 수가 없습니다. **대표사진은 한 장이니 한 번만 보여야 합니다.**
 *
 * 그래서 지역마다 사진을 그 **지역 모양으로 오려** 두고, 그 그림 한 장을 지역의
 * 경계 상자에 맞춰 지도에 얹습니다(`ImageSource`). 확대·축소를 해도 좌표에 붙어 있어
 * 늘 한 장입니다.
 *
 * **비치게 두는 이유** — 완전히 덮으면 그 지역의 길·지명이 사라져서 어디인지
 * 알 수 없게 됩니다. 사진은 "다녀왔다" 는 표시이지 지도를 대신하는 것이 아닙니다.
 *
 * iOS(85%)보다 **더 비칩니다**. iOS 는 벡터 지도라 길·지명이 **사진 위에 다시**
 * 그려지지만, 여기 OSM 타일은 그림 한 장이라 라벨이 **사진 뒤에** 깔립니다 —
 * 사진을 통해 비쳐야만 읽힙니다. 값이 달라야 두 앱의 보이는 결과가 같아집니다.
 */
private fun paintRegions(style: Style, fills: List<RegionFill>, covers: Map<String, Bitmap>) {
    // 지역마다 source·layer 가 하나씩이라 이름 앞머리로 찾아 걷어냅니다.
    style.layers.filter { it.id.startsWith(FILL_LAYER) }.forEach { style.removeLayer(it) }
    style.sources.filter { it.id.startsWith(FILL_SOURCE) }.forEach { style.removeSource(it) }

    fills.forEach { fill ->
        val cover = covers[fill.coverUrl] ?: return@forEach
        val box = fill.bounds() ?: return@forEach
        val cut = cover.cutTo(fill, box) ?: return@forEach

        val sourceId = FILL_SOURCE + fill.code
        style.addSource(
            ImageSource(
                sourceId,
                LatLngQuad(
                    LatLng(box.north, box.west),
                    LatLng(box.north, box.east),
                    LatLng(box.south, box.east),
                    LatLng(box.south, box.west),
                ),
                cut,
            )
        )
        // 라벨 레이어 **아래**에 끼웁니다 — 도로명·지명이 사진 위에 그대로 떠야
        // 채워진 지역에서도 어디인지 읽힙니다. iOS 도 같은 순서입니다.
        style.addLayerBelow(
            RasterLayer(FILL_LAYER + fill.code, sourceId)
                .withProperties(PropertyFactory.rasterOpacity(FILL_OPACITY)),
            OsmStyle.LABELS_LAYER,
        )
    }
}

/** 지역을 감싸는 네모. 사진을 얹을 자리입니다. */
private class GeoBox(val south: Double, val west: Double, val north: Double, val east: Double)

private fun RegionFill.bounds(): GeoBox? {
    var south = Double.MAX_VALUE
    var west = Double.MAX_VALUE
    var north = -Double.MAX_VALUE
    var east = -Double.MAX_VALUE
    polygons.forEach { polygon ->
        polygon.forEach { ring ->
            ring.forEach { point ->
                if (point[1] < south) south = point[1]
                if (point[1] > north) north = point[1]
                if (point[0] < west) west = point[0]
                if (point[0] > east) east = point[0]
            }
        }
    }
    if (north <= south || east <= west) return null
    return GeoBox(south, west, north, east)
}

/**
 * 사진을 지역 모양으로 오립니다.
 *
 * 사진은 네모고 지역은 아니라서, **가운데를 잘라 네모를 꽉 채운 뒤** 지역 밖을
 * 지웁니다. 늘이지 않는 이유: 사람이 찍힌 사진이 지역 모양대로 늘어나면 얼굴이 일그러집니다.
 *
 * 구멍(섬 안의 호수 같은 것)도 그대로 뚫립니다 — GeoJSON 의 두 번째 고리부터가
 * 구멍이고, `EVEN_ODD` 가 그걸 알아서 처리합니다.
 */
private fun Bitmap.cutTo(fill: RegionFill, box: GeoBox): Bitmap? {
    // 경계 상자의 가로세로 비. 위도가 높을수록 경도 1도가 좁아지므로 그만큼 줄입니다.
    val midLat = Math.toRadians((box.north + box.south) / 2)
    val wide = (box.east - box.west) * kotlin.math.cos(midLat)
    val tall = box.north - box.south
    if (wide <= 0.0 || tall <= 0.0) return null

    val w: Int
    val h: Int
    if (wide >= tall) {
        w = FILL_PX
        h = (FILL_PX * tall / wide).toInt().coerceAtLeast(1)
    } else {
        h = FILL_PX
        w = (FILL_PX * wide / tall).toInt().coerceAtLeast(1)
    }

    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)

    val path = Path()
    path.fillType = Path.FillType.EVEN_ODD
    fill.polygons.forEach { polygon ->
        polygon.forEach { ring ->
            ring.forEachIndexed { index, point ->
                val x = ((point[0] - box.west) / (box.east - box.west) * w).toFloat()
                val y = ((box.north - point[1]) / (box.north - box.south) * h).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }
    }
    canvas.clipPath(path)

    // 가운데 자르기 — 짧은 쪽에 맞춰 키우고 넘치는 만큼 양옆(또는 위아래)을 버립니다.
    val scale = maxOf(w.toFloat() / width, h.toFloat() / height)
    val cropW = (w / scale).toInt().coerceIn(1, width)
    val cropH = (h / scale).toInt().coerceIn(1, height)
    val src = Rect(
        (width - cropW) / 2,
        (height - cropH) / 2,
        (width - cropW) / 2 + cropW,
        (height - cropH) / 2 + cropH,
    )
    canvas.drawBitmap(this, src, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
    return out
}

private fun feature(type: String, coordinates: String, properties: String = ""): String =
    """{"type":"Feature","properties":{$properties},"geometry":{"type":"$type","coordinates":$coordinates}}"""

private const val ME_SOURCE = "my-location"
private const val ME_HALO_LAYER = "my-location-halo"
private const val ME_DOT_LAYER = "my-location-dot"

/**
 * **내가 지금 있는 자리.** 점 하나와 그것을 감싸는 옅은 원입니다.
 *
 * 지역 표시(딱지)와 다르게 생겨야 합니다 — 그건 "사진이 있는 곳" 이고 이건 "나" 라서,
 * 같은 모양이면 다녀온 지역 하나가 더 있는 것으로 읽힙니다. 그래서 네모가 아니라 **원**이고,
 * 유일하게 테두리가 흰색입니다.
 *
 * 자리를 찾은 뒤에도 **남아 있습니다.** 지도를 밀고 나서 다시 찾아갈 수 있어야 합니다.
 * 배율을 바꿔도 크기가 그대로라, 넓게 보면 점 하나로 작게 남습니다.
 */
private fun drawMyLocation(style: Style, me: MyPin?) {
    style.getLayer(ME_DOT_LAYER)?.let { style.removeLayer(it) }
    style.getLayer(ME_HALO_LAYER)?.let { style.removeLayer(it) }
    style.getSource(ME_SOURCE)?.let { style.removeSource(it) }
    if (me == null) return

    style.addSource(
        GeoJsonSource(ME_SOURCE, feature("Point", "[${me.longitude},${me.latitude}]"))
    )

    // 옅은 원이 먼저(아래), 점이 나중(위)입니다. 순서가 바뀌면 점이 원에 덮입니다.
    style.addLayer(
        CircleLayer(ME_HALO_LAYER, ME_SOURCE).withProperties(
            PropertyFactory.circleRadius(ME_HALO_RADIUS),
            PropertyFactory.circleColor(ME_COLOR),
            PropertyFactory.circleOpacity(0.18f),
        )
    )
    style.addLayer(
        CircleLayer(ME_DOT_LAYER, ME_SOURCE).withProperties(
            PropertyFactory.circleRadius(ME_DOT_RADIUS),
            PropertyFactory.circleColor(ME_COLOR),
            PropertyFactory.circleStrokeWidth(2f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
        )
    )
}

/** 내 자리 표시의 색. 앱의 레드입니다 — 지도 위에서 유일하게 "지금" 을 뜻하는 색입니다. */
private const val ME_COLOR = "#D8342A"
private const val ME_DOT_RADIUS = 6f
private const val ME_HALO_RADIUS = 20f

private fun List<DoubleArray>.ring(): String =
    joinToString(",", "[", "]") { """[${it[0]},${it[1]}]""" }

/** 밤인가 — 19시부터 아침 7시 전까지. 일출·일몰 계산까지는 이 앱엔 과합니다. */
private fun isNight(): Boolean = java.time.LocalTime.now().hour.let {
    it >= MapTuning.nightStartHour || it < MapTuning.nightEndHour
}
