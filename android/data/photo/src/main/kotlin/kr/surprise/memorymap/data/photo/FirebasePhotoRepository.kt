package kr.surprise.memorymap.data.photo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kr.surprise.memorymap.core.common.Failure
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.Cover
import kr.surprise.memorymap.core.model.CoverKey
import kr.surprise.memorymap.core.model.PathSafe
import kr.surprise.memorymap.core.model.Photo
import kr.surprise.memorymap.core.model.PhotoId
import kr.surprise.memorymap.core.model.RegionCode
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.network.FirebaseStorage
import kr.surprise.memorymap.domain.model.NewPhoto
import kr.surprise.memorymap.domain.repository.PhotoRepository
import java.time.LocalDate

/**
 * 사진을 Firebase Storage 에 REST 로 넣고 뺍니다. 웹과 같은 버킷을 씁니다.
 *
 * 대표사진은 `spaces/<공간ID>/covers.json` 한 파일에 모읍니다.
 * 로그인·Firestore 가 붙으면 문서로 옮깁니다 (`docs/app/SCREENS.md` 가 목표 구조).
 */
class FirebasePhotoRepository(
    private val storage: FirebaseStorage,
    private val uploaderUid: String,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
    private val newId: () -> String = { java.util.UUID.randomUUID().toString().replace("-", "").take(16) },
) : PhotoRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val photos = MutableStateFlow<Map<String, List<Photo>>>(emptyMap())
    private val covers = MutableStateFlow<Map<String, List<Cover>>>(emptyMap())

    override fun observePhotos(spaceId: SpaceId): Flow<List<Photo>> =
        photos.asStateFlow().map { it[spaceId.value].orEmpty() }

    override fun observeCovers(spaceId: SpaceId): Flow<List<Cover>> =
        covers.asStateFlow().map { it[spaceId.value].orEmpty() }

    override suspend fun refresh(spaceId: SpaceId): Outcome<Unit> {
        PathSafe.require(spaceId.value, "공간 ID")

        return when (val listed = storage.list(photoDir(spaceId))) {
            is Outcome.Fail -> listed
            is Outcome.Ok -> {
                val parsed = listed.value.mapNotNull { item ->
                    PhotoObjectName.parse(item.name)?.let { p ->
                        Photo(
                            id = p.id,
                            regionCode = p.regionCode,
                            takenOn = p.takenOn,
                            storagePath = item.fullPath,
                            downloadUrl = storage.downloadUrl(item.fullPath),
                            uploadedBy = uploaderUid,
                            // 목록 API 가 올린 시각을 주지 않습니다. 대표사진 기본값("가장 최근")이
                            // 흔들리지 않도록 결정적인 값을 만들어 씁니다. iOS 와 같은 규칙.
                            uploadedAtEpochSeconds = stableOrder(p),
                        )
                    }
                }
                photos.update { it + (spaceId.value to parsed) }
                loadCovers(spaceId)
                Outcome.Ok(Unit)
            }
        }
    }

    override suspend fun upload(spaceId: SpaceId, newPhotos: List<NewPhoto>): Outcome<List<Photo>> {
        PathSafe.require(spaceId.value, "공간 ID")
        val saved = ArrayList<Photo>(newPhotos.size)

        for (draft in newPhotos) {
            val id = PhotoId(newId())
            val name = PhotoObjectName.build(id, draft.regionCode, draft.takenOn)
            val path = photoDir(spaceId) + name

            when (val result = storage.upload(path, draft.bytes, "image/jpeg")) {
                is Outcome.Fail -> return Outcome.Fail(result.reason)
                is Outcome.Ok -> saved += Photo(
                    id = id,
                    regionCode = draft.regionCode,
                    takenOn = draft.takenOn,
                    storagePath = path,
                    downloadUrl = storage.downloadUrl(path),
                    uploadedBy = uploaderUid,
                    uploadedAtEpochSeconds = now(),
                )
            }
        }

        photos.update { current ->
            current + (spaceId.value to (current[spaceId.value].orEmpty() + saved))
        }
        return Outcome.Ok(saved)
    }

    override suspend fun delete(spaceId: SpaceId, id: PhotoId): Outcome<Unit> {
        val target = photos.value[spaceId.value]?.firstOrNull { it.id == id }
            ?: return Outcome.Fail(Failure.NotFound)

        return when (val result = storage.delete(target.storagePath)) {
            is Outcome.Fail -> result
            is Outcome.Ok -> {
                photos.update { current ->
                    current + (spaceId.value to current[spaceId.value].orEmpty().filterNot { it.id == id })
                }
                Outcome.Ok(Unit)
            }
        }
    }

    override suspend fun setCover(spaceId: SpaceId, key: CoverKey, id: PhotoId): Outcome<Unit> {
        val next = covers.value[spaceId.value].orEmpty()
            .filterNot { it.key.documentId == key.documentId } + Cover(key, id)

        val body = buildString {
            append('{')
            next.joinTo(this, ",") { "\"" + it.key.documentId + "\":\"" + it.photoId.value + "\"" }
            append('}')
        }.toByteArray()

        return when (val result = storage.upload(coversPath(spaceId), body, "application/json")) {
            is Outcome.Fail -> result
            is Outcome.Ok -> {
                covers.update { it + (spaceId.value to next) }
                Outcome.Ok(Unit)
            }
        }
    }

    private suspend fun loadCovers(spaceId: SpaceId) {
        val downloaded = storage.download(coversPath(spaceId))
        val parsed = when (downloaded) {
            is Outcome.Fail -> emptyList()   // 아직 대표를 한 번도 안 정한 공간
            is Outcome.Ok -> parseCovers(String(downloaded.value))
        }
        covers.update { it + (spaceId.value to parsed) }
    }

    private fun parseCovers(text: String): List<Cover> = try {
        json.parseToJsonElement(text).jsonObject.mapNotNull { (documentId, value) ->
            val photoId = (value as? JsonPrimitive)?.content ?: return@mapNotNull null
            documentIdToKey(documentId)?.let { Cover(it, PhotoId(photoId)) }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun documentIdToKey(documentId: String): CoverKey? = when {
        documentId.startsWith("region_") ->
            CoverKey.ForRegion(RegionCode(documentId.removePrefix("region_")))
        documentId.startsWith("day_") -> try {
            CoverKey.ForDay(LocalDate.parse(documentId.removePrefix("day_")))
        } catch (e: Exception) {
            null
        }
        else -> null
    }

    /** 찍은 날짜가 먼저, 같은 날이면 사진 ID 순. iOS `stableOrder` 와 같은 규칙입니다. */
    private fun stableOrder(parsed: PhotoObjectName.Parsed): Long {
        val day = parsed.takenOn.year * 10_000L + parsed.takenOn.monthValue * 100L + parsed.takenOn.dayOfMonth
        var tail = 0L
        for (c in parsed.id.value) tail = (tail * 31 + c.code) % 9_973
        return day * 10_000L + tail
    }

    private fun photoDir(spaceId: SpaceId) = "spaces/${spaceId.value}/photos/"
    private fun coversPath(spaceId: SpaceId) = "spaces/${spaceId.value}/covers.json"
}
