package kr.jjaguk.feature.calendar

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kr.jjaguk.core.common.Outcome
import kr.jjaguk.core.model.CoverKey
import kr.jjaguk.core.model.Region
import kr.jjaguk.core.model.SpaceId
import kr.jjaguk.core.ui.MviViewModel
import kr.jjaguk.domain.model.PhotoBoard
import kr.jjaguk.domain.usecase.ObservePhotoBoardUseCase
import kr.jjaguk.domain.usecase.SetCoverPhotoUseCase
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    spaceId: SpaceId,
    observeBoard: ObservePhotoBoardUseCase,
    private val setCover: SetCoverPhotoUseCase,
    private val regionNames: suspend () -> Map<String, Region>,
    private val track: (String, Map<String, String>) -> Unit = { _, _ -> },
    clock: Clock = Clock.systemDefaultZone(),
) : MviViewModel<CalendarIntent, CalendarState, CalendarEffect>(
    CalendarState(
        spaceId = spaceId,
        month = YearMonth.now(clock),
        today = LocalDate.now(clock),
    )
) {
    private var board: PhotoBoard = PhotoBoard.Empty
    private var names: Map<String, Region> = emptyMap()

    init {
        viewModelScope.launch { names = regionNames(); rebuild() }
        observeBoard(spaceId)
            .onEach { board = it; rebuild() }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: CalendarIntent) {
        when (intent) {
            CalendarIntent.PreviousMonth -> {
                setState { CalendarReducer.monthChanged(this, -1) }; rebuild()
            }
            CalendarIntent.NextMonth -> {
                setState { CalendarReducer.monthChanged(this, 1) }; rebuild()
            }
            is CalendarIntent.MonthSelected -> {
                setState { CalendarReducer.monthSelected(this, intent.month) }; rebuild()
            }
            is CalendarIntent.DayTapped -> setState { CalendarReducer.daySelected(this, intent.date) }
            CalendarIntent.CollapseToggled -> setState { CalendarReducer.collapseToggled(this) }
            CalendarIntent.AddTapped -> sendEffect(CalendarEffect.OpenUpload)
            is CalendarIntent.PhotoLongPressed -> viewModelScope.launch {
                val result = setCover(currentState().spaceId, CoverKey.ForDay(intent.date), intent.id)
                when (result) {
                    is Outcome.Ok -> track("cover_set_day", emptyMap())
                    is Outcome.Fail -> {
                        track("cover_set_day_failed", emptyMap())
                        sendEffect(CalendarEffect.ShowMessage(CalendarMessage.CoverFailed))
                    }
                }
            }
        }
    }

    private fun rebuild() {
        setState { CalendarReducer.rebuild(this, board, names) }
    }
}
