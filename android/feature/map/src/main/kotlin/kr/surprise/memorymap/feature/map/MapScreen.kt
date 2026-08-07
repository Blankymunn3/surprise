package kr.surprise.memorymap.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.component.FloatingSurface
import kr.surprise.memorymap.core.designsystem.component.MemoryFab
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PhotoThumb
import kr.surprise.memorymap.core.designsystem.component.PrimaryButton
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap

/** 지도 위 물건들의 가장자리 여백. 시안이 정한 값입니다. */
private val Edge = 14.dp

/**
 * 한 번 누를 때 옮기는 배율. 1단계면 넓이가 절반이 됩니다 — 두 손가락으로 벌리는 것과
 * 비슷한 폭입니다. 패미컴 스타일 지도(`MapPlastic.kt`)도 같은 값을 씁니다 —
 * 코틀린에서 최상위 `private` 은 파일 안까지라 `internal` 이어야 보입니다.
 */
internal const val ZOOM_STEP = 1.0

/**
 * 지도 탭. 지도가 이 칸을 **꽉 채우고**, 조작하는 것만 그 위에 떠 있습니다.
 *
 * 지도 위 상주물은 **검색칸과 ＋ 둘뿐입니다.** 나머지(줌·내 위치)는 조작 중에만
 * 쓰는 것이라 왼쪽 아래로 몰아 두고, 지역 시트는 눌렀을 때만 올라옵니다.
 *
 * 시트가 올라오면 ＋ 와 지도 버튼도 **같이 올라갑니다** — 가려진 채로 남으면 못 누릅니다.
 */
@Composable
fun MapScreen(
    state: MapState,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 패미컴 스타일 시험 중에는 짜임새가 통째로 다릅니다 — 조작하는 것이
    // 지도 위가 아니라 몸통 위(화면 밖)에 섭니다. 스위치는 designsystem 에 하나뿐입니다.
    Box(modifier.fillMaxSize()) { PlasticMapBody(state, onIntent) }
}

/**
 * 지도 위에 떠 있는 것들이 터치를 **먹게** 합니다.
 *
 * 지도는 AndroidView(MapLibre) 라 자기 방식으로 터치를 받습니다. 그 위에 얹은 Compose
 * 요소가 터치를 소비하지 않으면 **밑의 지도까지 같이 눌립니다** — 시트 안의 버튼을 눌렀는데
 * 시트 뒤쪽 지역이 함께 선택되던 것이 이것 때문입니다.
 *
 * **Main 단계**에서 먹습니다. 이 단계는 자식부터 위로 올라오므로, 글자칸·버튼이 **먼저**
 * 받고 남은 것만 우리가 먹습니다. Initial 에서 먹으면 자식한테 가기도 전에 가로채서
 * 검색창을 눌러도 자판이 안 올라오고 시트 버튼도 안 눌립니다.
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

@Composable
private fun MapCtlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    FloatingSurface(modifier = Modifier.size(40.dp)) {
        Box(
            Modifier.matchParentSize().clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = MemoryColors.Ink, modifier = Modifier.size(18.dp))
        }
    }
}

/** 지도 위 검색칸. 흰 면에 1px 잉크 선 — 반투명이 아닙니다. */
@Composable
private fun SearchField(
    query: String,
    onTyped: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingSurface(modifier = modifier.fillMaxWidth().height(44.dp)) {
        Row(
            Modifier.matchParentSize().padding(horizontal = Gap.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Gap.s),
        ) {
            Icon(
                MemoryIcons.Search,
                contentDescription = null,
                tint = MemoryColors.Ink,
                modifier = Modifier.size(15.dp),
            )
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(stringResource(R.string.map_search_placeholder), style = MemoryType.Body, color = MemoryColors.Ink3)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onTyped,
                    singleLine = true,
                    textStyle = MemoryType.Body.copy(color = MemoryColors.Ink),
                    cursorBrush = SolidColor(MemoryColors.Accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    MemoryIcons.Close,
                    contentDescription = stringResource(R.string.map_search_clear),
                    tint = MemoryColors.Ink,
                    modifier = Modifier.size(15.dp).clickable(onClick = onClear),
                )
            }
        }
    }
}

/**
 * 검색 결과. **사진이 있는 지역이 먼저** 옵니다 — 이미 다녀온 곳을 다시 찾는 일이
 * 새 곳을 찾는 일보다 훨씬 잦습니다. 그 순서는 뷰모델이 정하고 여기서는 그리기만 합니다.
 */
@Composable
private fun SearchResults(
    state: MapState,
    onPick: (kr.surprise.memorymap.core.model.Region) -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingSurface(modifier = modifier.fillMaxWidth().heightIn(max = 260.dp)) {
        LazyColumn {
            items(state.results, key = { it.code.value }) { region ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(region) }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(Gap.s),
                ) {
                    Text(region.name, style = MemoryType.Body)
                    region.parentName?.let {
                        Text(it, style = MemoryType.Micro, color = MemoryColors.Ink2)
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(MemoryStroke.Border)
                        .background(MemoryColors.Fill)
                )
            }
        }
    }
}

/**
 * 지역 시트. **위쪽에만 2px 잉크 선**을 긋고 나머지는 흰 면입니다.
 *
 * 손잡이(작은 막대)를 두지 않습니다 — 이 시트는 끌어 올리는 것이 아니라 지역을
 * 누르면 나타났다가 × 로 닫는 것이라, 끌 수 있게 생기면 안 됩니다.
 */
@Composable
private fun RegionSheet(
    sheet: RegionSheetUi,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MemoryColors.Surface)
            .padding(top = MemoryStroke.Divider),
    ) {
        // 시트 맨 위 선. Column 의 padding 위에 얹어 가로를 꽉 채웁니다.
        Box(
            Modifier
                .fillMaxWidth()
                .height(MemoryStroke.Divider)
                .background(MemoryColors.Ink)
        )

        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 26.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Gap.s)) {
                        Text(sheet.region.name, style = MemoryType.Title)
                        sheet.region.parentName?.let {
                            Text(it, style = MemoryType.Micro, color = MemoryColors.Ink2)
                        }
                    }
                    Text(
                        text = stringResource(R.string.map_sheet_count, sheet.photos.size),
                        style = MemoryType.Label,
                        color = MemoryColors.Ink2,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(
                    Modifier
                        .size(34.dp)
                        .border(MemoryStroke.Border, MemoryColors.Line)
                        .clickable { onIntent(MapIntent.SheetDismissed) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        MemoryIcons.Close,
                        contentDescription = stringResource(R.string.map_sheet_close),
                        tint = MemoryColors.Ink,
                        modifier = Modifier.size(15.dp),
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
                        contentDescription = stringResource(R.string.map_sheet_photo_description, sheet.region.displayName),
                        onClick = { onIntent(MapIntent.PhotoTapped(photo.id)) },
                        modifier = Modifier.size(92.dp),
                    )
                }
            }

            // 누르면 바로 대표가 되므로, 그렇다고 **말해 줘야** 합니다. 버튼이 없으니
            // 알려 주지 않으면 누를 수 있다는 것 자체를 모릅니다.
            Text(
                text = stringResource(R.string.map_sheet_cover_hint),
                style = MemoryType.Micro,
                color = MemoryColors.Ink2,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(Gap.m))

            PrimaryButton(
                text = stringResource(R.string.map_sheet_add),
                onClick = { onIntent(MapIntent.AddPhotoTapped) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
