package kr.surprise.memorymap.domain

import kr.surprise.memorymap.core.model.Photo
import kr.surprise.memorymap.core.model.PhotoId
import kr.surprise.memorymap.core.model.RegionCode
import java.time.LocalDate

fun photo(
    id: String,
    region: String = "11140",
    takenOn: LocalDate = LocalDate.of(2026, 3, 5),
    uploadedAt: Long = 0,
) = Photo(
    id = PhotoId(id),
    regionCode = RegionCode(region),
    takenOn = takenOn,
    storagePath = "spaces/s1/photos/$id.jpg",
    downloadUrl = "https://example.invalid/$id.jpg",
    uploadedBy = "u1",
    uploadedAtEpochSeconds = uploadedAt,
)
