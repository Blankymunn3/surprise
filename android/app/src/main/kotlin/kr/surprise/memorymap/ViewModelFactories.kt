package kr.surprise.memorymap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.feature.calendar.CalendarViewModel
import kr.surprise.memorymap.feature.map.MapViewModel
import kr.surprise.memorymap.feature.space.SpaceListViewModel
import kr.surprise.memorymap.feature.upload.UploadViewModel

/**
 * ViewModel 조립. Hilt 를 안 쓰기로 했으니 팩토리를 손으로 씁니다 —
 * 화면이 넷이라 이 파일 하나면 충분합니다.
 */
@Suppress("UNCHECKED_CAST")
class VmFactory(private val create: () -> ViewModel) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}

fun AppContainer.spaceListFactory() = VmFactory {
    SpaceListViewModel(observeSpaces, refreshSpaces, createSpace, joinSpace)
}

fun AppContainer.mapFactory(spaceId: SpaceId) = VmFactory {
    MapViewModel(spaceId, observeBoard, searchRegions, setCover, regions)
}

fun AppContainer.calendarFactory(spaceId: SpaceId) = VmFactory {
    CalendarViewModel(
        spaceId = spaceId,
        observeBoard = observeBoard,
        setCover = setCover,
        regionNames = { regions.all().associateBy { it.code.value } },
    )
}

fun AppContainer.uploadFactory(spaceId: SpaceId) = VmFactory {
    UploadViewModel(
        spaceId = spaceId,
        readHints = { uri -> exif.read(android.net.Uri.parse(uri)) },
        toJpeg = { uri -> downscaler.toJpeg(android.net.Uri.parse(uri)) },
        // 올리기가 실패해도 사진을 잃지 않는다는 약속. 지금은 사용자가 고른 원본이
        // 갤러리에 그대로 있으므로 따로 복사하지 않고, 다시 시도하도록 알리기만 합니다.
        keepLocally = { },
        uploadPhotos = uploadPhotos,
        searchRegions = searchRegions,
        regions = regions,
    )
}
