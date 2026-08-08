package kr.surprise.memorymap.feature.map

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

    private fun serve(client: Socket) {
        client.use {
            val line = it.getInputStream().bufferedReader().readLine() ?: return
            // "GET /15/27945/12696 HTTP/1.1" 에서 가운데 경로만
            val path = line.split(" ").getOrNull(1) ?: return
            val body = try {
                pixelated(path)
            } catch (_: Exception) {
                null
            }
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
    }

    private fun pixelated(path: String): ByteArray? {
        val upstream = URL("https://basemaps.cartocdn.com/light_nolabels$path.png")
        val connection = upstream.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("User-Agent", "jjaguk-android")

        val original = connection.inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
        val small = Bitmap.createScaledBitmap(original, CELLS, CELLS, true)

        // 채도·대비·포스터라이즈를 픽셀마다. 48×48 = 2,304칸이라 값싼 일입니다.
        val pixels = IntArray(CELLS * CELLS)
        small.getPixels(pixels, 0, CELLS, 0, 0, CELLS, CELLS)
        for (i in pixels.indices) {
            val p = pixels[i]
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF
            // CARTO 라이트는 색이 너무 옅어 그대로 픽셀화하면 밋밋합니다 —
            // 검수된 시안이 이 값으로 만들어졌습니다.
            val gray = (r * 30 + g * 59 + b * 11) / 100
            r = clamp(gray + ((r - gray) * SATURATION) / 100)
            g = clamp(gray + ((g - gray) * SATURATION) / 100)
            b = clamp(gray + ((b - gray) * SATURATION) / 100)
            r = clamp(((r - 128) * CONTRAST) / 100 + 128)
            g = clamp(((g - 128) * CONTRAST) / 100 + 128)
            b = clamp(((b - 128) * CONTRAST) / 100 + 128)
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
        private const val TILE = 256
        private const val CELLS = 48
        private const val SATURATION = 170  // ×1.7 을 정수 연산으로
        private const val CONTRAST = 118    // ×1.18
    }
}
