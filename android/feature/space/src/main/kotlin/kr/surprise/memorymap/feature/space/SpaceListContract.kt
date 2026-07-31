package kr.surprise.memorymap.feature.space

import kr.surprise.memorymap.core.common.Failure
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.model.SpaceKind

/** 화면을 그리는 데 필요한 전부. 불변입니다. */
data class SpaceListState(
    val spaces: SpacesUi = SpacesUi.Loading,
    val sheet: SpaceListSheet = SpaceListSheet.None,
    val pendingName: String = "",
    val pendingCode: String = "",
    /**
     * 만들기 시트에서 고른 종류. 기본이 **혼자**인 이유는 잘못 골라도 사진이 폰 밖으로
     * 나가지 않기 때문입니다. 반대로 두면 무심코 넘긴 사람의 사진이 서버로 갑니다.
     */
    val pendingKind: SpaceKind = SpaceKind.Personal,
    val working: Boolean = false,
)

/**
 * 로딩과 에러를 따로 두지 않는 이유: `isLoading = true` 이면서 `error != null` 같은
 * 모순된 상태를 애초에 만들 수 없게 하려는 것입니다 (`docs/app/MVI.md`).
 */
sealed interface SpacesUi {
    data object Loading : SpacesUi
    data class Ready(val items: List<Space>) : SpacesUi
    data class Failed(val reason: Failure) : SpacesUi
}

sealed interface SpaceListSheet {
    data object None : SpaceListSheet
    data object Create : SpaceListSheet
    data object Join : SpaceListSheet
    /** 만들자마자 뜨는 초대 코드 — 다시 찾게 하지 않으려고 */
    data class Invited(val spaceName: String, val code: String) : SpaceListSheet
}

/** 사용자가 **한 일**입니다. 무엇을 하라는 명령이 아닙니다. */
sealed interface SpaceListIntent {
    data object Appeared : SpaceListIntent
    data object PullToRefresh : SpaceListIntent
    data class SpaceTapped(val id: SpaceId) : SpaceListIntent
    data object CreateTapped : SpaceListIntent
    data object JoinTapped : SpaceListIntent
    data object SheetDismissed : SpaceListIntent
    data class NameTyped(val value: String) : SpaceListIntent
    data class KindSelected(val kind: SpaceKind) : SpaceListIntent
    data class CodeTyped(val value: String) : SpaceListIntent
    data object CreateConfirmed : SpaceListIntent
    data object JoinConfirmed : SpaceListIntent
    data class InviteCopied(val code: String) : SpaceListIntent
}

/** 한 번만 일어나는 일. 화면에 남아 있어야 하는 건 State 로 갑니다. */
sealed interface SpaceListEffect {
    /** 종류를 같이 넘깁니다 — 들어간 화면이 기기 안 사진을 볼지 서버 사진을 볼지 정합니다. */
    data class OpenSpace(val id: SpaceId, val kind: SpaceKind) : SpaceListEffect
    data class ShowMessage(val text: String) : SpaceListEffect
    data class ShareInvite(val code: String) : SpaceListEffect
}
