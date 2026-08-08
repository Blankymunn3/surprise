package kr.jjaguk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kr.jjaguk.core.model.SpaceId
import kr.jjaguk.core.model.SpaceKind
import kr.jjaguk.feature.calendar.CalendarViewModel
import kr.jjaguk.feature.map.MapViewModel
import kr.jjaguk.feature.space.SpaceListViewModel
import kr.jjaguk.feature.space.SpaceMenuViewModel
import kr.jjaguk.feature.upload.UploadViewModel

/**
 * ViewModel 조립. Hilt 를 안 쓰기로 했으니 팩토리를 손으로 씁니다 —
 * 화면이 넷이라 이 파일 하나면 충분합니다.
 */
@Suppress("UNCHECKED_CAST")
class VmFactory(private val build: () -> ViewModel) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = build() as T
}

fun AppContainer.spaceListFactory() = VmFactory {
    SpaceListViewModel(observeSpaces, refreshSpaces, createSpace, joinSpace, accounts, track)
}

/** 짜국 종류를 아는 팩토리는 기록마다 `kind` 를 자동으로 얹는다. */
private fun AppContainer.trackWith(kind: SpaceKind): (String, Map<String, String>) -> Unit =
    { event, params -> track(event, params + ("kind" to kind.name)) }

/**
 * 짜국의 **종류**를 같이 받습니다 — 혼자면 기기 안 사진, 같이 쓰면 서버 사진을 씁니다.
 * 고르는 일은 [AppContainer] 가 하고 화면은 어느 쪽인지 모릅니다.
 */
fun AppContainer.spaceMenuFactory(spaceId: SpaceId) = VmFactory {
    SpaceMenuViewModel(spaceId, observeSpaces, newInvite, renameSpace)
}

fun AppContainer.mapFactory(spaceId: SpaceId, kind: SpaceKind) = VmFactory {
    val photos = photoUseCases(kind)
    MapViewModel(spaceId, photos.observeBoard, searchRegions, photos.setCover, regions, trackWith(kind))
}

fun AppContainer.calendarFactory(spaceId: SpaceId, kind: SpaceKind) = VmFactory {
    val photos = photoUseCases(kind)
    CalendarViewModel(
        spaceId = spaceId,
        observeBoard = photos.observeBoard,
        setCover = photos.setCover,
        regionNames = { regions.all().associateBy { it.code.value } },
        track = trackWith(kind),
    )
}

fun AppContainer.uploadFactory(spaceId: SpaceId, kind: SpaceKind) = VmFactory {
    UploadViewModel(
        spaceId = spaceId,
        readHints = { uri -> exif.read(android.net.Uri.parse(uri)) },
        toJpeg = { uri -> downscaler.toJpeg(android.net.Uri.parse(uri)) },
        // 올리기가 실패해도 사진을 잃지 않는다는 약속. 지금은 사용자가 고른 원본이
        // 갤러리에 그대로 있으므로 따로 복사하지 않고, 다시 시도하도록 알리기만 합니다.
        keepLocally = { },
        uploadPhotos = photoUseCases(kind).uploadPhotos,
        searchRegions = searchRegions,
        regions = regions,
        track = trackWith(kind),
    )
}
