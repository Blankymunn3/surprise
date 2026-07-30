package kr.surprise.memorymap.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.component.GlassIconButton
import kr.surprise.memorymap.core.designsystem.component.GlassSurface
import kr.surprise.memorymap.core.designsystem.component.MemoryFab
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PhotoThumb
import kr.surprise.memorymap.core.designsystem.component.PrimaryButton
import kr.surprise.memorymap.core.designsystem.component.SoftButton
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap

/**
 * 지도 탭. 지도가 **화면 끝까지** 차고, 조작하는 것만 그 위에 떠 있습니다.
 * 유리를 쓰는 유일한 자리입니다 (`docs/app/design.html`).
 *
 * 시트가 올라오면 FAB 과 지도 버튼도 **같이 올라갑니다** — 가려진 채로 남으면 못 누릅니다.
 */
@Composable
fun MapScreen(
    state: MapState,
    onIntent: (MapIntent) -> Unit,
    topBarHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    // 시트 높이는 **재서** 씁니다. 사진이 있느냐에 따라 시트가 훌쩍 달라지는데,
    // 고정값으로 두면 시트가 짧을 때 버튼만 허공에 뜹니다.
    var sheetHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    // 시트가 없을 때는 화면 아래에서 40dp. 있을 때는 시트 바로 위에 Gap.l 만큼 띄웁니다.
    val floatBottom =
        if (state.sheet == null) 40.dp else maxOf(40.dp, sheetHeight + Gap.l)

    Box(modifier.fillMaxSize().background(MemoryColors.MapSea)) {
        MapCanvas(
            pins = state.pins,
            focus = state.focus,
            onTap = { lat, lon -> onIntent(MapIntent.MapTapped(lat, lon)) },
            modifier = Modifier.fillMaxSize(),
        )

        SearchPill(
            query = state.query,
            onTyped = { onIntent(MapIntent.QueryTyped(it)) },
            onClear = { onIntent(MapIntent.QueryCleared) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = Gap.l, end = Gap.l, top = topBarHeight + Gap.s),
        )

        if (state.results.isNotEmpty()) {
            SearchResults(
                state = state,
                onPick = { onIntent(MapIntent.RegionChosen(it)) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = Gap.l, end = Gap.l, top = topBarHeight + 56.dp),
            )
        }

        // 왼쪽 아래: 확대·축소·내 위치
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = Gap.l, bottom = floatBottom),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            GlassSurface(modifier = Modifier.size(width = 40.dp, height = 80.dp)) {
                Column {
                    MapCtlButton(MemoryIcons.Plus, "확대") { }
                    MapCtlButton(MemoryIcons.Minus, "축소") { }
                }
            }
            GlassIconButton(
                icon = MemoryIcons.MyLocation,
                contentDescription = "내 위치",
                onClick = { onIntent(MapIntent.MyLocationTapped) },
                modifier = Modifier.size(40.dp),
            )
        }

        MemoryFab(
            onClick = { onIntent(MapIntent.AddPhotoTapped) },
            contentDescription = "사진 올리기",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Gap.xl, bottom = floatBottom),
        )

        state.sheet?.let { sheet ->
            RegionSheet(
                sheet = sheet,
                canSetCover = state.canSetCover(),
                onIntent = onIntent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { sheetHeight = with(density) { it.height.toDp() } },
            )
        }
    }
}

@Composable
private fun MapCtlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = MemoryColors.Ink, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SearchPill(
    query: String,
    onTyped: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = Gap.l, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(MemoryIcons.Search, contentDescription = null, tint = MemoryColors.Ink3, modifier = Modifier.size(17.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("지역 검색", style = MemoryType.Body, color = MemoryColors.Ink3)
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
                    contentDescription = "지우기",
                    tint = MemoryColors.Ink3,
                    modifier = Modifier.size(16.dp).clickable(onClick = onClear),
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    state: MapState,
    onPick: (kr.surprise.memorymap.core.model.Region) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.fillMaxWidth().height(260.dp), shape = MemoryShapes.Card) {
        LazyColumn {
            items(state.results, key = { it.code.value }) { region ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(region) }
                        .padding(horizontal = Gap.l, vertical = Gap.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(region.name, style = MemoryType.Body, modifier = Modifier.weight(1f))
                    region.parentName?.let {
                        Text(it, style = MemoryType.Label, color = MemoryColors.Ink3)
                    }
                }
            }
        }
    }
}

/** 시트는 **불투명 흰색**입니다. 유리로 만들면 뒤의 지도가 비쳐 사진이 지저분해 보입니다. */
@Composable
private fun RegionSheet(
    sheet: RegionSheetUi,
    canSetCover: Boolean,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MemoryShapes.Sheet)
            .background(MemoryColors.Surface)
            .padding(start = Gap.xl, end = Gap.xl, top = 10.dp, bottom = 34.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(MemoryShapes.Pill)
                .background(MemoryColors.Line2)
                .clickable { onIntent(MapIntent.SheetDismissed) }
        )
        Spacer(Modifier.height(Gap.l))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(sheet.region.name, style = MemoryType.Title)
            sheet.region.parentName?.let {
                Spacer(Modifier.size(Gap.s))
                Text(it, style = MemoryType.Label, color = MemoryColors.Ink3)
            }
            Spacer(Modifier.weight(1f))
            Text("사진 ${sheet.photos.size}장", style = MemoryType.Label, color = MemoryColors.Ink2)
        }
        Spacer(Modifier.height(14.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(Gap.s)) {
            items(sheet.photos, key = { it.id.value }) { photo ->
                PhotoThumb(
                    url = photo.downloadUrl,
                    isCover = photo.id == sheet.coverId,
                    dateLabel = "${photo.takenOn.monthValue}.${photo.takenOn.dayOfMonth}",
                    contentDescription = "${sheet.region.displayName} 사진",
                    onClick = { onIntent(MapIntent.PhotoTapped(photo.id)) },
                    modifier = Modifier
                        .size(92.dp)
                        .then(
                            if (photo.id == sheet.selected) {
                                Modifier.background(MemoryColors.AccentTint, MemoryShapes.Thumb)
                            } else {
                                Modifier
                            }
                        ),
                )
            }
        }

        Spacer(Modifier.height(Gap.l))

        Row(horizontalArrangement = Arrangement.spacedBy(Gap.s)) {
            SoftButton(
                text = "대표로 지정",
                onClick = { if (canSetCover) onIntent(MapIntent.SetCoverTapped) },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = "사진 추가",
                onClick = { onIntent(MapIntent.AddPhotoTapped) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
