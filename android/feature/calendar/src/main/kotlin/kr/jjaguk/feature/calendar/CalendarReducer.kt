package kr.jjaguk.feature.calendar

import kr.jjaguk.core.model.Region
import kr.jjaguk.domain.CalendarMonth
import kr.jjaguk.domain.model.PhotoBoard
import java.time.LocalDate
import java.time.YearMonth

/**
 * 달력 화면의 상태 계산. **순수 함수**라 시간·네트워크를 쓰지 않고 today 를 인자로 받습니다.
 */
internal object CalendarReducer {

    fun rebuild(
        state: CalendarState,
        board: PhotoBoard,
        regionNames: Map<String, Region>,
    ): CalendarState {
        val cells = CalendarMonth.grid(state.month).map { date ->
            DayUi(
                date = date,
                coverUrl = date?.let { board.dayCover(it)?.downloadUrl },
                isToday = date == state.today,
                isSunday = date != null && CalendarMonth.isSunday(date),
            )
        }

        // 그달에 사진이 있는 날만, 최근 날짜부터
        val groups = board.byDay
            .filterKeys { YearMonth.from(it) == state.month }
            .entries
            .sortedByDescending { it.key }
            .map { (date, photos) ->
                DayGroup(
                    date = date,
                    placeName = photos.firstOrNull()
                        ?.let { regionNames[it.regionCode.value]?.displayName },
                    photos = photos,
                    coverId = board.dayCover(date)?.id,
                )
            }

        return state.copy(cells = cells, days = groups)
    }

    fun monthChanged(state: CalendarState, delta: Long): CalendarState =
        state.copy(month = state.month.plusMonths(delta), selected = null)

    fun monthSelected(state: CalendarState, month: YearMonth): CalendarState =
        if (month == state.month) state else state.copy(month = month, selected = null)

    fun daySelected(state: CalendarState, date: LocalDate): CalendarState =
        state.copy(selected = date)

    fun collapseToggled(state: CalendarState): CalendarState =
        state.copy(collapsed = !state.collapsed)
}

/** 고른 날이 있으면 그 날만, 없으면 그달 전부. */
internal fun CalendarState.visibleDays(): List<DayGroup> =
    selected?.let { picked -> days.filter { it.date == picked } }.orEmpty().ifEmpty { days }
