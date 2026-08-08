package kr.surprise.memorymap

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

/**
 * 사진을 그리는 요청에도 토큰을 얹기 위해 **Coil 의 기본 로더를 우리 것으로 바꿉니다.**
 *
 * `FirebaseStorage` 가 다는 헤더는 REST 요청에만 실립니다. 사진은 Coil 이 자기 방식으로
 * 받아 오기 때문에, 여기서 손대지 않으면 규칙을 조이는 순간 목록은 나오는데 사진만
 * 안 뜨는 상태가 됩니다 (`docs/app/AUTH.md`).
 */
class JjagukApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { authorizedClient(container) }))
            }
            .build()

    private companion object {
        /**
         * 우리 버킷으로 나가는 요청에만 토큰을 답니다. 지도 타일처럼 남의 서버로 가는
         * 요청에 우리 토큰을 실어 보내면 안 됩니다.
         *
         * `runBlocking` 을 쓰는 이유: OkHttp 인터셉터는 정지 함수가 아닙니다. 토큰이
         * 살아 있으면 파일을 한 번 읽는 정도라 짧고, 갱신이 필요할 때만 네트워크를 탑니다.
         * Coil 의 가져오기는 이미 IO 스레드에서 돕니다.
         */
        fun authorizedClient(container: AppContainer): OkHttpClient =
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val token = if (request.url.host == "firebasestorage.googleapis.com") {
                        runBlocking { container.accounts.idToken() }
                    } else {
                        null
                    }
                    chain.proceed(
                        if (token == null) request
                        else request.newBuilder().header("Authorization", "Bearer $token").build()
                    )
                }
                .build()
    }
}

/** 지도처럼 Compose 밖에서 그림을 받아야 할 때도 **같은 로더**를 쓰게 합니다. */
fun Context.memoryImageLoader(): ImageLoader = SingletonImageLoader.get(this)
