package kr.jjaguk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * 지금 있는 자리를 찾습니다.
 *
 * **안드로이드 기본 [LocationManager] 만 씁니다.** Play Services 의
 * `FusedLocationProviderClient` 가 더 똑똑하지만, 이 앱은 지도부터 MapLibre 로 골라
 * 구글 의존을 피해 왔습니다(`docs/app/ARCHITECTURE.md`). 지도를 한 번 옮기는 데
 * 필요한 정밀도는 기본 제공자로 충분합니다.
 *
 * 권한은 `ACCESS_COARSE_LOCATION` 하나입니다. 동네 수준이면 지도를 옮기기에 충분하고,
 * 정확한 위치까지 달라고 하면 사용자가 거절할 이유만 늘어납니다.
 */
internal sealed interface MyLocation {
    data class Found(val latitude: Double, val longitude: Double) : MyLocation

    /** 권한이 없다 — 부르는 쪽이 물어보고 다시 시도한다 */
    data object NoPermission : MyLocation

    /** 위치 기능이 꺼져 있다 (기내 모드·위치 끔) */
    data object Off : MyLocation

    /** 켜져 있는데 제때 못 잡았다 */
    data object NotFound : MyLocation
}

/**
 * 권한만 봅니다.
 *
 * **[findMyLocation] 으로 대신 확인하면 안 됩니다** — 그것은 권한이 있으면 실제로
 * 위치를 찾으러 가서 최대 8초를 씁니다. 권한 창을 띄울지만 정하려던 자리에서 부르면
 * 그 기다림이 두 번 생깁니다.
 */
internal fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * 마지막으로 알려진 자리를 먼저 봅니다. 그것이 없거나 너무 오래됐으면 **한 번만**
 * 새로 받습니다.
 *
 * 마지막 자리부터 보는 이유: 대개 즉시 나오고, 지도를 옮기는 데는 몇 분 전 자리로도
 * 충분합니다. 새로 받으려면 기기가 위성·기지국을 다시 잡아야 해서 몇 초씩 걸립니다.
 */
internal suspend fun findMyLocation(context: Context): MyLocation {
    if (!hasLocationPermission(context)) return MyLocation.NoPermission

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return MyLocation.Off

    val providers = manager.getProviders(true)
    if (providers.isEmpty()) return MyLocation.Off

    lastKnown(manager, providers)?.let { return MyLocation.Found(it.latitude, it.longitude) }

    // 새로 받습니다. 오래 기다리게 두지 않습니다 — 실내에서는 끝내 안 잡히기도 합니다.
    val fresh = withTimeoutOrNull(FRESH_TIMEOUT_MS) { requestOnce(manager, providers) }
    return if (fresh != null) MyLocation.Found(fresh.latitude, fresh.longitude) else MyLocation.NotFound
}

/**
 * 제공자마다 마지막 자리를 물어보고 **가장 최근 것**을 고릅니다.
 * 네트워크와 GPS 가 각각 다른 시각의 자리를 들고 있을 수 있습니다.
 */
private fun lastKnown(manager: LocationManager, providers: List<String>): Location? =
    providers
        .mapNotNull { provider ->
            @Suppress("MissingPermission") // 위에서 확인했습니다
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        .filter { System.currentTimeMillis() - it.time <= STALE_MS }
        .maxByOrNull { it.time }

/**
 * 한 번만 받고 곧바로 끊습니다. 계속 듣고 있으면 화면을 벗어나도 배터리를 씁니다.
 *
 * 어느 제공자가 먼저 답할지 몰라 **다 걸어 두고** 첫 답을 씁니다.
 */
private suspend fun requestOnce(manager: LocationManager, providers: List<String>): Location? =
    suspendCancellableCoroutine { continuation ->
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                @Suppress("MissingPermission")
                runCatching { manager.removeUpdates(this) }
                if (continuation.isActive) continuation.resume(location)
            }

            // 옛 기기(API 30 미만)에서는 이 셋이 추상 메서드라 비워도 두어야 합니다.
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
            @Deprecated("옛 기기 호환")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
        }

        @Suppress("MissingPermission")
        val requested = providers.count { provider ->
            runCatching {
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }.isSuccess
        }
        if (requested == 0 && continuation.isActive) continuation.resume(null)

        continuation.invokeOnCancellation {
            @Suppress("MissingPermission")
            runCatching { manager.removeUpdates(listener) }
        }
    }

/** 이보다 오래된 자리는 안 씁니다. 5분이면 지도를 옮기는 데는 충분히 '지금' 입니다. */
private const val STALE_MS = 5 * 60 * 1000L

/** 새로 받기를 기다리는 한도. 실내에서는 끝내 안 잡히기도 합니다. */
private const val FRESH_TIMEOUT_MS = 8_000L
