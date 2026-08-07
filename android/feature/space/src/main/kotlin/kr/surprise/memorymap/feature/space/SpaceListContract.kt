package kr.surprise.memorymap.feature.space

import android.content.Context
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
    /** 로그인했는지. 같이 쓰는 짜국을 만들거나 참여할 때만 봅니다. */
    val signedIn: Boolean = false,
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
    /**
     * 만들자마자 뜨는 초대 코드 — 다시 찾게 하지 않으려고.
     * id 와 종류를 같이 드는 이유: 이 화면의 '짜국 열기' 가 바로 들어가야 해서입니다.
     */
    data class Invited(
        val spaceId: SpaceId,
        val kind: SpaceKind,
        val spaceName: String,
        val code: String,
    ) : SpaceListSheet

    /**
     * 로그인이 필요해 **잠깐 끼어든** 화면. 앱을 켤 때가 아니라 여기서만 뜹니다
     * (`docs/app/design.html` 의 '로그인').
     *
     * [next] 를 들고 있는 이유: 로그인이 끝나면 **하던 일을 이어서** 해야 합니다.
     * 로그인만 하고 멈추면 사용자가 방금 뭘 하려던 건지 다시 찾아야 합니다.
     */
    data class SignIn(val next: Next) : SpaceListSheet

    /** 로그인 뒤에 이어서 할 일 */
    enum class Next { Create, Join }
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
    data class InviteShared(val code: String) : SpaceListIntent
    /** 초대 코드 화면의 '짜국 열기' — 방금 만든 짜국으로 바로 들어갑니다. */
    data object InviteOpenTapped : SpaceListIntent

    /** '구글로 계속하기' */
    data object SignInTapped : SpaceListIntent
    /** 구글 창에서 받아 온 ID 토큰. 사용자가 창을 닫았으면 이 인텐트가 오지 않습니다. */
    data class GoogleTokenReceived(val idToken: String) : SpaceListIntent
    /** '그냥 혼자 쓸래요' — 만들기에서 왔을 때만 있습니다. */
    data object SignInGaveUp : SpaceListIntent
}

/** 한 번만 일어나는 일. 화면에 남아 있어야 하는 건 State 로 갑니다. */
/**
 * 화면에 알릴 일. **문구가 아니라 "무슨 일이 있었는지"** 를 나릅니다 —
 * 말은 화면이 고릅니다. 뷰모델은 Composable 이 아니라 `stringResource` 를 못 씁니다.
 */
sealed interface SpaceListMessage {
    /** 구글 로그인이 안 됨 */
    data object SignInFailed : SpaceListMessage
    /** 그런 초대 코드가 없음 */
    data object InviteNotFound : SpaceListMessage
    /** 그 밖의 실패. 원인을 그대로 나르고 말은 화면이 고릅니다. */
    data class Failed(val reason: Failure) : SpaceListMessage
}

sealed interface SpaceListEffect {
    /** 종류를 같이 넘깁니다 — 들어간 화면이 기기 안 사진을 볼지 서버 사진을 볼지 정합니다. */
    data class OpenSpace(val id: SpaceId, val kind: SpaceKind, val name: String) : SpaceListEffect
    data class ShowMessage(val message: SpaceListMessage) : SpaceListEffect
    /** 시스템 공유 창으로 코드를 보냅니다. */
    data class ShareInvite(val code: String) : SpaceListEffect
    /** 클립보드에 코드를 넣습니다. 공유와 다른 일이라 따로 둡니다. */
    data class CopyInvite(val code: String) : SpaceListEffect

    /**
     * 구글 계정 고르기 창을 띄워 달라는 부탁. **화면이 아니라 앱 껍데기가** 합니다 —
     * `Activity` 가 필요해서요. 받아 온 토큰은 [SpaceListIntent.GoogleTokenReceived] 로 돌아옵니다.
     */
    data object StartGoogleSignIn : SpaceListEffect
}

/**
 * [SpaceListMessage] 를 사람이 읽을 말로. **화면 쪽에서 고릅니다.**
 *
 * 실패 원인마다 다르게 적는 이유: "다시 해 주세요" 만 띄우면 몇 번을 다시 해도
 * 안 되는 경우(권한)를 알 길이 없습니다.
 */
fun SpaceListMessage.say(context: Context): String = context.getString(
    when (this) {
        SpaceListMessage.SignInFailed -> R.string.msg_sign_in_failed
        SpaceListMessage.InviteNotFound -> R.string.msg_invite_not_found
        is SpaceListMessage.Failed -> when (reason) {
            Failure.Denied -> R.string.msg_denied
            Failure.Network -> R.string.msg_network
            Failure.Timeout -> R.string.msg_timeout
            Failure.TooLarge -> R.string.msg_too_large
            Failure.NotFound -> R.string.msg_not_found
            Failure.Unknown -> R.string.msg_unknown
        }
    }
)
