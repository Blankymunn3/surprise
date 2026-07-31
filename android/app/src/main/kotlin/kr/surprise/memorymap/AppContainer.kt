package kr.surprise.memorymap

import android.content.Context
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.model.SpaceKind
import kr.surprise.memorymap.core.network.FirebaseStorage
import kr.surprise.memorymap.data.photo.ExifReader
import kr.surprise.memorymap.data.photo.FirebasePhotoRepository
import kr.surprise.memorymap.data.photo.ImageDownscaler
import kr.surprise.memorymap.data.photo.LocalPhotoRepository
import kr.surprise.memorymap.data.region.AssetRegionCatalog
import kr.surprise.memorymap.data.space.SharedSpaceRepository
import kr.surprise.memorymap.domain.repository.PhotoRepository
import kr.surprise.memorymap.domain.usecase.CreateSpaceUseCase
import kr.surprise.memorymap.domain.usecase.JoinSpaceUseCase
import kr.surprise.memorymap.domain.usecase.ObservePhotoBoardUseCase
import kr.surprise.memorymap.domain.usecase.ObserveSpacesUseCase
import kr.surprise.memorymap.domain.usecase.RefreshPhotosUseCase
import kr.surprise.memorymap.domain.usecase.RefreshSpacesUseCase
import kr.surprise.memorymap.domain.usecase.SearchRegionsUseCase
import kr.surprise.memorymap.domain.usecase.SetCoverPhotoUseCase
import kr.surprise.memorymap.domain.usecase.UploadPhotosUseCase

/**
 * 손으로 조립하는 DI(Composition Root).
 *
 * **Hilt 를 쓰지 않기로 한 이유**: 화면이 여섯이고 의존성이 열 개 남짓입니다.
 * 이 규모에서 Hilt 는 빌드에 코드 생성 단계를 하나 더 얹는 값만 하고, 얻는 게 없습니다.
 * 그래프가 커지면 그때 바꿉니다 — 그때 고칠 자리가 이 파일 하나뿐이도록 만들어 뒀습니다.
 * (iOS 도 같은 이유로 수동 조립입니다 — `docs/app/ARCHITECTURE.md`)
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** 웹과 **같은 버킷**입니다. 한쪽에서 넣은 사진이 다른 쪽에서 보여야 합니다. */
    private val storage = FirebaseStorage(bucket = "our-surprise.firebasestorage.app")

    val regions = AssetRegionCatalog(appContext)
    val spaces = SharedSpaceRepository(appContext, storage)

    val exif = ExifReader(appContext, regions)
    val downscaler = ImageDownscaler(appContext)

    val observeSpaces = ObserveSpacesUseCase(spaces)
    val refreshSpaces = RefreshSpacesUseCase(spaces)
    val createSpace = CreateSpaceUseCase(spaces)
    val joinSpace = JoinSpaceUseCase(spaces)

    val searchRegions = SearchRegionsUseCase(regions)

    /**
     * 사진 저장소가 **둘**입니다. 혼자 짜국은 기기 안, 같이 쓰는 짜국은 서버 —
     * 어느 쪽을 쓸지는 **여기서만** 정합니다. 화면과 도메인은 어느 쪽인지 모릅니다.
     *
     * 저장소는 종류마다 한 벌씩만 만들어 돌려씁니다. 받아 둔 사진을 자기 안에 들고 있어서
     * 화면마다 새로 만들면 매번 다시 받아오게 됩니다.
     */
    private val remote = PhotoUseCases(FirebasePhotoRepository(storage, uploaderUid = "me"))
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
