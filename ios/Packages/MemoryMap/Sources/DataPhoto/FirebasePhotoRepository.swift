import CoreCommon
import CoreModel
import CoreNetwork
import Domain
import Foundation

/// 사진을 Firebase Storage 에 REST 로 넣고 뺍니다. 웹·안드로이드와 **같은 버킷**입니다.
/// 대표사진은 `spaces/<공간ID>/covers.json` 한 파일에 모읍니다.
public actor FirebasePhotoRepository: PhotoRepository {

    private let storage: FirebaseStorage
    private let uploaderUid: String
    private let newId: @Sendable () -> String

    private var photosBySpace: [String: [Photo]] = [:]
    private var coversBySpace: [String: [Cover]] = [:]

    public init(
        storage: FirebaseStorage,
        uploaderUid: String,
        newId: @escaping @Sendable () -> String = { UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased() }
    ) {
        self.storage = storage
        self.uploaderUid = uploaderUid
        self.newId = newId
    }

    public func board(for spaceId: SpaceId) async -> PhotoBoard {
        PhotoBoard(
            photos: photosBySpace[spaceId.value] ?? [],
            covers: coversBySpace[spaceId.value] ?? []
        )
    }

    public func refresh(spaceId: SpaceId) async -> Outcome<Void> {
        switch await storage.list(prefix: photoDir(spaceId)) {
        case .fail(let reason):
            return .fail(reason)
        case .ok(let items):
            photosBySpace[spaceId.value] = items.compactMap { item in
                guard let parsed = PhotoObjectName.parse(item.name) else { return nil }
                return Photo(
                    id: parsed.id, regionCode: parsed.regionCode, takenOn: parsed.takenOn,
                    storagePath: item.fullPath,
                    downloadURL: storage.downloadURL(item.fullPath),
                    uploadedBy: uploaderUid,
                    uploadedAt: parsed.stableOrder
                )
            }
            await loadCovers(spaceId)
            return .ok(())
        }
    }

    public func upload(spaceId: SpaceId, photos: [NewPhoto]) async -> Outcome<[Photo]> {
        var saved: [Photo] = []
        for draft in photos {
            let id = PhotoId(String(newId().prefix(16)))
            let name = PhotoObjectName.build(id: id, regionCode: draft.regionCode, takenOn: draft.takenOn)
            let path = photoDir(spaceId) + name

            switch await storage.upload(path: path, data: draft.data, contentType: "image/jpeg") {
            case .fail(let reason):
                return .fail(reason)
            case .ok:
                saved.append(Photo(
                    id: id, regionCode: draft.regionCode, takenOn: draft.takenOn,
                    storagePath: path, downloadURL: storage.downloadURL(path),
                    uploadedBy: uploaderUid, uploadedAt: Int(Date().timeIntervalSince1970)
                ))
            }
        }
        photosBySpace[spaceId.value, default: []].append(contentsOf: saved)
        return .ok(saved)
    }

    public func delete(spaceId: SpaceId, id: PhotoId) async -> Outcome<Void> {
        guard let target = photosBySpace[spaceId.value]?.first(where: { $0.id == id }) else {
            return .fail(.notFound)
        }
        switch await storage.delete(path: target.storagePath) {
        case .fail(let reason):
            return .fail(reason)
        case .ok:
            photosBySpace[spaceId.value]?.removeAll { $0.id == id }
            return .ok(())
        }
    }

    public func setCover(spaceId: SpaceId, key: CoverKey, id: PhotoId) async -> Outcome<Void> {
        var next = (coversBySpace[spaceId.value] ?? []).filter { $0.key.documentId != key.documentId }
        next.append(Cover(key: key, photoId: id))

        guard let body = CoversFile.data(next) else { return .fail(.unknown) }

        switch await storage.upload(path: coversPath(spaceId), data: body, contentType: "application/json") {
        case .fail(let reason):
            return .fail(reason)
        case .ok:
            coversBySpace[spaceId.value] = next
            return .ok(())
        }
    }

    private func loadCovers(_ spaceId: SpaceId) async {
        guard case .ok(let data) = await storage.download(path: coversPath(spaceId)) else {
            coversBySpace[spaceId.value] = []   // 아직 대표를 한 번도 안 정한 공간
            return
        }
        coversBySpace[spaceId.value] = CoversFile.parse(data)
    }

    private func photoDir(_ spaceId: SpaceId) -> String { "spaces/\(spaceId.value)/photos/" }
    private func coversPath(_ spaceId: SpaceId) -> String { "spaces/\(spaceId.value)/covers.json" }
}
