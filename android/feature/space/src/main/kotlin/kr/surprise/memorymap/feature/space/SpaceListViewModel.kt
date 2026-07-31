package kr.surprise.memorymap.feature.space

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kr.surprise.memorymap.core.common.Failure
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.SpaceKind
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
            is SpaceListIntent.SpaceTapped ->
                sendEffect(SpaceListEffect.OpenSpace(intent.id, currentState().kindOf(intent.id)))
            SpaceListIntent.CreateTapped -> setState { SpaceListReducer.sheetOpened(this, SpaceListSheet.Create) }
            SpaceListIntent.JoinTapped -> setState { SpaceListReducer.sheetOpened(this, SpaceListSheet.Join) }
            SpaceListIntent.SheetDismissed -> setState { SpaceListReducer.sheetDismissed(this) }
            is SpaceListIntent.NameTyped -> setState { SpaceListReducer.nameTyped(this, intent.value) }
            is SpaceListIntent.KindSelected -> setState { SpaceListReducer.kindSelected(this, intent.kind) }
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
        val kind = currentState().pendingKind
        setState { SpaceListReducer.working(this) }

        viewModelScope.launch {
            when (val result = createSpace(name, kind)) {
                is Outcome.Ok -> {
                    val (space, invite) = result.value
                    setState { SpaceListReducer.created(this, space, invite?.code) }
                }
                is Outcome.Fail -> {
                    setState { SpaceListReducer.failedAction(this) }
                    sendEffect(SpaceListEffect.ShowMessage(reasonText(result.reason)))
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
                    sendEffect(SpaceListEffect.ShowMessage(
                        if (result.reason == Failure.NotFound) "그런 초대 코드를 못 찾았어요."
                        else reasonText(result.reason)
                    ))
                }
            }
        }
    }

    /**
     * 왜 실패했는지 사람 말로. **원인마다 다르게 적는 이유**: "다시 해 주세요" 만
     * 띄우면 몇 번을 다시 해도 안 되는 경우(권한)를 알 길이 없습니다.
     */
    private fun reasonText(reason: Failure): String = when (reason) {
        Failure.Denied ->
            "저장소에 쓸 권한이 없어요. Firebase 콘솔에서 storage.rules 를 게시했는지 확인해 주세요."
        Failure.Network -> "인터넷이 안 되는 것 같아요."
        Failure.Timeout -> "응답이 너무 느려요. 잠시 뒤 다시 해 주세요."
        Failure.TooLarge -> "파일이 너무 커요."
        Failure.NotFound -> "찾을 수 없어요."
        Failure.Unknown -> "알 수 없는 문제가 생겼어요."
    }
}
