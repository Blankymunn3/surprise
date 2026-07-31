import CoreCommon
import CoreModel
import Domain
import Foundation

/// **혼자 쓰는 짜국**의 사진. 앱 폴더에만 두고 서버를 아예 안 씁니다.
///
/// ```
/// Application Support/spaces/<짜국ID>/photos/2026-03-05_11140_a1b2c3.jpg
/// Application Support/spaces/<짜국ID>/covers.json
/// ```
///
/// `FirebasePhotoRepository` 와 **파일 이름·폴더 모양이 같습니다.** 나중에 '같이' 로 바꿀 때
/// 이 폴더를 그대로 올리면 되도록 하려는 것입니다 (`docs/app/AUTH.md`).
/// 안드로이드 `LocalPhotoRepository` 와 같은 구조입니다.
public actor LocalPhotoRepository: PhotoRepository {

    private let uploaderUid: String
    private let newId: @Sendable () -> String
    private let root: URL

    private var photosBySpace: [String: [Photo]] = [:]
    private var coversBySpace: [String: [Cover]] = [:]

    public init(
        uploaderUid: String,
        newId: @escaping @Sendable () -> String = { UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased() }
    ) {
        self.uploaderUid = uploaderUid
        self.newId = newId
        // Application Support 는 처음엔 없을 수 있습니다. 쓸 때 만듭니다.
        let base = FileManager.default
            .urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        self.root = base.appendingPathComponent("spaces", isDirectory: true)
    }

    public func board(for spaceId: SpaceId) async -> PhotoBoard {
        PhotoBoard(
            photos: photosBySpace[spaceId.value] ?? [],
            covers: coversBySpace[spaceId.value] ?? []
        )
    }

    public func refresh(spaceId: SpaceId) async -> Outcome<Void> {
        guard PathSafe.isSafe(spaceId.value) else { return .fail(.unknown) }

        // 폴더가 없는 것은 실패가 아닙니다 — 아직 한 장도 안 넣은 짜국입니다.
        let dir = photoDir(spaceId)
        let names = (try? FileManager.default.contentsOfDirectory(atPath: dir.path)) ?? []

        photosBySpace[spaceId.value] = names.compactMap { name in
            guard let parsed = PhotoObjectName.parse(name) else { return nil }
            let file = dir.appendingPathComponent(name)
            return Photo(
                id: parsed.id, regionCode: parsed.regionCode, takenOn: parsed.takenOn,
                storagePath: file.path, downloadURL: file.absoluteString,
                uploadedBy: uploaderUid, uploadedAt: parsed.stableOrder
            )
        }
        loadCovers(spaceId)
        return .ok(())
    }

    public func upload(spaceId: SpaceId, photos: [NewPhoto]) async -> Outcome<[Photo]> {
        guard PathSafe.isSafe(spaceId.value) else { return .fail(.unknown) }
        let dir = photoDir(spaceId)
        guard makeDirectory(dir) else { return .fail(.unknown) }

        var saved: [Photo] = []
        for draft in photos {
            let id = PhotoId(String(newId().prefix(16)))
            let name = PhotoObjectName.build(id: id, regionCode: draft.regionCode, takenOn: draft.takenOn)
            let file = dir.appendingPathComponent(name)

            do {
                try draft.data.write(to: file, options: .atomic)
            } catch {
                // 여기까지 쓴 것은 지우지 않습니다. 이미 들어간 사진을 되돌리면
                // 사용자가 고른 것 중 무엇이 남았는지 알 수 없게 됩니다.
                return .fail(.unknown)
            }

            saved.append(Photo(
                id: id, regionCode: draft.regionCode, takenOn: draft.takenOn,
                storagePath: file.path, downloadURL: file.absoluteString,
                uploadedBy: uploaderUid, uploadedAt: Int(Date().timeIntervalSince1970)
            ))
        }

        photosBySpace[spaceId.value, default: []].append(contentsOf: saved)
        return .ok(saved)
    }

    public func delete(spaceId: SpaceId, id: PhotoId) async -> Outcome<Void> {
        guard let target = photosBySpace[spaceId.value]?.first(where: { $0.id == id }) else {
            return .fail(.notFound)
        }

        let file = URL(fileURLWithPath: target.storagePath)
        // 이미 없는 파일은 지운 것으로 봅니다 — 목록에서 사라지는 게 사용자가 원한 결과입니다.
        if FileManager.default.fileExists(atPath: file.path) {
            guard (try? FileManager.default.removeItem(at: file)) != nil else { return .fail(.unknown) }
        }

        photosBySpace[spaceId.value]?.removeAll { $0.id == id }
        return .ok(())
    }

    public func setCover(spaceId: SpaceId, key: CoverKey, id: PhotoId) async -> Outcome<Void> {
        guard PathSafe.isSafe(spaceId.value) else { return .fail(.unknown) }

        var next = (coversBySpace[spaceId.value] ?? []).filter { $0.key.documentId != key.documentId }
        next.append(Cover(key: key, photoId: id))

        guard makeDirectory(spaceDir(spaceId)), let body = CoversFile.data(next) else {
            return .fail(.unknown)
        }
        guard (try? body.write(to: coversFile(spaceId), options: .atomic)) != nil else {
            return .fail(.unknown)
        }

        coversBySpace[spaceId.value] = next
        return .ok(())
    }

    private func loadCovers(_ spaceId: SpaceId) {
        guard let data = try? Data(contentsOf: coversFile(spaceId)) else {
            coversBySpace[spaceId.value] = []   // 아직 대표를 한 번도 안 정한 짜국
            return
        }
        coversBySpace[spaceId.value] = CoversFile.parse(data)
    }

    private func makeDirectory(_ url: URL) -> Bool {
        (try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)) != nil
    }

    private func spaceDir(_ spaceId: SpaceId) -> URL {
        root.appendingPathComponent(spaceId.value, isDirectory: true)
    }

    private func photoDir(_ spaceId: SpaceId) -> URL {
        spaceDir(spaceId).appendingPathComponent("photos", isDirectory: true)
    }

    private func coversFile(_ spaceId: SpaceId) -> URL {
        spaceDir(spaceId).appendingPathComponent("covers.json")
    }
}
