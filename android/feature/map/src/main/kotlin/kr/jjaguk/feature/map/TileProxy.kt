package kr.jjaguk.feature.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors

/**
 * 지도 바탕 타일을 **픽셀 그림으로 바꿔 주는** 앱 안의 작은 서버.
 *
 * MapLibre 는 타일을 URL 로만 받지, 받은 그림을 앱이 중간에서 만질 길이 없습니다.
 * 그래서 127.0.0.1 에 문을 하나 열고, MapLibre 가 여기로 타일을 달라고 하면
 * 서버(CARTO)에서 받아 픽셀화해서 내줍니다. 이 파일 밖에서는 아무도 이 서버의
 * 존재를 모릅니다 — 스타일 JSON 의 타일 주소가 여기를 가리킬 뿐입니다.
 *
 * **픽셀화는 iOS(`PhotoMap.PixelTileOverlay`)와 같은 수식이어야 합니다** —
 * 타일당 48칸, 채도 1.7, 대비 1.18, 4비트 포스터라이즈. 한쪽만 바꾸면
 * 두 폰을 나란히 놓았을 때 지도가 다르게 생겼습니다.
 *
 * 검수된 시안(2026-08-08, "라이트 96")의 값입니다: 화면 512px 에 96칸
 * = 타일(256px)당 48칸.
 */
/** [dark] 면 밤 지도 — 어두운 타일을 받아 어두운 쪽 정보를 증폭합니다. */
internal class TileProxy(private val dark: Boolean = false) : AutoCloseable {

    private val socket = ServerSocket(0)   // 빈 포트를 하나 받는다
    private val pool = Executors.newFixedThreadPool(4)

    val port: Int get() = socket.localPort

    init {
        Thread({
            while (!socket.isClosed) {
                val client = try {
                    socket.accept()
                } catch (_: Exception) {
                    break   // close() 로 닫힌 것 — 종료
                }
                pool.execute { serve(client) }
            }
        }, "tile-proxy").apply { isDaemon = true }.start()
    }

    override fun close() {
        socket.close()
        pool.shutdownNow()
    }

    /**
     * ⚠️ **이 안의 어떤 예외도 밖으로 나가면 안 됩니다.** 여기는 스레드 풀이고,
     * 풀 스레드의 미처리 예외는 앱 전체를 죽입니다. 실제로 그랬습니다 — 줌을 하면
     * MapLibre 가 필요 없어진 타일 요청을 끊는데, 이미 닫힌 소켓에 응답을 쓰다
     * Broken pipe 로 앱이 죽었습니다. 끊긴 요청은 실패가 아니라 **일상**입니다.
     */
    private fun serve(client: Socket) {
        try {
            client.use {
                val line = it.getInputStream().bufferedReader().readLine() ?: return
                // "GET /15/27945/12696 HTTP/1.1" 에서 가운데 경로만
                val path = line.split(" ").getOrNull(1) ?: return
                val body = pixelated(path)
                val out = it.getOutputStream()
                if (body == null) {
                    out.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n".toByteArray())
                } else {
                    out.write(
                        ("HTTP/1.1 200 OK\r\nContent-Type: image/png\r\n" +
                            // MapLibre 의 디스크 캐시가 이걸 보고 타일을 물고 있습니다 —
                            // 같은 타일을 매번 다시 픽셀화하지 않게.
                            "Cache-Control: max-age=604800\r\n" +
                            "Content-Length: ${body.size}\r\n\r\n").toByteArray()
                    )
                    out.write(body)
                }
                out.flush()
            }
        } catch (_: Exception) {
            // 끊긴 소켓·못 받은 타일 — 그 요청 하나만 버립니다.
        }
    }

    private fun pixelated(path: String): ByteArray? {
        val kind = if (dark) "dark_nolabels" else "light_nolabels"
        val upstream = URL("https://basemaps.cartocdn.com/$kind$path.png")
        val connection = upstream.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("User-Agent", "jjaguk-android")

        val original = connection.inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
        // 요청 하나 안에서는 한 값만 씁니다 — 도중에 RC 가 덮으면 배열 크기가 어긋납니다.
        val zoom = path.split('/').getOrNull(1)?.toIntOrNull() ?: 15
        val cells = cellsFor(zoom, MapTuning.pixelCells)
        val small = Bitmap.createScaledBitmap(original, cells, cells, true)

        // 옅음 증폭·포스터라이즈를 픽셀마다. 112×112 = 12,544칸이라 값싼 일입니다.
        val pixels = IntArray(cells * cells)
        small.getPixels(pixels, 0, cells, 0, 0, cells, cells)
        for (i in pixels.indices) {
            val p = pixels[i]
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF
            // **증폭** — CARTO 타일은 정보가 배경색 근처에 몰려 있어 그대로
            // 픽셀화하면 도로가 통째로 사라집니다(실기기에서 확인). 라이트는
            // 흰색에서 먼 만큼을(×3), 다크는 검정에서 먼 만큼을(×4) 벌립니다.
            // 값들은 실물 타일 z11·z15 로 견줘 골랐습니다.
            if (dark) {
                r = clamp(r * AMPLIFY_DARK)
                g = clamp(g * AMPLIFY_DARK)
                b = clamp(b * AMPLIFY_DARK)
            } else {
                r = clamp(255 - (255 - r) * AMPLIFY)
                g = clamp(255 - (255 - g) * AMPLIFY)
                b = clamp(255 - (255 - b) * AMPLIFY)
            }
            // 4비트 포스터라이즈 — 색을 16단계로 눌러야 픽셀아트 결이 남습니다.
            r = r and 0xF0; g = g and 0xF0; b = b and 0xF0
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        small.setPixels(pixels, 0, cells, 0, 0, cells, cells)

        // filter=false 가 nearest 확대 — 픽셀의 모서리가 살아야 합니다.
        val big = Bitmap.createScaledBitmap(small, TILE, TILE, false)
        return ByteArrayOutputStream().use {
            big.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.toByteArray()
        }
    }

    private fun clamp(v: Int) = v.coerceIn(0, 255)

    companion object {
        /**
         * iOS 와 **같은 그림**이 되는 조합은 512 규격 + 96칸입니다.
         *
         * - 256 규격 + 48칸: 고밀도 화면에서 MapLibre 가 더 높은 줌의 타일을
         *   가져와 칸이 iOS 의 절반 크기(잘아서 노이즈처럼 보임)
         * - 512 규격 + 48칸: 칸은 커졌는데 **정보 밀도가 절반**이라 가는 도로가
         *   칸 하나를 못 채워 통째로 사라짐 (실기기에서 확인)
         * - 512 규격 + 96칸: 칸 크기 512/96 = iOS 의 256/48 과 동일, 정보도 동일
         *
         * 96 → 112 는 사용자 요청("아주 조금만 더") — 디테일이 살짝 늘고
         * 픽셀 질감은 유지되는 선. iOS 와 칸 크기가 미세하게 달라지는 것은
         * 감수한 선택이다. 칸수는 [MapTuning.pixelCells] 로 옮겨 RC 로 돌릴 수
         * 있게 했다 — 여기 512 규격과의 조합 제약은 그대로다.
         */
        private const val TILE = 512
        private const val AMPLIFY = 3
        private const val AMPLIFY_DARK = 4

        /** 여기까지는 칸이 가장 잘다(×1.5) — 나라·세계가 보이는 범위. */
        private const val FINE_ZOOM_MAX = 7

        /** 여기부터는 검수값 그대로 — 동네가 보이는 범위. */
        private const val BASE_ZOOM_MIN = 14

        /**
         * 줌에 따라 칸수를 **서서히** 바꿉니다: z≤7 은 ×1.5, z≥14 는 검수값,
         * 사이(도·시)는 직선으로 줄어듭니다. 시 단위 줌에서 픽셀이 아쉽고
         * 단계가 튀지 않게 "부드럽게" 라는 피드백(2026-08-09)의 답입니다 —
         * 큰 형태(해안선·시가지)일수록 잘게 쪼개야 뭉개지지 않습니다.
         * iOS(PixelTileOverlay.cellsFor)와 같은 규칙.
         */
        internal fun cellsFor(zoom: Int, base: Int): Int = when {
            zoom <= FINE_ZOOM_MAX -> base * 3 / 2
            zoom >= BASE_ZOOM_MIN -> base
            else -> base + base * (BASE_ZOOM_MIN - zoom) /
                (2 * (BASE_ZOOM_MIN - FINE_ZOOM_MAX))
        }
    }
}
