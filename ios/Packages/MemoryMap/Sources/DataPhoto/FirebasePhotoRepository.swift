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
                    // 목록 API 가 올린 시각을 주지 않습니다. 대표사진 기본값("가장 최근")이
                    // 실행할 때마다 흔들리면 안 되므로 **결정적인** 값을 만들어 씁니다.
                    // Swift 의 hashValue 는 실행마다 씨앗이 달라 쓸 수 없습니다.
                    uploadedAt: Self.stableOrder(parsed)
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

        let dictionary = Dictionary(next.map { ($0.key.documentId, $0.photoId.value) },
                                    uniquingKeysWith: { _, last in last })
        guard let body = try? JSONSerialization.data(withJSONObject: dictionary) else {
            return .fail(.unknown)
        }

        switch await storage.upload(path: coversPath(spaceId), data: body, contentType: "application/json") {
        case .fail(let reason):
            return .fail(reason)
        case .ok:
            coversBySpace[spaceId.value] = next
            return .ok(())
        }
    }

    private func loadCovers(_ spaceId: SpaceId) async {
        guard case .ok(let data) = await storage.download(path: coversPath(spaceId)),
              let raw = try? JSONSerialization.jsonObject(with: data) as? [String: String]
        else {
            coversBySpace[spaceId.value] = []   // 아직 대표를 한 번도 안 정한 공간
            return
        }
        coversBySpace[spaceId.value] = raw.compactMap { documentId, photoId in
            guard let key = Self.key(from: documentId) else { return nil }
            return Cover(key: key, photoId: PhotoId(photoId))
        }
    }

    /// 찍은 날짜가 먼저, 같은 날이면 사진 ID 순. 안드로이드와 같은 규칙입니다.
    static func stableOrder(_ parsed: PhotoObjectName.Parsed) -> Int {
        let day = parsed.takenOn.year * 10_000 + parsed.takenOn.month * 100 + parsed.takenOn.day
        let tail = parsed.id.value.unicodeScalars.reduce(0) { ($0 &* 31 &+ Int($1.value)) % 9_973 }
        return day * 10_000 + tail
    }

    static func key(from documentId: String) -> CoverKey? {
        if documentId.hasPrefix("region_") {
            return .region(RegionCode(String(documentId.dropFirst(7))))
        }
        if documentId.hasPrefix("day_"), let date = CalendarDate(iso: String(documentId.dropFirst(4))) {
            return .day(date)
        }
        return nil
    }

    private func photoDir(_ spaceId: SpaceId) -> String { "spaces/\(spaceId.value)/photos/" }
    private func coversPath(_ spaceId: SpaceId) -> String { "spaces/\(spaceId.value)/covers.json" }
}
