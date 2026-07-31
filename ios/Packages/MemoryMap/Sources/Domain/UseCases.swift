import CoreCommon
import CoreModel
import Foundation

public struct ObservePhotoBoard: Sendable {
    private let photos: any PhotoRepository
    public init(photos: any PhotoRepository) { self.photos = photos }
    public func callAsFunction(_ spaceId: SpaceId) async -> PhotoBoard {
        await photos.board(for: spaceId)
    }
}

public struct RefreshPhotos: Sendable {
    private let photos: any PhotoRepository
    public init(photos: any PhotoRepository) { self.photos = photos }
    public func callAsFunction(_ spaceId: SpaceId) async -> Outcome<Void> {
        await photos.refresh(spaceId: spaceId)
    }
}

public struct UploadPhotos: Sendable {
    private let photos: any PhotoRepository
    public init(photos: any PhotoRepository) { self.photos = photos }
    public func callAsFunction(_ spaceId: SpaceId, _ drafts: [NewPhoto]) async -> Outcome<[Photo]> {
        guard !drafts.isEmpty else { return .fail(.unknown) }
        return await photos.upload(spaceId: spaceId, photos: drafts)
    }
}

public struct SetCoverPhoto: Sendable {
    private let photos: any PhotoRepository
    public init(photos: any PhotoRepository) { self.photos = photos }
    public func callAsFunction(_ spaceId: SpaceId, _ key: CoverKey, _ id: PhotoId) async -> Outcome<Void> {
        await photos.setCover(spaceId: spaceId, key: key, id: id)
    }
}

public struct ObserveSpaces: Sendable {
    private let spaces: any SpaceRepository
    public init(spaces: any SpaceRepository) { self.spaces = spaces }
    public func callAsFunction() async -> [Space] { await spaces.spaces() }
}

/// 저장소에서 내 공간들을 **다시 받아옵니다.** `ObserveSpaces` 는 받아 둔 것을 읽을 뿐이라
/// 이걸 부르지 않으면 앱을 켰을 때 목록이 늘 비어 보입니다.
public struct RefreshSpaces: Sendable {
    private let spaces: any SpaceRepository
    public init(spaces: any SpaceRepository) { self.spaces = spaces }
    public func callAsFunction() async -> Outcome<Void> { await spaces.refresh() }
}

public struct CreateSpace: Sendable {
    private let spaces: any SpaceRepository
    public init(spaces: any SpaceRepository) { self.spaces = spaces }
    public func callAsFunction(_ name: String, _ kind: SpaceKind) async -> Outcome<(Space, Invite?)> {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return .fail(.unknown) }
        return await spaces.create(name: trimmed, kind: kind)
    }
}

public struct JoinSpace: Sendable {
    private let spaces: any SpaceRepository
    public init(spaces: any SpaceRepository) { self.spaces = spaces }
    public func callAsFunction(_ code: String) async -> Outcome<Space> {
        await spaces.join(code: code.trimmingCharacters(in: .whitespacesAndNewlines).uppercased())
    }
}

public struct SearchRegions: Sendable {
    private let catalog: any RegionCatalog
    public init(catalog: any RegionCatalog) { self.catalog = catalog }
    public func callAsFunction(_ query: String) async -> [Region] {
        RegionSearch.rank(query: query, regions: await catalog.all())
    }
}
