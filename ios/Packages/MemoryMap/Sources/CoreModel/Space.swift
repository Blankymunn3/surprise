import Foundation

/// 짜국을 **혼자** 쓰는지 **둘이** 쓰는지.
///
/// 혼자 쓰는 짜국은 사진이 기기 안에만 있습니다. 서버도, 로그인도 안 씁니다.
/// (`docs/app/AUTH.md` 의 '혼자 쓰는 짜국은 서버에 안 올립니다')
public enum SpaceKind: Sendable, Hashable { case personal, shared }

public struct Space: Hashable, Sendable, Identifiable {
    public let spaceId: SpaceId
    public var name: String
    public var members: [Member]
    public var photoCount: Int
    public var regionCount: Int
    public var coverPhotoURL: String?
    public var lastPhotoOn: CalendarDate?
    /// 기본이 `.personal` 인 이유: 이 값이 없는 옛 데이터를 읽었을 때 **서버로 나가지
    /// 않는 쪽**이 안전합니다. 반대로 두면 옛 짜국이 조용히 공유로 취급됩니다.
    public var kind: SpaceKind

    public var id: String { spaceId.value }

    public init(
        spaceId: SpaceId, name: String, members: [Member],
        photoCount: Int = 0, regionCount: Int = 0,
        coverPhotoURL: String? = nil, lastPhotoOn: CalendarDate? = nil,
        kind: SpaceKind = .personal
    ) {
        self.spaceId = spaceId
        self.name = name
        self.members = members
        self.photoCount = photoCount
        self.regionCount = regionCount
        self.coverPhotoURL = coverPhotoURL
        self.lastPhotoOn = lastPhotoOn
        self.kind = kind
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
