package kr.surprise.memorymap

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kr.surprise.memorymap.core.common.Failure
import kr.surprise.memorymap.core.common.Outcome

/**
 * 구글 계정을 골라 **ID 토큰만** 받아 옵니다. 그 토큰을 Firebase 토큰으로 바꾸는 일은
 * `FirebaseAuthRepository` 가 합니다 (`docs/app/AUTH.md`).
 *
 * **앱 모듈에 있는 이유**: Credential Manager 는 계정 고르기 창을 띄워야 해서 `Activity` 가
 * 필요합니다. 데이터 계층이 화면을 알면 안 되므로, 창을 띄우는 여기까지만 앱이 맡고
 * 아래로는 문자열 하나만 내려보냅니다.
 */
class GoogleSignIn(private val serverClientId: String) {

    /**
     * 사용자가 창을 닫으면 [Failure.NotFound] 가 아니라 **[Cancelled]** 로 알립니다 —
     * 스스로 그만둔 것을 "실패했어요" 라고 띄우면 안 되기 때문입니다.
     */
    sealed interface Result {
        data class Token(val value: String) : Result
        data object Cancelled : Result
        data class Failed(val reason: Failure) : Result
    }

    suspend fun idToken(activity: Activity): Result {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            // false 라야 **이 앱에 처음 로그인하는 계정도** 목록에 뜹니다.
            // true 면 이미 쓴 적 있는 계정만 나와서 첫 로그인이 막힙니다.
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            Result.Token(credential.idToken)
        } catch (e: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (e: NoCredentialException) {
            // 기기에 구글 계정이 하나도 없는 경우
            Result.Failed(Failure.NotFound)
        } catch (e: GetCredentialException) {
            Result.Failed(Failure.Unknown)
        }
    }
}

/** [Outcome] 으로 받고 싶을 때. 취소는 실패가 아니라 `null` 입니다. */
suspend fun GoogleSignIn.idTokenOrNull(activity: Activity): Outcome<String>? =
    when (val result = idToken(activity)) {
        is GoogleSignIn.Result.Token -> Outcome.Ok(result.value)
        is GoogleSignIn.Result.Failed -> Outcome.Fail(result.reason)
        GoogleSignIn.Result.Cancelled -> null
    }
