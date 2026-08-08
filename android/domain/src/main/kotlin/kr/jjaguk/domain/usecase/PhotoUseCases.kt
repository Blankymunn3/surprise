package kr.jjaguk.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kr.jjaguk.core.common.Outcome
import kr.jjaguk.core.model.CoverKey
import kr.jjaguk.core.model.Photo
import kr.jjaguk.core.model.PhotoId
import kr.jjaguk.core.model.SpaceId
import kr.jjaguk.domain.model.NewPhoto
import kr.jjaguk.domain.model.PhotoBoard
import kr.jjaguk.domain.repository.PhotoRepository

/**
 * 공간 하나의 사진을 지도용·달력용으로 갈라 흘려보냅니다.
 * 지도 탭과 달력 탭이 이걸 **같이** 구독하므로 탭 전환에 다시 받지 않습니다.
 */
class ObservePhotoBoardUseCase(private val photos: PhotoRepository) {
    operator fun invoke(spaceId: SpaceId): Flow<PhotoBoard> =
        combine(photos.observePhotos(spaceId), photos.observeCovers(spaceId)) { list, covers ->
            PhotoBoard.of(list, covers)
        }
}

class RefreshPhotosUseCase(private val photos: PhotoRepository) {
    suspend operator fun invoke(spaceId: SpaceId): Outcome<Unit> = photos.refresh(spaceId)
}

class UploadPhotosUseCase(private val photos: PhotoRepository) {
    suspend operator fun invoke(spaceId: SpaceId, drafts: List<NewPhoto>): Outcome<List<Photo>> {
        require(drafts.isNotEmpty()) { "올릴 사진이 없습니다" }
        return photos.upload(spaceId, drafts)
    }
}

class SetCoverPhotoUseCase(private val photos: PhotoRepository) {
    suspend operator fun invoke(spaceId: SpaceId, key: CoverKey, id: PhotoId): Outcome<Unit> =
        photos.setCover(spaceId, key, id)
}

class DeletePhotoUseCase(private val photos: PhotoRepository) {
    suspend operator fun invoke(spaceId: SpaceId, id: PhotoId): Outcome<Unit> =
        photos.delete(spaceId, id)
}
