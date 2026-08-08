package kr.jjaguk.data.photo

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jjaguk.domain.model.ExifHint
import kr.jjaguk.domain.repository.RegionCatalog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 사진 파일에서 찍은 날짜와 위치를 읽습니다. **기기 안에서만** 합니다 —
 * 좌표를 서버로 보내 "여기가 어디죠?" 하고 묻지 않으려는 것입니다.
 */
class ExifReader(
    private val context: Context,
    private val regions: RegionCatalog,
) {
    // EXIF 의 촬영 시각은 "2026:03:05 14:22:31" 꼴입니다
    private val exifDate = DateTimeFormatter.ofPattern("yyyy:MM:dd")

    suspend fun read(uri: Uri): ExifHint = withContext(Dispatchers.IO) {
        val exif = try {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        } catch (e: Exception) {
            null
        } ?: return@withContext ExifHint(null, null)

        ExifHint(takenOn = readDate(exif), regionCode = readRegion(exif)?.code)
    }

    private fun readDate(exif: ExifInterface): LocalDate? {
        val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            ?: return null
        // 시각은 버리고 날짜만 씁니다. 시간대 때문에 밤 사진이 다음 날로 밀리지 않게.
        return try {
            LocalDate.parse(raw.substringBefore(' '), exifDate)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private suspend fun readRegion(exif: ExifInterface) =
        exif.latLong?.let { (lat, lon) -> regions.regionAt(lat, lon) }

    private operator fun DoubleArray.component1() = this[0]
    private operator fun DoubleArray.component2() = this[1]
}
