import FeatureMap
import FirebaseRemoteConfig
import Foundation

/**
 Remote Config → `MapTuning`. 앱을 다시 내지 않고 지도 손잡이(픽셀 칸수·밤 시간)를
 돌릴 수 있게 합니다. Firebase 는 여기서만 알고, 지도 모듈은 값만 받습니다 —
 관측 스택과 같은 경계입니다. 안드로이드 `RemoteTuning` 과 같은 자리.

 값이 없거나 아직 못 받았으면 **코드의 기본값이 그대로**입니다. RC 는 덮어쓰기만
 합니다 — 서버가 없어도 앱은 어제와 똑같이 돕니다.
 */
enum RemoteTuning {

    /// 칸수는 플랫폼마다 다른 키입니다 — 타일 규격이 달라 (256규격 48칸 = 512규격 96칸)
    /// 한 키로 두 앱을 함께 돌리면 한쪽이 반드시 어긋납니다.
    private static let pixelCells = "map_pixel_cells_ios"
    private static let nightStart = "map_night_start_hour"
    private static let nightEnd = "map_night_end_hour"

    static func start() {
        let config = RemoteConfig.remoteConfig()
        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = 3600
        config.configSettings = settings
        config.setDefaults([
            pixelCells: NSNumber(value: MapTuning.pixelCells),
            nightStart: NSNumber(value: MapTuning.nightStartHour),
            nightEnd: NSNumber(value: MapTuning.nightEndHour),
        ])
        apply(config)   // 지난 실행에서 받아 둔 값이 있으면 그것부터
        config.fetchAndActivate { _, _ in apply(config) }
    }

    /// 서버가 이상한 값을 넣어도 지도가 깨지지 않게 울타리를 칩니다.
    private static func apply(_ config: RemoteConfig) {
        MapTuning.pixelCells = min(512, max(16, config[pixelCells].numberValue.intValue))
        MapTuning.nightStartHour = min(23, max(0, config[nightStart].numberValue.intValue))
        MapTuning.nightEndHour = min(23, max(0, config[nightEnd].numberValue.intValue))
    }
}
