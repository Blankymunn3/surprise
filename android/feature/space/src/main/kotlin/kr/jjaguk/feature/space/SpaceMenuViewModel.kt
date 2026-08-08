package kr.jjaguk.feature.space

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kr.jjaguk.core.common.Outcome
import kr.jjaguk.core.model.Space
import kr.jjaguk.core.model.SpaceId
import kr.jjaguk.core.model.SpaceKind
import kr.jjaguk.core.ui.MviViewModel
import kr.jjaguk.domain.usecase.NewInviteUseCase
import kr.jjaguk.domain.usecase.ObserveSpacesUseCase
import kr.jjaguk.domain.usecase.RenameSpaceUseCase

/** ⋯ 메뉴가 보여 주는 것 — 멤버 · 초대 코드 · 이름. */
data class SpaceMenuState(
    val spaceId: SpaceId,
    val space: Space? = null,
    /** 초대 코드. 같이 쓰는 짜국에서만, 메뉴를 처음 열 때 한 번 만듭니다. */
    val code: String? = null,
    val renaming: Boolean = false,
    val pendingName: String = "",
    val working: Boolean = false,
)

sealed interface SpaceMenuIntent {
    data object Appeared : SpaceMenuIntent
    data object Dismissed : SpaceMenuIntent
    data class CodeCopied(val code: String) : SpaceMenuIntent
    data object RenameTapped : SpaceMenuIntent
    data class NameTyped(val value: String) : SpaceMenuIntent
    data object RenameConfirmed : SpaceMenuIntent
}

sealed interface SpaceMenuEffect {
    data object Close : SpaceMenuEffect
    data class CopyCode(val code: String) : SpaceMenuEffect
    data object RenameFailed : SpaceMenuEffect
}

/**
 * ⋯ 메뉴.
 *
 * 초대 코드를 **메뉴를 열 때 만듭니다.** 저장소가 코드→짜국만 들고 있어서 이미 쓰던
 * 코드를 되찾을 길이 없기 때문입니다 (`invites/{code}` 한 방향). 코드는 여러 개여도
 * 모두 같은 짜국을 가리키므로 문제는 없고, 한 번 만들면 이 화면이 사는 동안 그대로 씁니다.
 */
class SpaceMenuViewModel(
    spaceId: SpaceId,
    private val observeSpaces: ObserveSpacesUseCase,
    private val newInvite: NewInviteUseCase,
    private val renameSpace: RenameSpaceUseCase,
) : MviViewModel<SpaceMenuIntent, SpaceMenuState, SpaceMenuEffect>(SpaceMenuState(spaceId)) {

    init {
        observeSpaces()
            .onEach { list ->
                val found = list.firstOrNull { it.id == currentState().spaceId }
                setState { copy(space = found) }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SpaceMenuIntent) {
        when (intent) {
            SpaceMenuIntent.Appeared -> ensureCode()
            SpaceMenuIntent.Dismissed -> sendEffect(SpaceMenuEffect.Close)
            is SpaceMenuIntent.CodeCopied -> sendEffect(SpaceMenuEffect.CopyCode(intent.code))
            SpaceMenuIntent.RenameTapped -> setState {
                copy(renaming = true, pendingName = space?.name.orEmpty())
            }
            is SpaceMenuIntent.NameTyped -> setState { copy(pendingName = intent.value) }
            SpaceMenuIntent.RenameConfirmed -> rename()
        }
    }

    private fun ensureCode() {
        val state = currentState()
        // 혼자 쓰는 짜국에는 초대할 사람이 없습니다.
        if (state.code != null || state.space?.kind != SpaceKind.Shared) return

        viewModelScope.launch {
            when (val made = newInvite(state.spaceId)) {
                is Outcome.Ok -> setState { copy(code = made.value.code) }
                is Outcome.Fail -> Unit // 코드 칸만 비워 둡니다 — 메뉴의 나머지는 쓸 수 있습니다
            }
        }
    }

    private fun rename() {
        val state = currentState()
        val name = state.pendingName.trim()
        if (name.isEmpty() || state.working) return

        setState { copy(working = true) }
        viewModelScope.launch {
            when (renameSpace(state.spaceId, name)) {
                is Outcome.Ok -> setState { copy(renaming = false, working = false) }
                is Outcome.Fail -> {
                    setState { copy(working = false) }
                    sendEffect(SpaceMenuEffect.RenameFailed)
                }
            }
        }
    }
}
