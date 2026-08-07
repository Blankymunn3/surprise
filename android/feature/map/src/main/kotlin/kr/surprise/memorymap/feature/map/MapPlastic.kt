package kr.surprise.memorymap.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PhotoThumb
import kr.surprise.memorymap.core.designsystem.theme.PlasticColors
import kr.surprise.memorymap.core.designsystem.theme.PlasticShapes
import kr.surprise.memorymap.core.designsystem.theme.PlasticSize
import kr.surprise.memorymap.core.designsystem.theme.Pretendard
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap
import kr.surprise.memorymap.core.designsystem.theme.pressable
import kr.surprise.memorymap.core.designsystem.theme.raisedPlastic
import kr.surprise.memorymap.core.designsystem.theme.sunken
import kr.surprise.memorymap.core.model.Region

/**
 * **시험용 화면 — 패미컴 컨트롤러 스타일의 지도.**
 *
 * 지금 앱의 지도는 [MapScreen] 이고, 이 파일은 같은 상태를 다른 옷으로 그린 것뿐입니다.
 * 상태·Intent 는 하나도 건드리지 않습니다.
 *
 * **바뀐 짜임새 — 조작하는 것이 지도 위에서 내려왔습니다.**
 *
 * 지금 화면은 지도가 전면을 덮고 검색칸·＋·줌 버튼이 그 위에 떠 있습니다. 이 스타일에서는
 * 지도를 몸통에 **끼운 화면**으로 다루고, 조작은 전부 몸통 위(화면 밖)로 내립니다 —
 * 컨트롤러의 버튼이 TV 화면 안에 있지 않은 것과 같습니다.
 *
 * 덤으로 얻는 것이 있습니다. 지금 화면은 지도 위에 얹은 것마다 [blockMapTouches] 로
 * 터치를 막아야 했는데(안 막으면 시트 버튼을 눌렀는데 뒤쪽 지역이 같이 선택됨),
 * 조작이 화면 밖으로 나가면 그 문제가 **아예 생기지 않습니다.** 지역 시트만 지도를 덮으므로
 * 거기에만 남겨 뒀습니다.
 *
 * 잃는 것도 있습니다 — **지도가 그만큼 좁아집니다.** 지도 앱에서 지도 넓이는 그냥 손해라,
 * 이 스타일을 채택할지 정할 때 가장 크게 저울질할 대목입니다.
 */
@Composable
internal fun PlasticMapBody(state: MapState, onIntent: (MapIntent) -> Unit) {
    var zoom by remember { mutableStateOf(ZoomNudge()) }
    var pan by remember { mutableStateOf(PanNudge()) }
    val keyboard = LocalSoftwareKeyboardController.current

    fun nudgeZoom(delta: Double) { zoom = ZoomNudge(zoom.serial + 1, delta) }
    fun nudgePan(dx: Float, dy: Float) { pan = PanNudge(pan.serial + 1, dx, dy) }

    Column(
        Modifier
            .fillMaxSize()
            .background(PlasticColors.Body)
            .padding(horizontal = Gap.s)
    ) {
        CartridgeSlot(
            query = state.query,
            onTyped = { onIntent(MapIntent.QueryTyped(it)) },
            onClear = { onIntent(MapIntent.QueryCleared) },
        )

        // 끼운 화면. 지도와 지역 시트가 **둘 다 이 안에** 있습니다 —
        // 시트가 몸통 위로 올라오면 화면 밖에 그림이 그려지는 꼴이라 어색합니다.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .sunken(PlasticShapes.Screen)
        ) {
            MapCanvas(
                pins = state.pins,
                focus = state.focus,
                focusCount = state.focusCount,
                outline = state.outline,
                fills = state.fills,
                myLocation = state.myLocation,
                onTap = { lat, lon ->
                    keyboard?.hide()
                    onIntent(MapIntent.MapTapped(lat, lon))
                },
                modifier = Modifier.fillMaxSize(),
                zoom = zoom,
                pan = pan,
            )

            // 검색 결과는 화면 **안** 위쪽에 겹칩니다. 슬롯 바로 아래에 두면
            // 몸통 위에 종이가 붙은 것처럼 떠 보입니다.
            if (state.results.isNotEmpty()) {
                SlotResults(
                    results = state.results,
                    onPick = { onIntent(MapIntent.RegionChosen(it)) },
                    modifier = Modifier.align(Alignment.TopCenter).padding(Gap.s).blockMapTouches(),
                )
            }

            state.sheet?.let { sheet ->
                PlasticRegionSheet(
                    sheet = sheet,
                    onIntent = onIntent,
                    modifier = Modifier.align(Alignment.BottomCenter).blockMapTouches(),
                )
            }
        }

        Pad(
            onZoomIn = { nudgeZoom(ZOOM_STEP) },
            onZoomOut = { nudgeZoom(-ZOOM_STEP) },
            onPan = ::nudgePan,
            onMyLocation = { onIntent(MapIntent.MyLocationTapped) },
            onAdd = { onIntent(MapIntent.AddPhotoTapped) },
        )
    }
}

/**
 * **카트리지 슬롯 = 검색칸.**
 *
 * 컨트롤러가 아니라 본체에서 가져온 형태입니다. 위쪽에 가로로 길게 파인 홈이 있고
 * 거기에 무언가를 꽂는다 — 지역을 찾아 넣는 자리로 이만한 그림이 없습니다.
 */
@Composable
private fun CartridgeSlot(query: String, onTyped: (String) -> Unit, onClear: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = Gap.s, bottom = Gap.s)
            .sunken(PlasticShapes.Chip, face = PlasticColors.PlateLo)
            .padding(horizontal = Gap.m, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap.s),
    ) {
        // 슬롯 왼쪽의 작은 홈. 돋보기 아이콘 대신입니다 — 이 판 위에서는
        // 아이콘 하나가 떠 보이는데, 파인 홈은 슬롯의 일부로 읽힙니다.
        Box(
            Modifier
                .size(width = 3.dp, height = 14.dp)
                .clip(PlasticShapes.Chip)
                .background(PlasticColors.Ink)
        )
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("지역 검색 — 강릉, 제주…", style = slotStyle, color = PlasticColors.OnPlateDim)
            }
            BasicTextField(
                value = query,
                onValueChange = onTyped,
                singleLine = true,
                textStyle = slotStyle.copy(color = PlasticColors.OnPlate),
                cursorBrush = SolidColor(PlasticColors.Red),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Text(
                text = "×",
                style = slotStyle.copy(fontSize = 17.sp),
                color = PlasticColors.OnPlateDim,
                modifier = Modifier.pressable(onClick = onClear).padding(horizontal = Gap.xs),
            )
        }
    }
}

private val slotStyle = TextStyle(
    fontFamily = Pretendard,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
)

@Composable
private fun SlotResults(
    results: List<Region>,
    onPick: (Region) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .clip(PlasticShapes.Chip)
            .background(PlasticColors.Plate)
    ) {
        LazyColumn {
            items(results, key = { it.code.value }) { region ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(region) }
                        .padding(horizontal = Gap.m, vertical = 11.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(Gap.s),
                ) {
                    Text(region.name, style = slotStyle, color = PlasticColors.OnPlate)
                    region.parentName?.let {
                        Text(
                            text = it,
                            fontFamily = Pretendard,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = PlasticColors.OnPlateDim,
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(PlasticColors.PlateLo))
            }
        }
    }
}

/**
 * 아래 조작부 — **실물 컨트롤러의 배치 그대로**입니다.
 *
 * ```
 *  ┌─ 검정 페이스플레이트 ──────────────────────┐
 *  │   ✛ 십자키        ▭ ▭          ● ●        │
 *  │   지도 이동      축소 확대     B    A       │
 *  │                              내위치 올리기  │
 *  └────────────────────────────────────────────┘
 * ```
 *
 * 지금 화면 그대로 **회색 몸통 위**에 놓습니다. 실물은 버튼이 검정 페이스플레이트
 * 위에 있지만, 그 판을 여기 깔면 위의 지도 화면과 같은 검정이 두 덩어리가 되어
 * 어느 쪽이 화면인지 흐려집니다. 여기서는 배치와 아이콘만 실물에서 가져옵니다.
 *
 * **A·B 글자는 붙이지 않습니다.** 실물에는 버튼 아래에 빨간 A·B 가 찍혀 있지만,
 * 사진첩 앱에서는 그 글자가 무엇을 하는 버튼인지 알려 주지 않습니다 — 목록 화면에서
 * SELECT·START 를 뺀 것과 같은 이유입니다. 대신 하는 일을 아이콘으로 그립니다.
 */
@Composable
private fun Pad(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onPan: (Float, Float) -> Unit,
    onMyLocation: () -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gap.xs, vertical = Gap.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DPad(
            onUp = { onPan(0f, -PAN_STEP) },
            onDown = { onPan(0f, PAN_STEP) },
            onLeft = { onPan(-PAN_STEP, 0f) },
            onRight = { onPan(PAN_STEP, 0f) },
        )

        Spacer(Modifier.weight(1f))

        // 가운데 고무 알약 둘 — 실물의 SELECT · START 자리입니다. 그 글자는 안 씁니다.
        Box(Modifier.raisedPlastic(PlasticShapes.Housing).padding(PlasticSize.ButtonInset)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Gap.xs)) {
                Pill(MemoryIcons.Minus, "축소", onZoomOut)
                Pill(MemoryIcons.Plus, "확대", onZoomIn)
            }
        }

        Spacer(Modifier.weight(1f))

        // B · A — 실물처럼 B 가 왼쪽입니다. 자주 쓰는 쪽(올리기)이 A 인 것도 실물과 같습니다:
        // 오른쪽 끝 버튼이 엄지가 가장 편히 닿는 자리입니다.
        Box(Modifier.raisedPlastic(PlasticShapes.Housing).padding(PlasticSize.ButtonInset)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Gap.s)) {
                RedButton(MemoryIcons.MyLocation, "내 위치", onMyLocation)
                RedButton(MemoryIcons.Plus, "사진 올리기", onAdd)
            }
        }
    }
}

/** 고무 알약. 실물에서 SELECT·START 가 있던 자리이고, 여기서는 확대·축소입니다. */
@Composable
private fun Pill(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = PlasticSize.PillWidth, height = PlasticSize.PillHeight)
            .clip(PlasticShapes.Pill)
            .background(PlasticColors.Rubber)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = PlasticColors.OnRubber, modifier = Modifier.size(14.dp))
    }
}

/** 빨간 A·B 버튼. */
@Composable
private fun RedButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(PlasticSize.RedButton)
            .clip(PlasticShapes.Pill)
            .background(PlasticColors.Red)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = PlasticColors.OnRed, modifier = Modifier.size(18.dp))
    }
}

/**
 * 십자키. 한 덩어리 고무 위에 눌리는 자리 넷을 얹습니다. **네 팔이 다 지도를 밉니다.**
 *
 * 실물처럼 **십자 모양 하나**로 만들려면 가운데 세로 기둥과 가로 들보를 겹쳐 놓고
 * 그 위에 누를 자리를 배치해야 합니다. 모서리 네 곳은 판이 비쳐 보이는 빈칸입니다.
 *
 * 가운데는 **누르는 자리가 아닙니다.** 실물에서도 십자키 한가운데는 그냥 플라스틱이라
 * 눌러도 아무 일이 없고, 여기서도 화살표 넷이 이미 할 일을 다 나눠 가졌습니다.
 */
@Composable
private fun DPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    Box(Modifier.raisedPlastic(PlasticShapes.Housing).padding(PlasticSize.ButtonInset)) {
        DPadFace(onUp, onDown, onLeft, onRight)
    }
}

@Composable
private fun DPadFace(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    BoxWithConstraints(Modifier.size(PlasticSize.Cross)) {
        val arm = maxWidth / 3

        // 고무 십자 — 세로 기둥과 가로 들보를 겹칩니다.
        Box(
            Modifier
                .width(arm)
                .fillMaxHeight()
                .align(Alignment.Center)
                .clip(PlasticShapes.Knob)
                .background(PlasticColors.Rubber)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(arm)
                .align(Alignment.Center)
                .clip(PlasticShapes.Knob)
                .background(PlasticColors.Rubber)
        )

        // 누르는 자리. 십자 밖(모서리)에는 아무것도 두지 않습니다.
        Arm(Alignment.TopCenter, arm, MemoryIcons.ChevronUp, "위로", onUp)
        Arm(Alignment.BottomCenter, arm, MemoryIcons.ChevronDown, "아래로", onDown)
        Arm(Alignment.CenterStart, arm, MemoryIcons.ChevronLeft, "왼쪽으로", onLeft)
        Arm(Alignment.CenterEnd, arm, MemoryIcons.ChevronRight, "오른쪽으로", onRight)

        // 가운데의 오목한 원. 실물의 그 원이고, 누르는 곳은 아닙니다.
        Box(
            Modifier
                .size(arm)
                .align(Alignment.Center)
                .clip(PlasticShapes.Pill)
                .background(PlasticColors.Ink),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(PlasticSize.DotCore)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.RubberHi)
            )
        }
    }
}

@Composable
private fun BoxScope.Arm(
    at: Alignment,
    arm: Dp,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(arm).align(at).pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = PlasticColors.OnRubber, modifier = Modifier.size(12.dp))
    }
}

/**
 * 지역 시트 — 끼운 화면 **안에서** 아래부터 올라오는 검정 판.
 *
 * 몸통 색(회색)을 쓰지 않습니다. 화면 안에 몸통 색이 나타나면 플라스틱이 화면을
 * 뚫고 올라온 것처럼 보입니다. 화면 안의 것은 화면 색으로 그립니다.
 */
@Composable
private fun PlasticRegionSheet(
    sheet: RegionSheetUi,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(PlasticShapes.Screen)
            .background(PlasticColors.Plate)
            .padding(start = Gap.m, end = Gap.m, top = Gap.m, bottom = Gap.l),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(Gap.s),
                ) {
                    Text(
                        text = sheet.region.name,
                        fontFamily = Pretendard,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = PlasticColors.OnPlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    sheet.region.parentName?.let {
                        Text(
                            text = it,
                            fontFamily = Pretendard,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = PlasticColors.OnPlateDim,
                        )
                    }
                }
                Text(
                    text = "사진 ${sheet.photos.size}장 · 누르면 대표사진이 돼요",
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = PlasticColors.OnPlateDim,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // 닫기는 검은 고무 버튼입니다 — 빨강은 주 동작에만.
            Box(
                Modifier
                    .size(PlasticSize.SheetClose)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.Rubber)
                    .pressable { onIntent(MapIntent.SheetDismissed) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "×",
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = PlasticColors.OnRubber,
                )
            }
        }

        Spacer(Modifier.height(Gap.m))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(Gap.s)) {
            items(sheet.photos, key = { it.id.value }) { photo ->
                PhotoThumb(
                    url = photo.downloadUrl,
                    isCover = photo.id == sheet.coverId,
                    dateLabel = "${photo.takenOn.monthValue}.${photo.takenOn.dayOfMonth}",
                    contentDescription = "${sheet.region.displayName} 사진",
                    onClick = { onIntent(MapIntent.PhotoTapped(photo.id)) },
                    modifier = Modifier.size(PlasticSize.SheetPhoto),
                )
            }
        }
    }
}

/** 십자키 좌·우 한 번에 미는 폭. 화면의 1/3 이면 밀린 것이 보이면서도 길을 잃지 않습니다. */
private const val PAN_STEP = 0.33f

/**
 * 지도 위에 얹은 것이 터치를 **먹게** 합니다. [MapScreen] 의 같은 이름과 같은 이유이고,
 * 이 스타일에서는 화면 안을 덮는 것(검색 결과·지역 시트)에만 필요합니다.
 */
private fun Modifier.blockMapTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Main).changes.forEach {
                if (!it.isConsumed) it.consume()
            }
        }
    }
}
