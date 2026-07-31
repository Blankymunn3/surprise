import CoreModel
import Domain
import Foundation

private struct RegionRow: Decodable {
    let code: String
    let name: String
    let parent: String?
}

/// 지역 이름과 경계를 앱에 동봉한 파일에서 읽습니다.
/// **안드로이드·웹과 같은 데이터**라 코드가 같고, 그래서 서로의 사진이 보입니다.
public actor AssetRegionCatalog: RegionCatalog {

    private var regions: [Region]?
    private var shapesKorea: [GeoShape]?
    private var shapesWorld: [GeoShape]?

    public init() {}

    public func all() async -> [Region] {
        if let regions { return regions }
        let loaded = Self.loadRegions()
        regions = loaded
        return loaded
    }

    public func find(_ codeValue: String) async -> Region? {
        await all().first { $0.code.value == codeValue }
    }

    /// 국내 시군구를 먼저 보고 없으면 나라로 내려갑니다 —
    /// 한국에서 찍은 사진이 "대한민국" 이 아니라 "서울 중구" 로 붙어야 합니다.
    public func regionAt(latitude: Double, longitude: Double) async -> Region? {
        if let hit = koreaShapes().first(where: { $0.contains(lon: longitude, lat: latitude) }) {
            return await find(hit.code)
        }
        if let hit = worldShapes().first(where: { $0.contains(lon: longitude, lat: latitude) }) {
            return await find(hit.code)
        }
        return nil
    }

    public func shape(of code: RegionCode) async -> [[[GeoPoint]]] {
        let shapes = code.value.hasPrefix("C-") ? worldShapes() : koreaShapes()
        guard let shape = shapes.first(where: { $0.code == code.value }) else { return [] }
        // 저장된 순서는 GeoJSON 과 같은 (경도, 위도) 입니다. 여기서 한 번만 뒤집습니다.
        return shape.polygons.map { polygon in
            polygon.map { ring in
                ring.map { GeoPoint(latitude: $0.1, longitude: $0.0) }
            }
        }
    }

    public func center(of code: RegionCode) async -> (Double, Double)? {
        let shapes = code.value.hasPrefix("C-") ? worldShapes() : koreaShapes()
        guard let shape = shapes.first(where: { $0.code == code.value }) else { return nil }
        // 경계 상자의 가운데. 정확한 무게중심은 필요 없고 "그 지역 어딘가" 면 됩니다.
        return ((shape.minLat + shape.maxLat) / 2, (shape.minLon + shape.maxLon) / 2)
    }

    private func koreaShapes() -> [GeoShape] {
        if let shapesKorea { return shapesKorea }
        let loaded = Self.loadShapes("boundaries_kr")
        shapesKorea = loaded
        return loaded
    }

    private func worldShapes() -> [GeoShape] {
        if let shapesWorld { return shapesWorld }
        let loaded = Self.loadShapes("boundaries_world")
        shapesWorld = loaded
        return loaded
    }

    static func loadRegions() -> [Region] {
        guard let url = Bundle.module.url(forResource: "regions", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let rows = try? JSONDecoder().decode([RegionRow].self, from: data)
        else { return [] }
        return rows.map { Region(code: RegionCode($0.code), name: $0.name, parentName: $0.parent) }
    }

    static func loadShapes(_ resource: String) -> [GeoShape] {
        guard let url = Bundle.module.url(forResource: resource, withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let rows = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]]
        else { return [] }
        return rows.compactMap { GeoShape(row: $0) }
    }
}

/// 지역 하나의 경계
struct GeoShape {
    let code: String
    let polygons: [[[(Double, Double)]]]
    let minLon: Double, minLat: Double, maxLon: Double, maxLat: Double

    init?(row: [String: Any]) {
        guard let code = row["c"] as? String,
              let geometry = row["g"] as? [String: Any],
              let type = geometry["type"] as? String,
              let coordinates = geometry["coordinates"] as? [Any]
        else { return nil }

        let polygons: [[[(Double, Double)]]]
        switch type {
        case "Polygon":
            polygons = [Self.rings(coordinates)]
        case "MultiPolygon":
            polygons = coordinates.compactMap { ($0 as? [Any]).map(Self.rings) }
        default:
            return nil
        }
        guard !polygons.isEmpty else { return nil }

        var minLon = Double.greatestFiniteMagnitude, minLat = Double.greatestFiniteMagnitude
        var maxLon = -Double.greatestFiniteMagnitude, maxLat = -Double.greatestFiniteMagnitude
        for polygon in polygons {
            for ring in polygon {
                for (lon, lat) in ring {
                    minLon = min(minLon, lon); maxLon = max(maxLon, lon)
                    minLat = min(minLat, lat); maxLat = max(maxLat, lat)
                }
            }
        }

        self.code = code
        self.polygons = polygons
        self.minLon = minLon; self.minLat = minLat
        self.maxLon = maxLon; self.maxLat = maxLat
    }

    func contains(lon: Double, lat: Double) -> Bool {
        guard lon >= minLon, lon <= maxLon, lat >= minLat, lat <= maxLat else { return false }
        return polygons.contains { PointInPolygon.inPolygon(lon: lon, lat: lat, rings: $0) }
    }

    static func rings(_ polygon: [Any]) -> [[(Double, Double)]] {
        polygon.compactMap { ring in
            (ring as? [Any])?.compactMap { point -> (Double, Double)? in
                guard let xy = point as? [Any], xy.count >= 2,
                      let x = (xy[0] as? NSNumber)?.doubleValue,
                      let y = (xy[1] as? NSNumber)?.doubleValue
                else { return nil }
                return (x, y)
            }
        }
    }
}
