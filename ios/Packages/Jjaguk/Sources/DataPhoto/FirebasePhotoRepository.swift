import CoreCommon
import CoreModel
import CoreNetwork
import Domain
import Foundation

/// **같이 쓰는 짜국**의 사진. 파일은 Storage 에, **지역·날짜는 Firestore 문서**에 둡니다.
///
/// ```
/// spaces/{짜국ID}/photos/{사진ID}.jpg        파일 (Storage)
/// spaces/{짜국ID}/photos/{사진ID}            지역·날짜·올린 사람 (Firestore)
/// spaces/{짜국ID}/covers/{대표키}            대표사진 (Firestore)
/// ```
///
/// 전에는 지역·날짜를 **파일 이름에** 적었습니다(`PhotoObjectName`). 로그인도 Firestore 도
/// 없어서 사진 정보를 둘 곳이 없었기 때문입니다. 이제 문서가 있으니 이름은 ID 하나로
/// 짧아졌습니다 (`docs/app/AUTH.md`).
///
/// **혼자 쓰는 짜국은 여전히 파일 이름 방식**입니다 (`LocalPhotoRepository`) —
/// 기기 안에는 Firestore 가 없으니까요.
public actor FirebasePhotoRepository: PhotoRepository {

    private let storage: FirebaseStorage
    private let firestore: Firestore
    private let accounts: any AuthRepository
    private let now: @Sendable () -> Int
    private let newId: @Sendable () -> String

    private var photosBySpace: [String: [Photo]] = [:]
    private var coversBySpace: [String: [Cover]] = [:]

    public init(
        storage: FirebaseStorage,
        firestore: Firestore,
        accounts: any AuthRepository,
        now: @escaping @Sendable () -> Int = { Int(Date().timeIntervalSince1970) },
        newId: @escaping @Sendable () -> String = {
            String(UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased().prefix(16))
        }
    ) {
        self.storage = storage
        self.firestore = firestore
        self.accounts = accounts
        self.now = now
        self.newId = newId
    }

    public func board(for spaceId: SpaceId) async -> PhotoBoard {
        PhotoBoard(
            photos: photosBySpace[spaceId.value] ?? [],
            covers: coversBySpace[spaceId.value] ?? []
        )
    }

    public func refresh(spaceId: SpaceId) async -> Outcome<Void> {
        guard PathSafe.isSafe(spaceId.value) else { return .fail(.unknown) }

        switch await firestore.list(photoCollection(spaceId)) {
        case .fail(let reason):
            return .fail(reason)
        case .ok(let documents):
            photosBySpace[spaceId.value] = documents.compactMap { document in
                // 날짜가 없으면 달력에 놓을 자리가 없습니다.
                guard let iso = document.text("takenOn"),
                      let takenOn = CalendarDate(iso: iso),
                      let path = document.text("storagePath")
                else { return nil }

                return Photo(
                    id: PhotoId(document.id),
                    regionCode: RegionCode(document.text("regionCode") ?? ""),
                    takenOn: takenOn,
                    storagePath: path,
                    downloadURL: storage.downloadURL(path),
                    uploadedBy: document.text("uploadedBy") ?? "",
                    // 이제 **올린 시각이 문서에 있습니다.** 목록만 보고 흉내 내던 값
                    // (`stableOrder`)은 혼자 짜국에만 남았습니다.
                    uploadedAt: document.number("uploadedAt") ?? 0
                )
            }
            await loadCovers(spaceId)
            return .ok(())
        }
    }

    public func upload(spaceId: SpaceId, photos: [NewPhoto]) async -> Outcome<[Photo]> {
        guard PathSafe.isSafe(spaceId.value) else { return .fail(.unknown) }
        guard let uid = await accounts.account()?.uid else { return .fail(.denied) }

        var saved: [Photo] = []
        for draft in photos {
            let id = PhotoId(newId())
            let path = photoDir(spaceId) + id.value + ".jpg"

            // 파일을 **먼저** 올립니다. 문서만 남고 파일이 없으면 빈 칸이 보이는데,
            // 반대(파일만 있고 문서가 없음)는 목록에 안 나올 뿐이라 덜 나쁩니다.
            if case .fail(let reason) = await storage.upload(
                path: path, data: draft.data, contentType: "image/jpeg"
            ) { return .fail(reason) }

            let uploadedAt = now()
            let fields: [String: Firestore.Value] = [
                "regionCode": .text(draft.regionCode.value),
                "takenOn": .text(draft.takenOn.iso),
                "storagePath": .text(path),
                "uploadedBy": .text(uid),
                "uploadedAt": .number(uploadedAt),
            ]
            if case .fail(let reason) = await firestore.set(
                "\(photoCollection(spaceId))/\(id.value)", fields: fields
            ) { return .fail(reason) }

            saved.append(Photo(
                id: id, regionCode: draft.regionCode, takenOn: draft.takenOn,
                storagePath: path, downloadURL: storage.downloadURL(path),
                uploadedBy: uid, uploadedAt: uploadedAt
            ))
        }

        photosBySpace[spaceId.value, default: []].append(contentsOf: saved)
        return .ok(saved)
    }

    public func delete(spaceId: SpaceId, id: PhotoId) async -> Outcome<Void> {
        guard let target = photosBySpace[spaceId.value]?.first(where: { $0.id == id }) else {
            return .fail(.notFound)
        }

        // 문서를 **먼저** 지웁니다. 파일만 남는 것은 눈에 안 보이지만, 문서만 남으면
        // 목록에 빈 칸이 생깁니다.
        if case .fail(let reason) = await firestore.delete(
            "\(photoCollection(spaceId))/\(id.value)"
        ) { return .fail(reason) }
        _ = await storage.delete(path: target.storagePath)

        photosBySpace[spaceId.value]?.removeAll { $0.id == id }
        return .ok(())
    }

    public func setCover(spaceId: SpaceId, key: CoverKey, id: PhotoId) async -> Outcome<Void> {
        switch await firestore.set(
            "\(coverCollection(spaceId))/\(key.documentId)", fields: ["photoId": .text(id.value)]
        ) {
        case .fail(let reason):
            return .fail(reason)
        case .ok:
            var next = (coversBySpace[spaceId.value] ?? []).filter { $0.key.documentId != key.documentId }
            next.append(Cover(key: key, photoId: id))
            coversBySpace[spaceId.value] = next
            return .ok(())
        }
    }

    private func loadCovers(_ spaceId: SpaceId) async {
        guard case .ok(let documents) = await firestore.list(coverCollection(spaceId)) else {
            coversBySpace[spaceId.value] = []   // 아직 대표를 한 번도 안 정한 짜국
            return
        }
        coversBySpace[spaceId.value] = documents.compactMap { document in
            guard let photoId = document.text("photoId"),
                  let key = CoverKey(documentId: document.id)
            else { return nil }
            return Cover(key: key, photoId: PhotoId(photoId))
        }
    }

    private func photoDir(_ spaceId: SpaceId) -> String { "spaces/\(spaceId.value)/photos/" }
    private func photoCollection(_ spaceId: SpaceId) -> String { "spaces/\(spaceId.value)/photos" }
    private func coverCollection(_ spaceId: SpaceId) -> String { "spaces/\(spaceId.value)/covers" }
}
