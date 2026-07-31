package kr.surprise.memorymap.data.photo

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kr.surprise.memorymap.core.common.Failure
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.Cover
import kr.surprise.memorymap.core.model.CoverKey
import kr.surprise.memorymap.core.model.PathSafe
import kr.surprise.memorymap.core.model.Photo
import kr.surprise.memorymap.core.model.PhotoId
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.domain.model.NewPhoto
import kr.surprise.memorymap.domain.repository.PhotoRepository
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * **혼자 쓰는 짜국**의 사진. 앱 폴더에만 두고 서버를 아예 안 씁니다.
 *
 * ```
 * filesDir/spaces/<짜국ID>/photos/2026-03-05_11140_a1b2c3.jpg
 * filesDir/spaces/<짜국ID>/covers.json
 * ```
 *
 * [FirebasePhotoRepository] 와 **파일 이름·폴더 모양이 같습니다.** 나중에 '같이' 로 바꿀 때
 * 이 폴더를 그대로 올리면 되도록 하려는 것입니다 (`docs/app/AUTH.md`).
 *
 * 이름에 지역·날짜가 들어 있어 목록 한 번으로 다 알 수 있는 것도 서버 쪽과 같습니다.
 * 기기 안에는 Firestore 가 없으니 로그인이 붙어도 **혼자 짜국은 이 방식 그대로** 둡니다.
 */
class LocalPhotoRepository(
    context: Context,
    private val uploaderUid: String,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
    private val newId: () -> String = { UUID.randomUUID().toString().replace("-", "").take(16) },
) : PhotoRepository {

    private val root = File(context.filesDir, "spaces")

    private val photos = MutableStateFlow<Map<String, List<Photo>>>(emptyMap())
    private val covers = MutableStateFlow<Map<String, List<Cover>>>(emptyMap())

    override fun observePhotos(spaceId: SpaceId): Flow<List<Photo>> =
        photos.asStateFlow().map { it[spaceId.value].orEmpty() }

    override fun observeCovers(spaceId: SpaceId): Flow<List<Cover>> =
        covers.asStateFlow().map { it[spaceId.value].orEmpty() }

    override suspend fun refresh(spaceId: SpaceId): Outcome<Unit> = withContext(Dispatchers.IO) {
        PathSafe.require(spaceId.value, "공간 ID")

        // 폴더가 없는 것은 실패가 아닙니다 — 아직 한 장도 안 넣은 짜국입니다.
        val files = photoDir(spaceId).listFiles().orEmpty()
        val parsed = files.mapNotNull { file ->
            PhotoObjectName.parse(file.name)?.let { p ->
                Photo(
                    id = p.id,
                    regionCode = p.regionCode,
                    takenOn = p.takenOn,
                    storagePath = file.absolutePath,
                    downloadUrl = Uri.fromFile(file).toString(),
                    uploadedBy = uploaderUid,
                    uploadedAtEpochSeconds = p.stableOrder(),
                )
            }
        }
        photos.update { it + (spaceId.value to parsed) }
        loadCovers(spaceId)
        Outcome.Ok(Unit)
    }

    override suspend fun upload(spaceId: SpaceId, newPhotos: List<NewPhoto>): Outcome<List<Photo>> =
        withContext(Dispatchers.IO) {
            PathSafe.require(spaceId.value, "공간 ID")
            val dir = photoDir(spaceId)
            if (!dir.exists() && !dir.mkdirs()) return@withContext Outcome.Fail(Failure.Unknown)

            val saved = ArrayList<Photo>(newPhotos.size)
            for (draft in newPhotos) {
                val id = PhotoId(newId())
                val file = File(dir, PhotoObjectName.build(id, draft.regionCode, draft.takenOn))

                try {
                    file.writeBytes(draft.bytes)
                } catch (e: IOException) {
                    // 여기까지 쓴 것은 지우지 않습니다. 이미 들어간 사진을 되돌리면
                    // 사용자가 고른 것 중 무엇이 남았는지 알 수 없게 됩니다.
                    return@withContext Outcome.Fail(Failure.Unknown)
                }

                saved += Photo(
                    id = id,
                    regionCode = draft.regionCode,
                    takenOn = draft.takenOn,
                    storagePath = file.absolutePath,
                    downloadUrl = Uri.fromFile(file).toString(),
                    uploadedBy = uploaderUid,
                    uploadedAtEpochSeconds = now(),
                )
            }

            photos.update { current ->
                current + (spaceId.value to (current[spaceId.value].orEmpty() + saved))
            }
            Outcome.Ok(saved)
        }

    override suspend fun delete(spaceId: SpaceId, id: PhotoId): Outcome<Unit> =
        withContext(Dispatchers.IO) {
            val target = photos.value[spaceId.value]?.firstOrNull { it.id == id }
                ?: return@withContext Outcome.Fail(Failure.NotFound)

            val file = File(target.storagePath)
            // 이미 없는 파일은 지운 것으로 봅니다 — 목록에서 사라지는 게 사용자가 원한 결과입니다.
            if (file.exists() && !file.delete()) return@withContext Outcome.Fail(Failure.Unknown)

            photos.update { current ->
                current + (spaceId.value to current[spaceId.value].orEmpty().filterNot { it.id == id })
            }
            Outcome.Ok(Unit)
        }

    override suspend fun setCover(spaceId: SpaceId, key: CoverKey, id: PhotoId): Outcome<Unit> =
        withContext(Dispatchers.IO) {
            PathSafe.require(spaceId.value, "공간 ID")
            val next = covers.value[spaceId.value].orEmpty()
                .filterNot { it.key.documentId == key.documentId } + Cover(key, id)

            val dir = spaceDir(spaceId)
            if (!dir.exists() && !dir.mkdirs()) return@withContext Outcome.Fail(Failure.Unknown)

            try {
                coversFile(spaceId).writeBytes(CoversFile.serialize(next))
            } catch (e: IOException) {
                return@withContext Outcome.Fail(Failure.Unknown)
            }

            covers.update { it + (spaceId.value to next) }
            Outcome.Ok(Unit)
        }

    private fun loadCovers(spaceId: SpaceId) {
        val file = coversFile(spaceId)
        val parsed = if (file.exists()) {
            try {
                CoversFile.parse(file.readText())
            } catch (e: IOException) {
                emptyList()
            }
        } else {
            emptyList()   // 아직 대표를 한 번도 안 정한 짜국
        }
        covers.update { it + (spaceId.value to parsed) }
    }

    private fun spaceDir(spaceId: SpaceId) = File(root, spaceId.value)
    private fun photoDir(spaceId: SpaceId) = File(spaceDir(spaceId), "photos")
    private fun coversFile(spaceId: SpaceId) = File(spaceDir(spaceId), "covers.json")
}
