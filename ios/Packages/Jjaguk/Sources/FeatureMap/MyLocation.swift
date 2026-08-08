import CoreLocation
import Foundation

/**
 지금 있는 자리를 찾습니다. 안드로이드 `MyLocation.kt` 와 같은 결과를 냅니다.

 `CLLocationManager` 는 델리게이트로 답하는 옛 방식이라, 여기서 `async` 로 감싸
 화면 쪽에서는 한 줄로 부를 수 있게 합니다.
 */
public enum MyLocation: Sendable {
    case found(latitude: Double, longitude: Double)

    /// 사용자가 거절했거나 기기 정책으로 막혔다
    case denied

    /// 위치 기능 자체가 꺼져 있다
    case off

    /// 켜져 있는데 제때 못 잡았다
    case notFound
}

/**
 한 번 부를 때마다 자리를 한 번 찾습니다.

 **정확도를 낮춰 부릅니다** (`kCLLocationAccuracyKilometer`). 지도를 옮기는 데는
 동네 수준이면 충분하고, 정밀도를 높이면 그만큼 오래 걸리고 배터리도 씁니다.
 안드로이드에서 `ACCESS_COARSE_LOCATION` 만 받는 것과 같은 판단입니다.

 화면이 살아 있는 동안만 쓰므로 `@MainActor` 입니다 — `CLLocationManager` 는
 만든 스레드로 답을 돌려줍니다.
 */
@MainActor
public final class MyLocationFinder: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()

    /// 권한 창의 답을 기다리는 자리
    private var permission: CheckedContinuation<CLAuthorizationStatus, Never>?
    /// 좌표를 기다리는 자리
    private var fix: CheckedContinuation<MyLocation, Never>?

    public override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
    }

    /**
     자리를 찾습니다. 권한이 아직 없으면 **여기서 물어보고** 답을 기다립니다 —
     화면 쪽에서 권한과 좌표를 따로 다루지 않아도 됩니다.
     */
    public func find() async -> MyLocation {
        var status = manager.authorizationStatus

        if status == .notDetermined {
            status = await withCheckedContinuation { continuation in
                permission = continuation
                manager.requestWhenInUseAuthorization()
            }
        }

        switch status {
        case .denied, .restricted:
            return .denied
        case .notDetermined:
            // 창을 닫아 버린 경우. 다음에 다시 물어볼 수 있으니 거절로 기록하지 않습니다.
            return .notFound
        default:
            break
        }

        guard CLLocationManager.locationServicesEnabled() else { return .off }

        // 마지막으로 알던 자리가 있으면 그것부터 씁니다. 대개 즉시 나오고,
        // 지도를 옮기는 데는 몇 분 전 자리로도 충분합니다.
        if let last = manager.location, -last.timestamp.timeIntervalSinceNow <= staleSeconds {
            return .found(latitude: last.coordinate.latitude, longitude: last.coordinate.longitude)
        }

        return await withCheckedContinuation { continuation in
            fix = continuation
            manager.requestLocation()
        }
    }

    // MARK: - CLLocationManagerDelegate

    public nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in
            // 권한 창을 띄우지 않았을 때도 불립니다(앱을 켤 때 한 번). 기다리는 사람이
            // 없으면 그냥 넘깁니다 — 안 그러면 continuation 을 두 번 깨웁니다.
            guard status != .notDetermined, let waiting = permission else { return }
            permission = nil
            waiting.resume(returning: status)
        }
    }

    public nonisolated func locationManager(
        _ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]
    ) {
        let coordinate = locations.last?.coordinate
        Task { @MainActor in
            guard let waiting = fix else { return }
            fix = nil
            if let coordinate {
                waiting.resume(returning: .found(
                    latitude: coordinate.latitude, longitude: coordinate.longitude
                ))
            } else {
                waiting.resume(returning: .notFound)
            }
        }
    }

    public nonisolated func locationManager(
        _ manager: CLLocationManager, didFailWithError error: Error
    ) {
        Task { @MainActor in
            guard let waiting = fix else { return }
            fix = nil
            // 실내에서는 끝내 안 잡히기도 합니다. 실패와 못 찾음을 가르지 않습니다 —
            // 사용자가 할 수 있는 일("잠시 뒤에 다시")이 어느 쪽이든 같습니다.
            waiting.resume(returning: .notFound)
        }
    }
}

/// 이보다 오래된 자리는 안 씁니다. 5분이면 지도를 옮기는 데는 충분히 '지금' 입니다.
/// 안드로이드 `STALE_MS` 와 같은 값입니다.
private let staleSeconds: TimeInterval = 5 * 60
