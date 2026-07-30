// swift-tools-version: 6.0
import PackageDescription

/**
 안드로이드와 **같은 층 구조, 같은 이름**을 씁니다.
 `data/photo` 를 고쳤으면 `DataPhoto` 도 봐야 한다는 걸 이름만 보고 알 수 있게 하려는 것입니다.

 macOS 도 플랫폼에 넣은 이유: CI 에서 `swift test` 를 돌리려면 macOS 로도 빌드돼야 합니다.
 그래서 UIKit 전용 API 를 이 패키지 안에서 쓰지 않습니다. 지도(MapLibre)처럼 iOS 전용인 것은
 앱 껍데기 쪽에 둡니다.
 */
let package = Package(
    name: "MemoryMap",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "CoreModel", targets: ["CoreModel"]),
        .library(name: "CoreCommon", targets: ["CoreCommon"]),
        .library(name: "DesignSystem", targets: ["DesignSystem"]),
        .library(name: "Domain", targets: ["Domain"]),
        .library(name: "DataPhoto", targets: ["DataPhoto"]),
        .library(name: "DataSpace", targets: ["DataSpace"]),
        .library(name: "DataRegion", targets: ["DataRegion"]),
        .library(name: "FeatureSpace", targets: ["FeatureSpace"]),
        .library(name: "FeatureMap", targets: ["FeatureMap"]),
        .library(name: "FeatureCalendar", targets: ["FeatureCalendar"]),
        .library(name: "FeatureUpload", targets: ["FeatureUpload"]),
    ],
    targets: [
        .target(name: "CoreModel"),
        .target(name: "CoreCommon"),
        .target(name: "CoreNetwork", dependencies: ["CoreCommon"]),
        .target(name: "DesignSystem", dependencies: ["CoreModel"]),
        .target(name: "Domain", dependencies: ["CoreModel", "CoreCommon"]),
        .target(name: "DataPhoto", dependencies: ["Domain", "CoreNetwork"]),
        .target(name: "DataSpace", dependencies: ["Domain", "CoreNetwork"]),
        .target(name: "DataRegion", dependencies: ["Domain"], resources: [.process("Resources")]),
        .target(name: "FeatureSpace", dependencies: ["Domain", "DesignSystem"]),
        .target(name: "FeatureMap", dependencies: ["Domain", "DesignSystem"]),
        .target(name: "FeatureCalendar", dependencies: ["Domain", "DesignSystem"]),
        .target(name: "FeatureUpload", dependencies: ["Domain", "DesignSystem"]),

        .testTarget(name: "DomainTests", dependencies: ["Domain"]),
        .testTarget(name: "DataPhotoTests", dependencies: ["DataPhoto"]),
        .testTarget(name: "DataRegionTests", dependencies: ["DataRegion"]),
        .testTarget(name: "FeatureSpaceTests", dependencies: ["FeatureSpace"]),
        .testTarget(name: "FeatureUploadTests", dependencies: ["FeatureUpload"]),
    ]
)
