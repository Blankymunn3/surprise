package kr.jjaguk

import android.content.Context
import kr.jjaguk.core.common.Outcome
import kr.jjaguk.core.model.SpaceId
import kr.jjaguk.core.model.SpaceKind
import kr.jjaguk.core.network.FirebaseAuth
import kr.jjaguk.core.network.FirebaseStorage
import kr.jjaguk.core.network.Firestore
import kr.jjaguk.data.auth.FirebaseAuthRepository
import kr.jjaguk.data.photo.ExifReader
import kr.jjaguk.data.photo.FirebasePhotoRepository
import kr.jjaguk.data.photo.ImageDownscaler
import kr.jjaguk.data.photo.LocalPhotoRepository
import kr.jjaguk.data.region.AssetRegionCatalog
import kr.jjaguk.data.space.SharedSpaceRepository
import kr.jjaguk.domain.repository.PhotoRepository
import kr.jjaguk.domain.usecase.CreateSpaceUseCase
import kr.jjaguk.domain.usecase.JoinSpaceUseCase
import kr.jjaguk.domain.usecase.NewInviteUseCase
import kr.jjaguk.domain.usecase.RenameSpaceUseCase
import kr.jjaguk.domain.usecase.ObservePhotoBoardUseCase
import kr.jjaguk.domain.usecase.ObserveSpacesUseCase
import kr.jjaguk.domain.usecase.RefreshPhotosUseCase
import kr.jjaguk.domain.usecase.RefreshSpacesUseCase
import kr.jjaguk.domain.usecase.SearchRegionsUseCase
import kr.jjaguk.domain.usecase.SetCoverPhotoUseCase
import kr.jjaguk.domain.usecase.UploadPhotosUseCase

/**
 * 손으로 조립하는 DI(Composition Root).
 *
 * **Hilt 를 쓰지 않기로 한 이유**: 화면이 여섯이고 의존성이 열 개 남짓입니다.
 * 이 규모에서 Hilt 는 빌드에 코드 생성 단계를 하나 더 얹는 값만 하고, 얻는 게 없습니다.
 * 그래프가 커지면 그때 바꿉니다 — 그때 고칠 자리가 이 파일 하나뿐이도록 만들어 뒀습니다.
 * (iOS 도 같은 이유로 수동 조립입니다 — `docs/app/ARCHITECTURE.md`)
 */
/**
 * 이 빌드가 붙는 서버 한 벌. **비밀이 아닙니다** — 실제 보안은 규칙이 합니다.
 * (`google-services.json` 의 값들. 파일을 읽지 않고 여기 적어 두는 이유는
 * 조립하는 곳 한 군데만 보면 이 앱이 어디에 붙는지 알 수 있게 하려는 것입니다.)
 *
 * dev 와 prod 를 **flavor 가 고릅니다** (`BuildConfig.FLAVOR`). 지금은 dev 전용
 * Firebase 프로젝트가 아직 없어 둘이 같은 값입니다 — dev 프로젝트를 만들면
 * [Dev] 의 네 값만 그 프로젝트 것으로 바꾸면 됩니다. 코드는 안 바뀝니다.
 *
 * dev 패키지(`kr.jjaguk.app.dev`)도 같은 프로젝트에 등록돼 있습니다(2026-08-08,
 * 디버그 SHA-1 포함) — 웹 클라이언트는 프로젝트 공용이라 dev 도 같은 값입니다.
 */
private class FirebaseEnv(
    val projectId: String,
    val bucket: String,
    val apiKey: String,
    /** ⚠️ **web 클라이언트(type 3)** 입니다. android 클라이언트(type 1)를 주면
     *  ID 토큰이 안 나옵니다 — 자주 틀리는 곳이라 `docs/app/AUTH.md` 에도 적어 뒀습니다. */
    val webClientId: String,
) {
    companion object {
        val Prod = FirebaseEnv(
            projectId = "our-surprise",
            bucket = "our-surprise.firebasestorage.app",
            apiKey = "AIzaSyBMg1m08msg7_xPISuEwx5gwImi5MVz-1Q",
            webClientId = "419812459548-ldmh2hi0vb5lmjase8jpctqei4sk4mbp.apps.googleusercontent.com",
        )

        /** 아직 prod 와 같다. dev 프로젝트가 생기면 여기만 바꾼다. */
        val Dev = Prod

        fun of(flavor: String): FirebaseEnv = if (flavor == "dev") Dev else Prod
    }
}

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** 뷰모델들이 받는 기록 클로저. Firebase 는 여기서만 안다. */
    val track: (String, Map<String, String>) -> Unit =
        FirebaseTracker(appContext)::track

    private val env = FirebaseEnv.of(BuildConfig.FLAVOR)

    /**
     * 웹과 **같은 버킷**입니다. 한쪽에서 넣은 사진이 다른 쪽에서 보여야 합니다.
     *
     * 토큰을 **함수로** 넘기는 이유: 저장소가 만들어지는 시점에는 아직 로그인 전입니다.
     * 요청할 때마다 물어봐야 그때의 토큰(필요하면 갱신된 것)이 실립니다.
     */
    private val storage = FirebaseStorage(
        bucket = env.bucket,
        token = { accounts.idToken() },
    )

    private val auth = FirebaseAuth(apiKey = env.apiKey)

    val googleSignIn = GoogleSignIn(serverClientId = env.webClientId)

    val accounts = FirebaseAuthRepository(appContext, auth)

    /** 멤버 판정이 사는 곳. 규칙이 여기를 봅니다 (`firestore.rules`). */
    private val firestore = Firestore(projectId = env.projectId, token = { accounts.idToken() })

    val regions = AssetRegionCatalog(appContext)
    val spaces = SharedSpaceRepository(appContext, firestore, accounts)

    val exif = ExifReader(appContext, regions)
    val downscaler = ImageDownscaler(appContext)

    val observeSpaces = ObserveSpacesUseCase(spaces)
    val refreshSpaces = RefreshSpacesUseCase(spaces)
    val createSpace = CreateSpaceUseCase(spaces)
    val joinSpace = JoinSpaceUseCase(spaces)
    val newInvite = NewInviteUseCase(spaces)
    val renameSpace = RenameSpaceUseCase(spaces)

    val searchRegions = SearchRegionsUseCase(regions)

    /**
     * 사진 저장소가 **둘**입니다. 혼자 짜국은 기기 안, 같이 쓰는 짜국은 서버 —
     * 어느 쪽을 쓸지는 **여기서만** 정합니다. 화면과 도메인은 어느 쪽인지 모릅니다.
     *
     * 저장소는 종류마다 한 벌씩만 만들어 돌려씁니다. 받아 둔 사진을 자기 안에 들고 있어서
     * 화면마다 새로 만들면 매번 다시 받아오게 됩니다.
     */
    private val remote = PhotoUseCases(FirebasePhotoRepository(storage, firestore, accounts))
    private val local = PhotoUseCases(LocalPhotoRepository(appContext, uploaderUid = "me"))

    internal fun photoUseCases(kind: SpaceKind): PhotoUseCases =
        if (kind == SpaceKind.Personal) local else remote

    /** 사진을 올린 뒤 지도·달력이 새 사진을 보게 합니다. */
    suspend fun refreshPhotos(kind: SpaceKind, spaceId: SpaceId): Outcome<Unit> =
        photoUseCases(kind).refreshPhotos(spaceId)
}

/** 한 저장소에 딸린 사진 유스케이스 한 벌. 종류마다 하나씩 있습니다. */
internal class PhotoUseCases(repository: PhotoRepository) {
    val observeBoard = ObservePhotoBoardUseCase(repository)
    val refreshPhotos = RefreshPhotosUseCase(repository)
    val uploadPhotos = UploadPhotosUseCase(repository)
    val setCover = SetCoverPhotoUseCase(repository)
}
