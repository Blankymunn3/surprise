import Foundation

public struct Space: Hashable, Sendable, Identifiable {
    public let spaceId: SpaceId
    public var name: String
    public var members: [Member]
    public var photoCount: Int
    public var regionCount: Int
    public var coverPhotoURL: String?
    public var lastPhotoOn: CalendarDate?

    public var id: String { spaceId.value }

    public init(
        spaceId: SpaceId, name: String, members: [Member],
        photoCount: Int = 0, regionCount: Int = 0,
        coverPhotoURL: String? = nil, lastPhotoOn: CalendarDate? = nil
    ) {
        self.spaceId = spaceId
        self.name = name
        self.members = members
        self.photoCount = photoCount
        self.regionCount = regionCount
        self.coverPhotoURL = coverPhotoURL
        self.lastPhotoOn = lastPhotoOn
    }
}

public struct Member: Hashable, Sendable, Identifiable {
    public let uid: String
    public let displayName: String
    public let role: MemberRole

    public var id: String { uid }

    public init(uid: String, displayName: String, role: MemberRole) {
        self.uid = uid
        self.displayName = displayName
        self.role = role
    }

    /// 프로필 사진 대신 쓰는 이름 첫 글자.
    public var initial: String {
        displayName.isEmpty ? "?" : String(displayName.prefix(1))
    }
}

public enum MemberRole: Sendable { case owner, member }

public struct Invite: Hashable, Sendable {
    public let code: String
    public let spaceId: SpaceId
    public init(code: String, spaceId: SpaceId) {
        self.code = code
        self.spaceId = spaceId
    }
}
