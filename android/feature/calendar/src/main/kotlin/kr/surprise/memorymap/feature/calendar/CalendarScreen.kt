package kr.surprise.memorymap.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.component.DayCell
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PhotoThumb
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap

private val WEEKDAYS = listOf("일", "월", "화", "수", "목", "금", "토")

/**
 * 달력 탭. 지도 탭과 **같은 밝은 바탕**입니다 —
 * 탭 하나 옮겼다고 앱이 뒤집히면 안 됩니다.
 */
@Composable
fun CalendarScreen(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MemoryColors.Paper),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        item { MonthHeader(state, onIntent) }

        if (!state.collapsed) {
            item { WeekdayRow() }
            item { MonthGrid(state, onIntent) }
        }

        item { CollapseBar(state.collapsed) { onIntent(CalendarIntent.CollapseToggled) } }

        val groups = state.visibleDays()
        if (groups.isEmpty()) {
            item {
                Text(
                    "이 달엔 아직 사진이 없어요",
                    style = MemoryType.Body,
                    color = MemoryColors.Ink3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                )
            }
        }

        items(groups.size, key = { groups[it].date.toString() }) { index ->
            DaySection(groups[index], onIntent)
        }
    }
}

@Composable
private fun MonthHeader(state: CalendarState, onIntent: (CalendarIntent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = Gap.m, top = Gap.l, bottom = Gap.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "3월" 크게, "2026" 작게 — 대부분 올해를 보고 연도는 확인용입니다
        Text("${state.month.monthValue}월", style = MemoryType.Display)
        Spacer(Modifier.size(Gap.s))
        Text(
            state.month.year.toString(),
            style = MemoryType.Body,
            color = MemoryColors.Ink3,
            modifier = Modifier.weight(1f),
        )
        NavButton(MemoryIcons.ChevronLeft, "이전 달") { onIntent(CalendarIntent.PreviousMonth) }
        NavButton(MemoryIcons.ChevronRight, "다음 달") { onIntent(CalendarIntent.NextMonth) }
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(MemoryShapes.Pill).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = MemoryColors.Ink2, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun WeekdayRow() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = Gap.s),
        horizontalArrangement = Arrangement.spacedBy(Gap.xs),
    ) {
        WEEKDAYS.forEachIndexed { index, label ->
            Text(
                text = label,
                style = MemoryType.Micro,
                color = if (index == 0) MemoryColors.Accent else MemoryColors.Ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(state: CalendarState, onIntent: (CalendarIntent) -> Unit) {
    // 칸 수가 정해져 있어(최대 6줄) 스크롤 없는 격자로 그립니다
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(Gap.xs),
    ) {
        state.cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(Gap.xs)) {
                week.forEach { cell ->
                    Box(Modifier.weight(1f)) {
                        val date = cell.date
                        if (date == null) {
                            Spacer(Modifier.fillMaxWidth().height(0.dp))
                        } else {
                            DayCell(
                                day = date.dayOfMonth,
                                photoUrl = cell.coverUrl,
                                isToday = cell.isToday,
                                isSunday = cell.isSunday,
                                onClick = { onIntent(CalendarIntent.DayTapped(date)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapseBar(collapsed: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = Gap.l)
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (collapsed) "달력 펼치기" else "달력 접기",
            style = MemoryType.Label,
            color = MemoryColors.Ink2,
        )
        Spacer(Modifier.size(6.dp))
        Icon(
            if (collapsed) MemoryIcons.ChevronDown else MemoryIcons.ChevronUp,
            contentDescription = null,
            tint = MemoryColors.Ink2,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun DaySection(group: DayGroup, onIntent: (CalendarIntent) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = Gap.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${group.date.monthValue}월 ${group.date.dayOfMonth}일", style = MemoryType.Headline)
            Spacer(Modifier.size(Gap.s))
            Text(WEEKDAYS[group.date.dayOfWeek.value % 7], style = MemoryType.Label, color = MemoryColors.Ink3)

            // 달력은 "언제" 를 보는 화면이지만 그 사진이 어디였는지가 늘 따라옵니다
            group.placeName?.let { place ->
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(MemoryShapes.Pill)
                        .background(MemoryColors.AccentTint)
                        .padding(horizontal = 11.dp, vertical = 4.dp)
                ) {
                    Text(place, style = MemoryType.Micro, color = MemoryColors.Accent)
                }
            }
        }

        Spacer(Modifier.height(9.dp))

        // 한 줄에 3장. 4장을 넣으면 썸네일이 90dp 아래로 내려가 얼굴이 안 보입니다.
        group.photos.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = Gap.xs),
                horizontalArrangement = Arrangement.spacedBy(Gap.xs),
            ) {
                row.forEach { photo ->
                    PhotoThumb(
                        url = photo.downloadUrl,
                        isCover = photo.id == group.coverId,
                        contentDescription = "${group.date} 사진",
                        onClick = { onIntent(CalendarIntent.PhotoLongPressed(group.date, photo.id)) },
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
