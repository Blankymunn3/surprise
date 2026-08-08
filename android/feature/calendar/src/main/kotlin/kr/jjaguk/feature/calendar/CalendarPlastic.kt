package kr.jjaguk.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kr.jjaguk.core.designsystem.component.FRAMES_RATIO
import kr.jjaguk.core.designsystem.component.PhotoFramesScene
import kr.jjaguk.core.designsystem.component.PhotoThumb
import kr.jjaguk.core.designsystem.theme.PlasticColors
import kr.jjaguk.core.designsystem.theme.PlasticShapes
import kr.jjaguk.core.designsystem.theme.PlasticSize
import kr.jjaguk.core.designsystem.theme.Galmuri11
import kr.jjaguk.core.designsystem.theme.AppFont
import kr.jjaguk.core.designsystem.theme.Space as Gap
import kr.jjaguk.core.designsystem.theme.pressable
import kr.jjaguk.core.designsystem.theme.raisedPlastic
import kr.jjaguk.core.designsystem.theme.sunken
import kr.jjaguk.domain.CalendarMonth
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * **달력 — 패미컴 컨트롤러 스타일.**
 *
 * 짜임새는 지도와 같습니다 — 몸통 위에 화면을 끼우고, 조작은 화면 밖입니다.
 * 다만 달력은 **화면 안이 두 층**입니다: 위는 격자, 아래는 그날 사진들.
 * 두 층 사이에 홈을 파서 나눕니다 (선을 긋지 않습니다 — 이 판에서는 선보다 홈입니다).
 *
 * **칸마다 베벨을 주지 않습니다.** 42칸에 전부 두르면 화면이 자글자글해져서
 * 정작 사진이 안 보입니다. 베벨은 기기와 화면의 경계에만 둡니다.
 * 칸은 색과 밝기로만 가릅니다.
 */
@Composable
internal fun PlasticCalendarBody(state: CalendarState, onIntent: (CalendarIntent) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(PlasticColors.Body)
            .padding(horizontal = Gap.s)
    ) {
        MonthBar(state, onIntent)

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .sunken(PlasticShapes.Screen)
                .padding(Gap.s)
        ) {
            if (!state.collapsed) {
                WeekdayStrip()
                Spacer(Modifier.height(Gap.xs))
                PlasticGrid(state, onIntent)
                // 격자와 목록 사이의 홈. 판을 파낸 자국이라 두 층이 갈립니다.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = Gap.s)
                        .height(2.dp)
                        .clip(PlasticShapes.Chip)
                        .background(PlasticColors.PlateLo)
                )
            }

            val groups = state.visibleDays()
            if (groups.isEmpty()) {
                // 남는 세로를 **weight 로** 받습니다. 위에 격자가 이미 자리를 먹은
                // Column 안이라, fillMaxSize 로 두면 부모보다 커져 아래가 잘립니다.
                Box(Modifier.weight(1f)) { EmptyPlate() }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = Gap.m),
                ) {
                    items(groups.size, key = { groups[it].date.toString() }) { index ->
                        PlasticDaySection(
                            group = groups[index],
                            isSelected = state.selected == groups[index].date,
                            onIntent = onIntent,
                        )
                    }
                }
            }
        }

        Bottom(
            collapsed = state.collapsed,
            onToggle = { onIntent(CalendarIntent.CollapseToggled) },
            onAdd = { onIntent(CalendarIntent.AddTapped) },
        )
    }
}

/**
 * 몸통 위의 연·월과 달 넘김.
 *
 * 화살표 두 개는 **몸통에 앉힌 작은 버튼**입니다. 컨트롤러의 SELECT·START 자리에
 * 있던 그 모양인데, 여기서는 무엇을 하는지가 화살표로 드러나므로 라벨을 붙일 필요가
 * 없습니다 (목록 화면에서 SELECT·START 글자를 뺀 것과 같은 이유입니다).
 */
@Composable
private fun MonthBar(state: CalendarState, onIntent: (CalendarIntent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gap.xs, vertical = Gap.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap.s),
    ) {
        // 연·월을 한 덩어리로. 지난 해를 넘겨 볼 때는 연도가 더 중요합니다.
        Text(
            text = stringResource(R.string.calendar_month, state.month.year, state.month.monthValue),
            fontFamily = AppFont,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = PlasticColors.Ink,
            modifier = Modifier.weight(1f),
        )
        MonthNav("‹") { onIntent(CalendarIntent.PreviousMonth) }
        MonthNav("›") { onIntent(CalendarIntent.NextMonth) }
    }
}

@Composable
private fun MonthNav(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(PlasticSize.MonthNav)
            .clip(PlasticShapes.Knob)
            .background(PlasticColors.Rubber)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            fontFamily = AppFont,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = PlasticColors.OnRubber,
        )
    }
}

@Composable
private fun WeekdayStrip() {
    Row(Modifier.fillMaxWidth()) {
        weekdays().forEachIndexed { index, label ->
            Text(
                text = label,
                // 요일 줄은 각인 — 갈무리11 (2026-08-09 검수 시안)
                fontFamily = Galmuri11,
                fontSize = 11.sp,
                // 일요일만 빨강입니다. 이 판에서 빨강은 주 동작 색이 아니라
                // 몸통 위에서만 그렇고, 검정 화면 안에서는 그냥 잘 보이는 색입니다.
                color = if (index == 0) PlasticColors.RedHi else PlasticColors.OnPlateDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 격자. 넘기는 방식은 [CalendarScreen] 의 것과 같습니다 —
 * 페이지를 아주 많이 잡고 가운데에서 시작해 무한히 넘기는 것처럼 보이게 합니다.
 */
@Composable
private fun PlasticGrid(state: CalendarState, onIntent: (CalendarIntent) -> Unit) {
    val anchor = rememberSaveable(saver = YearMonthSaver) { state.month }
    val pager = rememberPagerState(initialPage = PAGE_CENTER, pageCount = { PAGE_COUNT })

    fun monthAt(page: Int): YearMonth = anchor.plusMonths((page - PAGE_CENTER).toLong())

    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }
            .map { monthAt(it) }
            .distinctUntilChanged()
            .collect { onIntent(CalendarIntent.MonthSelected(it)) }
    }

    LaunchedEffect(state.month) {
        val target = PAGE_CENTER + ChronoUnit.MONTHS.between(anchor, state.month).toInt()
        if (target != pager.currentPage && target in 0 until PAGE_COUNT) {
            pager.animateScrollToPage(target)
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // 칸을 붙여 놓습니다 — 사이를 띄우면 42개가 흩어져 보이고, 붙이면
        // 판에 새긴 격자 하나로 읽힙니다. 칸끼리는 밝기로 갈립니다.
        val cell = (maxWidth / 7).coerceAtLeast(PlasticSize.DayCell)
        val gridHeight = cell * WEEK_ROWS

        HorizontalPager(state = pager, modifier = Modifier.height(gridHeight)) { page ->
            val month = monthAt(page)
            val cells = if (month == state.month) {
                state.cells
            } else {
                CalendarMonth.grid(month).map {
                    DayUi(it, null, isToday = false, isSunday = it != null && CalendarMonth.isSunday(it))
                }
            }

            Column {
                cells.chunked(7).forEach { week ->
                    Row {
                        week.forEach { dayCell ->
                            Box(Modifier.weight(1f).height(cell)) {
                                val date = dayCell.date
                                if (date != null) {
                                    PlasticDayCell(
                                        cell = dayCell,
                                        date = date,
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

/**
 * 달력 한 칸.
 *
 * **테두리를 두르지 않습니다.** 42칸에 선을 두르면 격자가 아니라 그물이 됩니다.
 * 대신 세 가지로 가릅니다:
 * - 사진 있는 날 → 사진이 칸을 채움
 * - 오늘 → 숫자만 빨강
 * - 고른 날 → 칸 바닥이 밝아짐 (눌러서 들어간 자리처럼)
 */
@Composable
private fun PlasticDayCell(
    cell: DayUi,
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(1.dp)
            .clip(PlasticShapes.Chip)
            .background(if (isSelected) PlasticColors.PlateHi else PlasticColors.PlateLo)
            .pressable(onClick = onClick)
    ) {
        cell.coverUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Text(
            text = date.dayOfMonth.toString(),
            // 달력 숫자는 A안(갈무리11) — 2026-08-09 검수. 크기는 11의 배수.
            fontFamily = Galmuri11,
            fontSize = 11.sp,
            color = when {
                cell.isToday -> PlasticColors.RedHi
                cell.isSunday -> PlasticColors.RedLo
                cell.coverUrl != null -> PlasticColors.OnRed
                else -> PlasticColors.OnPlateDim
            },
            modifier = Modifier.align(Alignment.TopStart).padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

/**
 * 날짜 하나와 그날 사진들.
 *
 * 고른 날은 **왼쪽에 빨간 선**이 섭니다 — 기준 디자인과 같은 방식입니다.
 * 칸을 통째로 칠하면 사진들이 상자에 갇힌 것처럼 보입니다.
 */
@Composable
private fun PlasticDaySection(
    group: DayGroup,
    isSelected: Boolean,
    onIntent: (CalendarIntent) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(bottom = Gap.m)) {
        Box(
            Modifier
                .width(3.dp)
                .heightIn(min = PlasticSize.DayCell)
                .background(if (isSelected) PlasticColors.Red else PlasticColors.Plate)
        )

        Column(Modifier.padding(start = Gap.s)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Gap.s),
            ) {
                Text(
                    text = stringResource(
                        R.string.calendar_day_header,
                        group.date.monthValue,
                        group.date.dayOfMonth,
                        weekdays()[group.date.dayOfWeek.value % 7],
                    ),
                    fontFamily = AppFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PlasticColors.OnPlate,
                )
                Text(
                    text = listOfNotNull(
                        group.placeName,
                        stringResource(R.string.calendar_photo_count, group.photos.size),
                    ).joinToString(" · "),
                    fontFamily = AppFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = PlasticColors.OnPlateDim,
                )
            }

            Spacer(Modifier.height(Gap.xs))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Gap.xs),
                verticalArrangement = Arrangement.spacedBy(Gap.xs),
            ) {
                group.photos.forEach { photo ->
                    PhotoThumb(
                        url = photo.downloadUrl,
                        isCover = photo.id == group.coverId,
                        contentDescription = stringResource(R.string.calendar_photo_description, group.date.toString()),
                        onClick = { onIntent(CalendarIntent.PhotoLongPressed(group.date, photo.id)) },
                        modifier = Modifier.size(PlasticSize.CalendarPhoto),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPlate() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = Gap.l),
        verticalArrangement = Arrangement.Center,
    ) {
        PhotoFramesScene(Modifier.fillMaxWidth(0.42f).aspectRatio(FRAMES_RATIO))
        Spacer(Modifier.height(Gap.m))
        Text(
            text = stringResource(R.string.calendar_empty),
            fontFamily = AppFont,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = PlasticColors.OnPlate,
        )
    }
}

/**
 * 아래 조작부 — 왼쪽 접기(고무 알약), 오른쪽 사진 올리기(빨간 A 버튼).
 *
 * 지도의 [kr.jjaguk.feature.map.MapScreen] 조작부와 **오른쪽이 같습니다.**
 * 두 탭에서 빨간 버튼이 같은 자리에서 같은 일을 해야 손이 기억합니다.
 */
@Composable
private fun Bottom(collapsed: Boolean, onToggle: () -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gap.xs, vertical = Gap.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap.m),
    ) {
        Box(
            Modifier
                .weight(1f)
                .raisedPlastic(PlasticShapes.Housing)
                .padding(PlasticSize.ButtonInset)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(PlasticSize.Button)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.Rubber)
                    .pressable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(if (collapsed) R.string.calendar_expand else R.string.calendar_collapse),
                    fontFamily = AppFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PlasticColors.OnRubber,
                )
            }
        }

        Box(Modifier.raisedPlastic(PlasticShapes.Housing).padding(PlasticSize.ButtonInset)) {
            Box(
                Modifier
                    .size(PlasticSize.Button)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.Red)
                    .pressable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "＋",
                    fontFamily = AppFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = PlasticColors.OnRed,
                )
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
