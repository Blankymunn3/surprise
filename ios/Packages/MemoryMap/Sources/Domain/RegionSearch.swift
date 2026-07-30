import CoreModel

/// 지역 검색 순서. 안드로이드 `RegionSearch` 와 같아야 두 앱이 같은 결과를 냅니다.
///
/// 1. 이름이 정확히 같은 것 → 2. 앞부분이 맞는 것 → 3. 이름에 들어 있는 것 → 4. 상위 이름에 들어 있는 것
/// 같은 등급이면 국내 시군구 → 나라 → 해외 시도 → 장소 순서입니다.
public enum RegionSearch {

    public static func rank(query: String, regions: [Region], limit: Int = 30) -> [Region] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return [] }

        let scored: [(Region, Int)] = regions.compactMap { region in
            guard let s = score(query: q, region: region) else { return nil }
            return (region, s)
        }

        return scored
            .sorted { lhs, rhs in
                if lhs.1 != rhs.1 { return lhs.1 < rhs.1 }
                let lk = kindOrder(lhs.0.code.kind), rk = kindOrder(rhs.0.code.kind)
                if lk != rk { return lk < rk }
                if lhs.0.name.count != rhs.0.name.count { return lhs.0.name.count < rhs.0.name.count }
                return lhs.0.displayName < rhs.0.displayName
            }
            .prefix(limit)
            .map(\.0)
    }

    static func score(query: String, region: Region) -> Int? {
        let name = region.name.lowercased()
        let q = query.lowercased()
        if name == q { return 0 }
        if name.hasPrefix(q) { return 1 }
        if name.contains(q) { return 2 }
        if let parent = region.parentName?.lowercased(), parent.contains(q) { return 3 }
        return nil
    }

    static func kindOrder(_ kind: RegionKind) -> Int {
        switch kind {
        case .koreanDistrict: return 0
        case .country: return 1
        case .subdivision: return 2
        case .place: return 3
        }
    }
}
