package kr.surprise.memorymap.feature.calendar

import android.content.Context

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

/**
 * 화면에 알릴 일. **문구가 아니라 "무슨 일이 있었는지"** 를 나릅니다 —
 * 말은 화면이 고릅니다. [kr.surprise.memorymap.feature.map.MapMessage] 와 같은 규칙입니다.
 */
enum class CalendarMessage {
    /** 대표사진을 바꾸지 못함 */
    CoverFailed,
}

sealed interface CalendarEffect {
    data object OpenUpload : CalendarEffect
    data class ShowMessage(val message: CalendarMessage) : CalendarEffect
}

/** [CalendarMessage] 를 사람이 읽을 말로. 화면 쪽에서 고릅니다. */
fun CalendarMessage.say(context: Context): String = context.getString(
    when (this) {
        CalendarMessage.CoverFailed -> R.string.calendar_msg_cover_failed
    }
)
