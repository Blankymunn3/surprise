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
internal class TileProxy : AutoCloseable {

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
        val upstream = URL("https://basemaps.cartocdn.com/light_nolabels$path.png")
        val connection = upstream.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("User-Agent", "jjaguk-android")

        val original = connection.inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
        val small = Bitmap.createScaledBitmap(original, CELLS, CELLS, true)

        // 옅음 증폭·포스터라이즈를 픽셀마다. 48×48 = 2,304칸이라 값싼 일입니다.
        val pixels = IntArray(CELLS * CELLS)
        small.getPixels(pixels, 0, CELLS, 0, 0, CELLS, CELLS)
        for (i in pixels.indices) {
            val p = pixels[i]
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF
            // **옅음 증폭** — CARTO 라이트는 정보가 흰색 근처에 몰려 있어, 그대로
            // 픽셀화하면 광역에서 도로가 통째로 사라집니다(실기기에서 확인).
            // 흰색에서 먼 만큼을 3배로 벌리면 도로·강·공원이 살아납니다.
            r = clamp(255 - (255 - r) * AMPLIFY)
            g = clamp(255 - (255 - g) * AMPLIFY)
            b = clamp(255 - (255 - b) * AMPLIFY)
            // 4비트 포스터라이즈 — 색을 16단계로 눌러야 픽셀아트 결이 남습니다.
            r = r and 0xF0; g = g and 0xF0; b = b and 0xF0
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        small.setPixels(pixels, 0, CELLS, 0, 0, CELLS, CELLS)

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
         * 감수한 선택이다.
         */
        private const val TILE = 512
        private const val CELLS = 112
        private const val AMPLIFY = 3
    }
}
