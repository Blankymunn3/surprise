package kr.surprise.memorymap.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kr.surprise.memorymap.domain.CalendarMonth
import kr.surprise.memorymap.core.designsystem.component.DayCell
import kr.surprise.memorymap.core.designsystem.component.HillScene
import kr.surprise.memorymap.core.designsystem.component.SCENE_RATIO
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PhotoThumb
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap
import java.time.YearMonth
import java.time.temporal.ChronoUnit

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
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HillScene(Modifier.fillMaxWidth(0.66f).aspectRatio(SCENE_RATIO).clip(MemoryShapes.Card))
                    Spacer(Modifier.height(Gap.l))
                    Text(
                        "이 달엔 아직 사진이 없어요",
                        style = MemoryType.Body,
                        color = MemoryColors.Ink3,
                        textAlign = TextAlign.Center,
                    )
                }
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

/**
 * 옆으로 넘겨 달을 바꿉니다.
 *
 * 페이지 수를 아주 크게 잡고 가운데에서 시작합니다 — 무한히 넘기는 것처럼 보이게 하는
 * 흔한 방법입니다. 실제로 그리는 건 화면에 보이는 한 장뿐입니다.
 *
 * **보이는 달만** 사진을 채워 그립니다. 옆 페이지는 상태에 없는 달이라 사진을 모르는데,
 * 빈 격자를 잠깐 보여 주는 편이 엉뚱한 달의 사진을 보여 주는 것보다 낫습니다.
 */
@Composable
private fun MonthGrid(state: CalendarState, onIntent: (CalendarIntent) -> Unit) {
    val anchor = rememberSaveable(saver = YearMonthSaver) { state.month }
    val pager = rememberPagerState(initialPage = PAGE_CENTER, pageCount = { PAGE_COUNT })

    fun monthAt(page: Int): YearMonth = anchor.plusMonths((page - PAGE_CENTER).toLong())

    // 넘김이 멈춘 뒤에만 알립니다. 손가락을 따라가며 매번 다시 그리면 화면이 떨립니다.
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }
            .map { monthAt(it) }
            .distinctUntilChanged()
            .collect { onIntent(CalendarIntent.MonthSelected(it)) }
    }

    // 위 화살표로 달을 바꿨을 때 페이지도 따라오게
    LaunchedEffect(state.month) {
        val target = PAGE_CENTER + ChronoUnit.MONTHS.between(anchor, state.month).toInt()
        if (target != pager.currentPage && target in 0 until PAGE_COUNT) {
            pager.animateScrollToPage(target)
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        // 높이를 미리 정합니다. 달마다 줄 수가 달라 그때그때 재면 넘길 때 화면이 출렁입니다.
        val cell = (maxWidth - Gap.xs * 6) / 7
        val gridHeight = cell * WEEK_ROWS + Gap.xs * (WEEK_ROWS - 1)

        HorizontalPager(state = pager, modifier = Modifier.height(gridHeight)) { page ->
            val month = monthAt(page)
            val cells = if (month == state.month) {
                state.cells
            } else {
                CalendarMonth.grid(month).map { DayUi(it, null, isToday = false, isSunday = it != null && CalendarMonth.isSunday(it)) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Gap.xs)) {
                cells.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Gap.xs)) {
                        week.forEach { dayCell ->
                            Box(Modifier.weight(1f)) {
                                val date = dayCell.date
                                if (date == null) {
                                    Spacer(Modifier.fillMaxWidth().height(cell))
                                } else {
                                    DayCell(
                                        day = date.dayOfMonth,
                                        photoUrl = dayCell.coverUrl,
                                        isToday = dayCell.isToday,
                                        isSunday = dayCell.isSunday,
                                        onClick = { onIntent(CalendarIntent.DayTapped(date)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 6줄이면 어떤 달이든 들어갑니다. 늘 6줄로 그려야 넘길 때 높이가 안 바뀝니다. */
private const val WEEK_ROWS = 6

/** ±100년. 넘기다 끝에 닿을 일은 없습니다. */
private const val PAGE_COUNT = 2401
private const val PAGE_CENTER = PAGE_COUNT / 2

/** 화면이 다시 만들어져도 기준 달이 흔들리지 않게 저장합니다. */
private val YearMonthSaver = Saver<YearMonth, String>(
    save = { it.toString() },
    restore = { YearMonth.parse(it) },
)

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
