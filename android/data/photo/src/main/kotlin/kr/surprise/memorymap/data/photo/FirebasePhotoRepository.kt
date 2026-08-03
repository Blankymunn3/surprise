package kr.surprise.memorymap.data.photo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
import kr.surprise.memorymap.core.network.Firestore
import kr.surprise.memorymap.domain.model.NewPhoto
import kr.surprise.memorymap.domain.repository.AuthRepository
import kr.surprise.memorymap.domain.repository.PhotoRepository
import java.time.LocalDate

/**
 * **같이 쓰는 짜국**의 사진. 파일은 Storage 에, **지역·날짜는 Firestore 문서**에 둡니다.
 *
 * ```
 * spaces/{짜국ID}/photos/{사진ID}.jpg        파일 (Storage)
 * spaces/{짜국ID}/photos/{사진ID}            지역·날짜·올린 사람 (Firestore)
 * spaces/{짜국ID}/covers/{대표키}            대표사진 (Firestore)
 * ```
 *
 * 전에는 지역·날짜를 **파일 이름에** 적었습니다(`PhotoObjectName`). 로그인도 Firestore 도
 * 없어서 사진 정보를 둘 곳이 없었기 때문입니다. 이제 문서가 있으니 이름은 ID 하나로
 * 짧아졌습니다 (`docs/app/AUTH.md`).
 *
 * **혼자 쓰는 짜국은 여전히 파일 이름 방식**입니다 ([LocalPhotoRepository]) —
 * 기기 안에는 Firestore 가 없으니까요.
 */
class FirebasePhotoRepository(
    private val storage: FirebaseStorage,
    private val firestore: Firestore,
    private val accounts: AuthRepository,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
    private val newId: () -> String = { java.util.UUID.randomUUID().toString().replace("-", "").take(16) },
) : PhotoRepository {

    private val photos = MutableStateFlow<Map<String, List<Photo>>>(emptyMap())
    private val covers = MutableStateFlow<Map<String, List<Cover>>>(emptyMap())

    override fun observePhotos(spaceId: SpaceId): Flow<List<Photo>> =
        photos.asStateFlow().map { it[spaceId.value].orEmpty() }

    override fun observeCovers(spaceId: SpaceId): Flow<List<Cover>> =
        covers.asStateFlow().map { it[spaceId.value].orEmpty() }

    override suspend fun refresh(spaceId: SpaceId): Outcome<Unit> {
        PathSafe.require(spaceId.value, "짜국 ID")

        val documents = when (val listed = firestore.list(photoCollection(spaceId))) {
            is Outcome.Fail -> return listed
            is Outcome.Ok -> listed.value
        }

        val parsed = documents.mapNotNull { document ->
            val takenOn = document.text("takenOn")?.let {
                try {
                    LocalDate.parse(it)
                } catch (e: Exception) {
                    null
                }
            } ?: return@mapNotNull null   // 날짜가 없으면 달력에 놓을 자리가 없습니다
            val path = document.text("storagePath") ?: return@mapNotNull null

            Photo(
                id = PhotoId(document.id),
                regionCode = RegionCode(document.text("regionCode").orEmpty()),
                takenOn = takenOn,
                storagePath = path,
                downloadUrl = storage.downloadUrl(path),
                uploadedBy = document.text("uploadedBy").orEmpty(),
                // 이제 **올린 시각이 문서에 있습니다.** 목록만 보고 흉내 내던 값
                // (`stableOrder`)은 혼자 짜국에만 남았습니다.
                uploadedAtEpochSeconds = document.number("uploadedAt") ?: 0,
            )
        }

        photos.update { it + (spaceId.value to parsed) }
        loadCovers(spaceId)
        return Outcome.Ok(Unit)
    }

    override suspend fun upload(spaceId: SpaceId, newPhotos: List<NewPhoto>): Outcome<List<Photo>> {
        PathSafe.require(spaceId.value, "짜국 ID")
        val uid = accounts.account()?.uid ?: return Outcome.Fail(Failure.Denied)
        val saved = ArrayList<Photo>(newPhotos.size)

        for (draft in newPhotos) {
            val id = PhotoId(newId())
            val path = photoDir(spaceId) + id.value + ".jpg"

            // 파일을 **먼저** 올립니다. 문서만 남고 파일이 없으면 빈 칸이 보이는데,
            // 반대(파일만 있고 문서가 없음)는 목록에 안 나올 뿐이라 덜 나쁩니다.
            when (val uploaded = storage.upload(path, draft.bytes, "image/jpeg")) {
                is Outcome.Fail -> return Outcome.Fail(uploaded.reason)
                is Outcome.Ok -> Unit
            }

            val uploadedAt = now()
            val fields = mapOf(
                "regionCode" to Firestore.Value.Text(draft.regionCode.value),
                "takenOn" to Firestore.Value.Text(draft.takenOn.toString()),
                "storagePath" to Firestore.Value.Text(path),
                "uploadedBy" to Firestore.Value.Text(uid),
                "uploadedAt" to Firestore.Value.Number(uploadedAt),
            )
            when (val written = firestore.set("${photoCollection(spaceId)}/${id.value}", fields)) {
                is Outcome.Fail -> return Outcome.Fail(written.reason)
                is Outcome.Ok -> Unit
            }

            saved += Photo(
                id = id,
                regionCode = draft.regionCode,
                takenOn = draft.takenOn,
                storagePath = path,
                downloadUrl = storage.downloadUrl(path),
                uploadedBy = uid,
                uploadedAtEpochSeconds = uploadedAt,
            )
        }

        photos.update { current ->
            current + (spaceId.value to (current[spaceId.value].orEmpty() + saved))
        }
        return Outcome.Ok(saved)
    }

    override suspend fun delete(spaceId: SpaceId, id: PhotoId): Outcome<Unit> {
        val target = photos.value[spaceId.value]?.firstOrNull { it.id == id }
            ?: return Outcome.Fail(Failure.NotFound)

        // 문서를 **먼저** 지웁니다. 파일만 남는 것은 눈에 안 보이지만, 문서만 남으면
        // 목록에 빈 칸이 생깁니다.
        when (val removed = firestore.delete("${photoCollection(spaceId)}/${id.value}")) {
            is Outcome.Fail -> return removed
            is Outcome.Ok -> Unit
        }
        storage.delete(target.storagePath)

        photos.update { current ->
            current + (spaceId.value to current[spaceId.value].orEmpty().filterNot { it.id == id })
        }
        return Outcome.Ok(Unit)
    }

    override suspend fun setCover(spaceId: SpaceId, key: CoverKey, id: PhotoId): Outcome<Unit> {
        val written = firestore.set(
            "${coverCollection(spaceId)}/${key.documentId}",
            mapOf("photoId" to Firestore.Value.Text(id.value)),
        )
        return when (written) {
            is Outcome.Fail -> written
            is Outcome.Ok -> {
                val next = covers.value[spaceId.value].orEmpty()
                    .filterNot { it.key.documentId == key.documentId } + Cover(key, id)
                covers.update { it + (spaceId.value to next) }
                Outcome.Ok(Unit)
            }
        }
    }

    private suspend fun loadCovers(spaceId: SpaceId) {
        val parsed = when (val listed = firestore.list(coverCollection(spaceId))) {
            is Outcome.Fail -> emptyList()   // 아직 대표를 한 번도 안 정한 짜국
            is Outcome.Ok -> listed.value.mapNotNull { document ->
                val photoId = document.text("photoId") ?: return@mapNotNull null
                CoverKey.of(document.id)?.let { Cover(it, PhotoId(photoId)) }
            }
        }
        covers.update { it + (spaceId.value to parsed) }
    }

    private fun photoDir(spaceId: SpaceId) = "spaces/${spaceId.value}/photos/"
    private fun photoCollection(spaceId: SpaceId) = "spaces/${spaceId.value}/photos"
    private fun coverCollection(spaceId: SpaceId) = "spaces/${spaceId.value}/covers"
}
