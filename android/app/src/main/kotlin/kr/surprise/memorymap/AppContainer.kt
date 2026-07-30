package kr.surprise.memorymap

import android.content.Context
import kr.surprise.memorymap.core.network.FirebaseStorage
import kr.surprise.memorymap.data.photo.ExifReader
import kr.surprise.memorymap.data.photo.FirebasePhotoRepository
import kr.surprise.memorymap.data.photo.ImageDownscaler
import kr.surprise.memorymap.data.region.AssetRegionCatalog
import kr.surprise.memorymap.data.space.SharedSpaceRepository
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

    private val photos = FirebasePhotoRepository(storage, uploaderUid = "me")

    val exif = ExifReader(appContext, regions)
    val downscaler = ImageDownscaler(appContext)

    val observeSpaces = ObserveSpacesUseCase(spaces)
    val refreshSpaces = RefreshSpacesUseCase(spaces)
    val createSpace = CreateSpaceUseCase(spaces)
    val joinSpace = JoinSpaceUseCase(spaces)

    val observeBoard = ObservePhotoBoardUseCase(photos)
    val refreshPhotos = RefreshPhotosUseCase(photos)
    val uploadPhotos = UploadPhotosUseCase(photos)
    val setCover = SetCoverPhotoUseCase(photos)

    val searchRegions = SearchRegionsUseCase(regions)
}
