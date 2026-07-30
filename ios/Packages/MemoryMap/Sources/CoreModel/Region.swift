import Foundation

/// 지역 코드. **웹·안드로이드와 반드시 같아야 합니다** —
/// 한쪽에서 넣은 사진이 다른 쪽에서도 보여야 하니까요. (`docs/app/CONVENTIONS.md`)
public struct RegionCode: Hashable, Sendable, CustomStringConvertible {
    public let value: String
    public init(_ value: String) { self.value = value }

    public var kind: RegionKind {
        if value.hasPrefix("C-") { return .country }
        if value.hasPrefix("P-") { return .subdivision }
        if !value.isEmpty && value.allSatisfy(\.isNumber) { return .koreanDistrict }
        return .place
    }

    /// `P-JPN-12` → `JPN`. 검색 결과에 나라 이름을 붙일 때 씁니다.
    public var countryISO3: String? {
        switch kind {
        case .country:
            return String(value.dropFirst(2))
        case .subdivision:
            let rest = value.dropFirst(2)
            return rest.split(separator: "-").first.map(String.init)
        default:
            return nil
        }
    }

    public var description: String { value }
}

public enum RegionKind: Sendable { case koreanDistrict, country, subdivision, place }

public struct Region: Hashable, Sendable, Identifiable {
    public let code: RegionCode
    public let name: String
    public let parentName: String?

    public var id: String { code.value }

    public init(code: RegionCode, name: String, parentName: String?) {
        self.code = code
        self.name = name
        self.parentName = parentName
    }

    /// "중구" 만으로는 어디인지 모르기 때문에 상위 이름을 붙입니다.
    public var displayName: String {
        guard let parentName, !parentName.isEmpty else { return name }
        return "\(parentName) \(name)"
    }
}
