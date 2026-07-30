package kr.surprise.memorymap.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.surprise.memorymap.core.common.Limits
import java.io.ByteArrayOutputStream

/**
 * 올리기 전에 사진을 줄입니다. **웹과 같은 값** — 최대 변 760px, JPEG 품질 72.
 * (`assets/firebase.js`, `docs/app/CONVENTIONS.md`)
 *
 * 회전 정보(EXIF Orientation)를 반영해서 굽습니다. 안 하면 세로로 찍은 사진이
 * 지도에서 옆으로 누워 보입니다.
 */
class ImageDownscaler(private val context: Context) {

    suspend fun toJpeg(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = context.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@withContext null

        val rotated = applyOrientation(uri, decoded)
        val scaled = scaleToMaxEdge(rotated)

        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, Limits.JPEG_QUALITY, out)
            if (scaled !== decoded) scaled.recycle()
            if (rotated !== decoded && rotated !== scaled) rotated.recycle()
            decoded.recycle()
            out.toByteArray()
        }
    }

    /** 큰 사진을 통째로 메모리에 올리지 않으려고 디코딩 단계에서 미리 줄입니다. */
    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= Limits.MAX_EDGE_PX) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleToMaxEdge(source: Bitmap): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= Limits.MAX_EDGE_PX) return source
        val ratio = Limits.MAX_EDGE_PX.toFloat() / longest
        return Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt().coerceAtLeast(1),
            (source.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun applyOrientation(uri: Uri, source: Bitmap): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        } catch (e: Exception) {
            null
        } ?: return source

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
