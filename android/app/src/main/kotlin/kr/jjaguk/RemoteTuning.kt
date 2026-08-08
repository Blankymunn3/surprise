package kr.jjaguk

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kr.jjaguk.feature.map.MapTuning

/**
 * Remote Config → [MapTuning]. 앱을 다시 내지 않고 지도 손잡이(픽셀 칸수·밤 시간)를
 * 돌릴 수 있게 합니다. Firebase 는 여기서만 알고, 지도 모듈은 값만 받습니다 —
 * 관측 스택과 같은 경계입니다 (`Tracking.kt`).
 *
 * 값이 없거나 아직 못 받았으면 **코드의 기본값이 그대로**입니다. RC 는 덮어쓰기만
 * 합니다 — 서버가 없어도 앱은 어제와 똑같이 돕니다.
 */
object RemoteTuning {

    private const val PIXEL_CELLS = "map_pixel_cells_android"
    private const val NIGHT_START = "map_night_start_hour"
    private const val NIGHT_END = "map_night_end_hour"

    fun start() {
        val config = FirebaseRemoteConfig.getInstance()
        config.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
        )
        val defaults = mapOf<String, Any>(
            PIXEL_CELLS to MapTuning.pixelCells.toLong(),
            NIGHT_START to MapTuning.nightStartHour.toLong(),
            NIGHT_END to MapTuning.nightEndHour.toLong(),
        )
        // 기본값 등록이 **끝난 뒤에만** 적용합니다 — 그전에 읽으면 0 이 나와서
        // 지도가 16칸짜리 뭉개진 그림이 됩니다.
        config.setDefaultsAsync(defaults).addOnCompleteListener {
            apply(config)
            config.fetchAndActivate().addOnCompleteListener { apply(config) }
        }
    }

    /** 서버가 이상한 값을 넣어도 지도가 깨지지 않게 울타리를 칩니다. */
    private fun apply(config: FirebaseRemoteConfig) {
        MapTuning.pixelCells = config.getLong(PIXEL_CELLS).toInt().coerceIn(16, 512)
        MapTuning.nightStartHour = config.getLong(NIGHT_START).toInt().coerceIn(0, 23)
        MapTuning.nightEndHour = config.getLong(NIGHT_END).toInt().coerceIn(0, 23)
    }
}
