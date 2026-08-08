package kr.jjaguk.feature.upload

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.jjaguk.core.common.Outcome
import kr.jjaguk.core.model.Region
import kr.jjaguk.core.model.SpaceId
import kr.jjaguk.core.ui.MviViewModel
import kr.jjaguk.domain.UploadPlan
import kr.jjaguk.domain.model.ExifHint
import kr.jjaguk.domain.model.NewPhoto
import kr.jjaguk.domain.repository.RegionCatalog
import kr.jjaguk.domain.usecase.SearchRegionsUseCase
import kr.jjaguk.domain.usecase.UploadPhotosUseCase
import java.time.Clock
import java.time.LocalDate

/** 사진에서 날짜·위치를 읽고, 줄이고, 올립니다. 하나라도 실패하면 기기에 남깁니다. */
class UploadViewModel(
    spaceId: SpaceId,
    private val readHints: suspend (String) -> ExifHint,
    private val toJpeg: suspend (String) -> ByteArray?,
    private val keepLocally: suspend (String) -> Unit,
    private val uploadPhotos: UploadPhotosUseCase,
    private val searchRegions: SearchRegionsUseCase,
    private val regions: RegionCatalog,
    private val clock: Clock = Clock.systemDefaultZone(),
) : MviViewModel<UploadIntent, UploadState, UploadEffect>(UploadState(spaceId)) {

    /**
     * 지역 시트에서 열었으면 그 지역. 상태가 아니라 여기 두는 이유는
     * **화면에 그리는 값이 아니라 사진을 만들 때 쓰는 재료**이기 때문입니다.
     */
    private var preselected: Region? = null

    override fun onIntent(intent: UploadIntent) {
        when (intent) {
            is UploadIntent.RegionPreselected -> preselected = intent.region
            is UploadIntent.PhotosPicked -> pick(intent.uris)
            is UploadIntent.RegionFieldTapped ->
                setState { UploadReducer.regionPickerOpened(this, intent.uri) }
            is UploadIntent.RegionQueryTyped -> search(intent.value)
            is UploadIntent.RegionChosen -> setState { UploadReducer.regionChosen(this, intent.region) }
            is UploadIntent.DateFieldTapped -> {
                val item = currentState().items.firstOrNull { it.uri == intent.uri } ?: return
                sendEffect(UploadEffect.OpenDatePicker(intent.uri, item.takenOn))
            }
            is UploadIntent.DateChosen ->
                setState { UploadReducer.dateChosen(this, intent.uri, intent.date) }
            UploadIntent.Confirmed -> upload()
            UploadIntent.RetryTapped -> setState { UploadReducer.retry(this) }
            UploadIntent.Dismissed ->
                // 지역 고르기 중이면 그것만 닫습니다 — 화면째 닫으면 고른 사진이 다 날아갑니다.
                if (currentState().editingRegionOf != null) {
                    setState { UploadReducer.regionPickerDismissed(this) }
                } else {
                    sendEffect(UploadEffect.Close)
                }
        }
    }

    private fun pick(uris: List<PickedPhoto>) {
        setState { UploadReducer.picked(this, uris) }
        if (uris.isEmpty()) return

        viewModelScope.launch {
            val today = LocalDate.now(clock)
            val hints = uris.map { readHints(it.uri) }

            // 제 값이 없는 사진은 **여럿의 값**으로 메웁니다. 한 장만 위치가 안 찍혀
            // 있다고 그 한 장만 빈칸으로 두면, 사용자가 그것만 따로 찾아 채워야 합니다.
            val fallback = UploadPlan.defaults(hints, today)
            val fallbackRegion = fallback.regionCode?.let { regions.find(it.value) }

            val items = uris.mapIndexed { index, picked ->
                val hint = hints[index]
                val own = hint.regionCode?.let { regions.find(it.value) }
                // 지역 시트에서 열었으면 **그 지역이 EXIF 보다 위**입니다. 사용자가
                // 지도에서 그 지역을 짚고 들어온 것이라, 사진이 무엇을 말하든
                // 여기에 넣겠다는 뜻입니다.
                val region = preselected ?: own ?: fallbackRegion
                UploadItem(
                    uri = picked.uri,
                    region = region,
                    takenOn = hint.takenOn ?: fallback.takenOn,
                    // 사진에서 왔든 여럿에서 메웠든 사용자가 고른 값은 아닙니다.
                    // 지역 시트에서 온 것은 고른 값이라 '자동' 딱지를 안 붙입니다.
                    regionAuto = preselected == null && region != null,
                    dateAuto = hint.takenOn != null || fallback.dateFromExif,
                )
            }
            setState { UploadReducer.hintsRead(this, items) }
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            val results = if (query.isBlank()) emptyList() else searchRegions(query)
            setState { UploadReducer.regionQueryTyped(this, query, results) }
        }
    }

    private fun upload() {
        val state = currentState()
        if (!state.canUpload()) return

        setState { UploadReducer.uploading(this) }

        viewModelScope.launch {
            val drafts = ArrayList<NewPhoto>(state.items.size)
            for ((index, item) in state.items.withIndex()) {
                val region = item.region ?: continue
                val bytes = toJpeg(item.uri)
                if (bytes == null) {
                    keepLocally(item.uri)
                    continue
                }
                // 사진마다 **제 지역·제 날짜**로 올라갑니다.
                drafts += NewPhoto(
                    localId = "${item.uri}#${index}",
                    bytes = bytes,
                    regionCode = region.code,
                    takenOn = item.takenOn,
                )
            }

            if (drafts.isEmpty()) {
                setState { UploadReducer.failed(this, savedLocally = true) }
                sendEffect(UploadEffect.ShowMessage(UploadMessage.UnreadableKept))
                return@launch
            }

            when (uploadPhotos(state.spaceId, drafts)) {
                is Outcome.Ok -> {
                    setState { UploadReducer.uploaded(this) }
                    sendEffect(UploadEffect.Close)
                }
                is Outcome.Fail -> {
                    // 사진을 잃지 않는 것이 먼저입니다
                    state.items.forEach { keepLocally(it.uri) }
                    setState { UploadReducer.failed(this, savedLocally = true) }
                    sendEffect(UploadEffect.ShowMessage(UploadMessage.UploadFailedKept))
                }
            }
        }
    }
}
