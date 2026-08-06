package kr.surprise.memorymap.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kr.surprise.memorymap.domain.CalendarMonth
import kr.surprise.memorymap.core.designsystem.component.DayCell
import kr.surprise.memorymap.core.designsystem.component.FRAMES_RATIO
import kr.surprise.memorymap.core.designsystem.component.PhotoFramesScene
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PhotoThumb
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 요일 이름. 리소스 배열에서 읽습니다 — **순서가 뜻을 가지므로** 배열이어야 합니다
 * (요일 번호로 꺼내 씁니다). 일요일부터입니다.
 */
@Composable
private fun weekdays(): List<String> = stringArrayResource(R.array.calendar_weekdays).toList()

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
    // 격자와 '달력 접기' 는 **붙박이**고 아래 목록만 구릅니다. 목록을 내리는데
    // 달력까지 같이 밀려 올라가면, 지금 무슨 달을 보고 있는지가 사라집니다.
    Column(modifier.fillMaxSize().background(MemoryColors.Paper)) {
        MonthHeader(state, onIntent)

        if (!state.collapsed) {
            WeekdayRow()
            MonthGrid(state, onIntent)
        }

        CollapseBar(state.collapsed) { onIntent(CalendarIntent.CollapseToggled) }
        Divider()

        val groups = state.visibleDays()
        if (groups.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                PhotoFramesScene(Modifier.fillMaxWidth(0.42f).aspectRatio(FRAMES_RATIO))
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.calendar_empty), style = MemoryType.Title)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = Gap.xl, end = Gap.xl, top = Gap.m, bottom = 90.dp),
            ) {
                items(groups.size, key = { groups[it].date.toString() }) { index ->
                    DaySection(groups[index], state.selected == groups[index].date, onIntent)
                }
            }
        }
    }
}

/** 구획선은 2px 입니다. 테두리(1px)보다 굵어야 '나누는 선' 으로 읽힙니다. */
@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Gap.xl)
            .height(MemoryStroke.Divider)
            .background(MemoryColors.Line2)
    )
}

@Composable
private fun MonthHeader(state: CalendarState, onIntent: (CalendarIntent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = Gap.xl, end = Gap.xl, top = Gap.s, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap.s),
    ) {
        // 연·월을 **한 덩어리**로 씁니다. 월만 크고 연도가 작으면 연도가 딸린
        // 주석처럼 보이는데, 지난 해를 넘겨 볼 때는 연도가 더 중요합니다.
        Text(
            stringResource(R.string.calendar_month, state.month.year, state.month.monthValue),
            style = MemoryType.Title,
            modifier = Modifier.weight(1f),
        )
        NavButton(MemoryIcons.ChevronLeft, stringResource(R.string.calendar_previous_month)) { onIntent(CalendarIntent.PreviousMonth) }
        NavButton(MemoryIcons.ChevronRight, stringResource(R.string.calendar_next_month)) { onIntent(CalendarIntent.NextMonth) }
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .background(MemoryColors.Surface)
            .border(MemoryStroke.Border, MemoryColors.Line)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = MemoryColors.Ink, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun WeekdayRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = Gap.xl)) {
        weekdays().forEachIndexed { index, label ->
            Text(
                text = label,
                style = MemoryType.Micro,
                // 일요일만 딥레드입니다. 레드는 주 동작에 쓰는 색이라, 눌러야 할 것이
                // 아닌 자리에는 한 단계 어두운 쪽을 씁니다.
                color = if (index == 0) MemoryColors.AccentDeep else MemoryColors.Ink2,
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

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = Gap.xl).padding(top = 6.dp)) {
        // 높이를 미리 정합니다. 달마다 줄 수가 달라 그때그때 재면 넘길 때 화면이 출렁입니다.
        val cell = (maxWidth - CELL_GAP * 6) / 7
        val gridHeight = cell * WEEK_ROWS + CELL_GAP * (WEEK_ROWS - 1)

        HorizontalPager(state = pager, modifier = Modifier.height(gridHeight)) { page ->
            val month = monthAt(page)
            val cells = if (month == state.month) {
                state.cells
            } else {
                CalendarMonth.grid(month).map { DayUi(it, null, isToday = false, isSunday = it != null && CalendarMonth.isSunday(it)) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                cells.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
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
                                        isSelected = state.selected == date,
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

/** 칸 사이. 3dp 면 칸끼리 붙지 않으면서도 달력이 한 덩어리로 보입니다. */
private val CELL_GAP = 3.dp

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
    // 가운데가 아니라 **왼끝**입니다. 위의 연·월, 아래 날짜 묶음과 왼쪽 선이 맞아야
    // 세 덩어리가 한 화면으로 읽힙니다.
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = Gap.xl, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap.xs),
    ) {
        Text(
            stringResource(if (collapsed) R.string.calendar_expand else R.string.calendar_collapse),
            style = MemoryType.Micro,
            color = MemoryColors.Ink2,
        )
        Icon(
            if (collapsed) MemoryIcons.ChevronDown else MemoryIcons.ChevronUp,
            contentDescription = null,
            tint = MemoryColors.Ink2,
            modifier = Modifier.size(12.dp),
        )
    }
}

/**
 * 날짜 하나와 그날 사진들.
 *
 * 고른 날은 **왼쪽에 레드 선**이 섭니다. 칸을 통째로 칠하거나 테두리를 두르면
 * 사진들이 상자에 갇힌 것처럼 보이는데, 선 하나면 "여기" 만 짚어 줍니다.
 */
@Composable
private fun DaySection(group: DayGroup, isSelected: Boolean, onIntent: (CalendarIntent) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = Gap.l)) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (isSelected) MemoryColors.Accent else MemoryColors.Paper)
        )

        Column(Modifier.padding(start = 10.dp)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Gap.s),
            ) {
                Text(
                    stringResource(
                        R.string.calendar_day_header,
                        group.date.monthValue,
                        group.date.dayOfMonth,
                        weekdays()[group.date.dayOfWeek.value % 7],
                    ),
                    style = MemoryType.Body,
                )
                // 달력은 "언제" 를 보는 화면이지만, 그 사진이 어디였는지가 늘 따라옵니다.
                Text(
                    text = listOfNotNull(
                        group.placeName,
                        stringResource(R.string.calendar_photo_count, group.photos.size),
                    ).joinToString(" · "),
                    style = MemoryType.Micro,
                    color = MemoryColors.Ink2,
                )
            }

            Spacer(Modifier.height(7.dp))

            // 한 줄에 몇 장인지 정하지 않고 **칸 크기를 고정**해 흘려 담습니다.
            // 셋으로 나누면 폰이 넓어질수록 사진만 커지는데, 크기가 같아야 눈이 훑기 쉽습니다.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                group.photos.forEach { photo ->
                    PhotoThumb(
                        url = photo.downloadUrl,
                        isCover = photo.id == group.coverId,
                        contentDescription = stringResource(R.string.calendar_photo_description, group.date.toString()),
                        onClick = { onIntent(CalendarIntent.PhotoLongPressed(group.date, photo.id)) },
                        modifier = Modifier.size(76.dp),
                    )
                }
            }
        }
    }
}
