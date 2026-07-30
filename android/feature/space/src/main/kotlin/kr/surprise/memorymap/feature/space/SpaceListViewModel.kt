package kr.surprise.memorymap.feature.space

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.ui.MviViewModel
import kr.surprise.memorymap.domain.usecase.CreateSpaceUseCase
import kr.surprise.memorymap.domain.usecase.JoinSpaceUseCase
import kr.surprise.memorymap.domain.usecase.ObserveSpacesUseCase
import kr.surprise.memorymap.domain.usecase.RefreshSpacesUseCase

class SpaceListViewModel(
    observeSpaces: ObserveSpacesUseCase,
    private val refreshSpaces: RefreshSpacesUseCase,
    private val createSpace: CreateSpaceUseCase,
    private val joinSpace: JoinSpaceUseCase,
) : MviViewModel<SpaceListIntent, SpaceListState, SpaceListEffect>(SpaceListState()) {

    init {
        observeSpaces()
            .onEach { items -> setState { SpaceListReducer.spacesLoaded(this, items) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SpaceListIntent) {
        when (intent) {
            SpaceListIntent.Appeared, SpaceListIntent.PullToRefresh -> refresh()
            is SpaceListIntent.SpaceTapped -> sendEffect(SpaceListEffect.OpenSpace(intent.id))
            SpaceListIntent.CreateTapped -> setState { SpaceListReducer.sheetOpened(this, SpaceListSheet.Create) }
            SpaceListIntent.JoinTapped -> setState { SpaceListReducer.sheetOpened(this, SpaceListSheet.Join) }
            SpaceListIntent.SheetDismissed -> setState { SpaceListReducer.sheetDismissed(this) }
            is SpaceListIntent.NameTyped -> setState { SpaceListReducer.nameTyped(this, intent.value) }
            is SpaceListIntent.CodeTyped -> setState { SpaceListReducer.codeTyped(this, intent.value) }
            SpaceListIntent.CreateConfirmed -> create()
            SpaceListIntent.JoinConfirmed -> join()
            is SpaceListIntent.InviteCopied -> sendEffect(SpaceListEffect.ShareInvite(intent.code))
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            when (val result = refreshSpaces()) {
                is Outcome.Fail -> setState { SpaceListReducer.loadFailed(this, result.reason) }
                is Outcome.Ok -> Unit   // 목록은 흐름으로 들어옵니다
            }
        }
    }

    private fun create() {
        if (!currentState().canCreate()) return
        val name = currentState().pendingName
        setState { SpaceListReducer.working(this) }

        viewModelScope.launch {
            when (val result = createSpace(name)) {
                is Outcome.Ok -> {
                    val (space, invite) = result.value
                    setState { SpaceListReducer.created(this, space, invite.code) }
                }
                is Outcome.Fail -> {
                    setState { SpaceListReducer.failedAction(this) }
                    sendEffect(SpaceListEffect.ShowMessage("공간을 만들지 못했어요. 잠시 뒤 다시 해 주세요."))
                }
            }
        }
    }

    private fun join() {
        if (!currentState().canJoin()) return
        val code = currentState().pendingCode
        setState { SpaceListReducer.working(this) }

        viewModelScope.launch {
            when (val result = joinSpace(code)) {
                is Outcome.Ok -> setState { SpaceListReducer.joined(this, result.value) }
                is Outcome.Fail -> {
                    setState { SpaceListReducer.failedAction(this) }
                    sendEffect(SpaceListEffect.ShowMessage("그런 초대 코드를 못 찾았어요."))
                }
            }
        }
    }
}
