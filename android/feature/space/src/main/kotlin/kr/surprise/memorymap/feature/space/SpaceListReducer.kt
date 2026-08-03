package kr.surprise.memorymap.feature.space

import kr.surprise.memorymap.core.common.Failure
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.model.SpaceKind

/**
 * 순수 함수입니다. 네트워크·시간·난수를 쓰지 않습니다 — 필요하면 인자로 받습니다.
 * 앱의 핵심이 상태 전이라 여기를 제일 먼저 테스트합니다 (`docs/app/MVI.md`).
 */
internal object SpaceListReducer {

    fun spacesLoaded(state: SpaceListState, items: List<Space>): SpaceListState =
        state.copy(spaces = SpacesUi.Ready(items), working = false)

    fun loadFailed(state: SpaceListState, reason: Failure): SpaceListState =
        state.copy(
            // 이미 목록을 보여 주고 있었다면 지우지 않습니다. 새로고침이 실패했다고
            // 눈앞의 목록이 사라지면 더 나쁩니다.
            spaces = if (state.spaces is SpacesUi.Ready) state.spaces else SpacesUi.Failed(reason),
            working = false,
        )

    fun accountChanged(state: SpaceListState, signedIn: Boolean): SpaceListState =
        state.copy(signedIn = signedIn)

    /** 시트를 열 때마다 종류도 기본(혼자)으로 되돌립니다 — 지난번에 고른 것이 남아 있으면 안 됩니다. */
    fun sheetOpened(state: SpaceListState, sheet: SpaceListSheet): SpaceListState =
        state.copy(sheet = sheet, pendingName = "", pendingCode = "", pendingKind = SpaceKind.Personal)

    /**
     * 로그인을 마치고 **원래 시트로 돌아올 때**. [sheetOpened] 와 달리 입력해 둔 것을
     * 지우지 않습니다 — 지우면 방금 적은 이름이 사라집니다.
     */
    fun sheetOpenedKeepingInput(state: SpaceListState, sheet: SpaceListSheet): SpaceListState =
        state.copy(sheet = sheet)

    fun sheetDismissed(state: SpaceListState): SpaceListState =
        state.copy(
            sheet = SpaceListSheet.None,
            pendingName = "",
            pendingCode = "",
            pendingKind = SpaceKind.Personal,
            working = false,
        )

    fun nameTyped(state: SpaceListState, value: String): SpaceListState =
        state.copy(pendingName = value)

    fun kindSelected(state: SpaceListState, kind: SpaceKind): SpaceListState =
        state.copy(pendingKind = kind)

    /**
     * 로그인 창이 끼어듭니다. **입력해 둔 것을 지우지 않습니다** — 로그인이 끝나면
     * 그대로 이어서 만들거나 참여해야 합니다.
     */
    fun signInNeeded(state: SpaceListState, next: SpaceListSheet.Next): SpaceListState =
        state.copy(sheet = SpaceListSheet.SignIn(next), working = false)

    /**
     * '그냥 혼자 쓸래요'. 만들기 시트로 **되돌아가되 혼자로 바꿔 둡니다** —
     * 곧장 만들어 버리면 방금 무엇이 만들어졌는지 모른 채 목록이 하나 늘어납니다.
     * 이름은 그대로 두어 다시 입력하지 않게 합니다.
     */
    fun signInGaveUp(state: SpaceListState): SpaceListState =
        state.copy(
            sheet = SpaceListSheet.Create,
            pendingKind = SpaceKind.Personal,
            working = false,
        )

    fun codeTyped(state: SpaceListState, value: String): SpaceListState =
        state.copy(pendingCode = value)

    fun working(state: SpaceListState): SpaceListState = state.copy(working = true)

    /**
     * 저장소도 목록에 넣고 여기서도 넣기 때문에 **같은 공간이 두 번 들어갈 수 있습니다.**
     * 목록은 `id` 를 키로 그리므로 그러면 화면이 죽습니다. `joined` 와 같은 방식으로
     * 같은 id 를 먼저 걷어냅니다.
     */
    fun created(state: SpaceListState, space: Space, code: String?): SpaceListState =
        state.copy(
            spaces = SpacesUi.Ready(state.currentItems().filterNot { it.id == space.id } + space),
            // 혼자 쓰는 짜국은 초대 코드가 없습니다. 보여 줄 것이 없으니 시트를 닫습니다.
            sheet = if (code == null) SpaceListSheet.None else SpaceListSheet.Invited(space.name, code),
            pendingName = "",
            working = false,
        )

    fun joined(state: SpaceListState, space: Space): SpaceListState =
        state.copy(
            spaces = SpacesUi.Ready(state.currentItems().filterNot { it.id == space.id } + space),
            sheet = SpaceListSheet.None,
            pendingCode = "",
            working = false,
        )

    fun failedAction(state: SpaceListState): SpaceListState = state.copy(working = false)

    private fun SpaceListState.currentItems(): List<Space> =
        (spaces as? SpacesUi.Ready)?.items.orEmpty()
}

/**
 * 목록에서 짜국의 종류를 찾습니다.
 *
 * 못 찾으면 **같이 쓰는 쪽**으로 봅니다 — 목록에 없는 것을 눌렀을 리는 없지만, 만에 하나 그렇다면
 * 지금까지의 짜국은 전부 서버 쪽이었으니 그쪽이 맞습니다.
 */
internal fun SpaceListState.kindOf(id: SpaceId): SpaceKind =
    (spaces as? SpacesUi.Ready)?.items?.firstOrNull { it.id == id }?.kind ?: SpaceKind.Shared

/** 공간을 만들 수 있는가. 화면의 버튼이 꺼지는 조건과 같아야 합니다. */
internal fun SpaceListState.canCreate(): Boolean = pendingName.isNotBlank() && !working

/** 참여할 수 있는가 — 코드는 여섯 글자입니다. */
internal fun SpaceListState.canJoin(): Boolean =
    pendingCode.trim().length >= 6 && !working
