package kr.surprise.memorymap.feature.calendar

import kr.surprise.memorymap.core.model.Photo
import kr.surprise.memorymap.core.model.PhotoId
import kr.surprise.memorymap.core.model.SpaceId
import java.time.LocalDate
import java.time.YearMonth

data class CalendarState(
    val spaceId: SpaceId,
    val month: YearMonth,
    val today: LocalDate,
    val cells: List<DayUi> = emptyList(),
    val days: List<DayGroup> = emptyList(),
    /** 접힘은 **기억합니다.** 다른 탭에 갔다 와도 접힌 채로 돌아와야 접는 의미가 있습니다. */
    val collapsed: Boolean = false,
    val selected: LocalDate? = null,
)

data class DayUi(
    val date: LocalDate?,
    val coverUrl: String?,
    val isToday: Boolean,
    val isSunday: Boolean,
)

data class DayGroup(
    val date: LocalDate,
    val placeName: String?,
    val photos: List<Photo>,
    val coverId: PhotoId?,
)

sealed interface CalendarIntent {
    data object PreviousMonth : CalendarIntent
    data object NextMonth : CalendarIntent

    /** 옆으로 넘겨서 고른 달. 몇 칸을 건너뛰었는지 모르므로 달을 그대로 받습니다. */
    data class MonthSelected(val month: YearMonth) : CalendarIntent
    data class DayTapped(val date: LocalDate) : CalendarIntent
    data object CollapseToggled : CalendarIntent
    data class PhotoLongPressed(val date: LocalDate, val id: PhotoId) : CalendarIntent
    data object AddTapped : CalendarIntent
}

sealed interface CalendarEffect {
    data object OpenUpload : CalendarEffect
    data class ShowMessage(val text: String) : CalendarEffect
}
