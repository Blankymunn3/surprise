package kr.surprise.memorymap.feature.upload

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.ui.MviViewModel
import kr.surprise.memorymap.domain.UploadPlan
import kr.surprise.memorymap.domain.model.ExifHint
import kr.surprise.memorymap.domain.model.NewPhoto
import kr.surprise.memorymap.domain.repository.RegionCatalog
import kr.surprise.memorymap.domain.usecase.SearchRegionsUseCase
import kr.surprise.memorymap.domain.usecase.UploadPhotosUseCase
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

    override fun onIntent(intent: UploadIntent) {
        when (intent) {
            is UploadIntent.PhotosPicked -> pick(intent.uris)
            UploadIntent.RegionFieldTapped -> setState { UploadReducer.regionPickerOpened(this) }
            is UploadIntent.RegionQueryTyped -> search(intent.value)
            is UploadIntent.RegionChosen -> setState { UploadReducer.regionChosen(this, intent.region) }
            is UploadIntent.DateChosen -> setState { UploadReducer.dateChosen(this, intent.date) }
            UploadIntent.Confirmed -> upload()
            UploadIntent.Dismissed -> sendEffect(UploadEffect.Close)
        }
    }

    private fun pick(uris: List<PickedPhoto>) {
        setState { UploadReducer.picked(this, uris) }
        if (uris.isEmpty()) return

        viewModelScope.launch {
            val hints = uris.map { readHints(it.uri) }
            val defaults = UploadPlan.defaults(hints, LocalDate.now(clock))
            val region = defaults.regionCode?.let { regions.find(it.value) }
            setState { UploadReducer.hintsRead(this, defaults, region) }
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
        val region = state.region ?: return
        val takenOn = state.takenOn ?: return

        setState { UploadReducer.uploading(this) }

        viewModelScope.launch {
            val drafts = ArrayList<NewPhoto>(state.picked.size)
            for ((index, picked) in state.picked.withIndex()) {
                val bytes = toJpeg(picked.uri)
                if (bytes == null) {
                    keepLocally(picked.uri)
                    continue
                }
                drafts += NewPhoto(
                    localId = "${picked.uri}#${index}",
                    bytes = bytes,
                    regionCode = region.code,
                    takenOn = takenOn,
                )
            }

            if (drafts.isEmpty()) {
                setState { UploadReducer.failed(this, savedLocally = true) }
                sendEffect(UploadEffect.ShowMessage("사진을 읽지 못해 기기에 남겨 뒀어요."))
                return@launch
            }

            when (uploadPhotos(state.spaceId, drafts)) {
                is Outcome.Ok -> {
                    setState { UploadReducer.uploaded(this) }
                    sendEffect(UploadEffect.Close)
                }
                is Outcome.Fail -> {
                    // 사진을 잃지 않는 것이 먼저입니다
                    state.picked.forEach { keepLocally(it.uri) }
                    setState { UploadReducer.failed(this, savedLocally = true) }
                    sendEffect(UploadEffect.ShowMessage("지금은 못 올려서 기기에 저장했어요. 나중에 다시 올릴게요."))
                }
            }
        }
    }
}
