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
    // 화면의 글을 Localizable.strings 로 뺐습니다. 이 값이 있어야 SwiftPM 이
    // `Resources/ko.lproj` 를 번역 파일로 알아봅니다 — 없으면 그냥 파일로 복사만 합니다.
    defaultLocalization: "ko",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "CoreModel", targets: ["CoreModel"]),
        .library(name: "CoreCommon", targets: ["CoreCommon"]),
        // 앱 껍데기가 저장소(Repository)를 조립할 때 FirebaseStorage 가 필요합니다.
        .library(name: "CoreNetwork", targets: ["CoreNetwork"]),
        .library(name: "DesignSystem", targets: ["DesignSystem"]),
        .library(name: "Domain", targets: ["Domain"]),
        .library(name: "DataAuth", targets: ["DataAuth"]),
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
        .target(name: "DesignSystem", dependencies: ["CoreModel"], resources: [.process("Resources")]),
        .target(name: "Domain", dependencies: ["CoreModel", "CoreCommon"]),
        .target(name: "DataAuth", dependencies: ["CoreModel", "CoreCommon", "Domain", "CoreNetwork"]),
        .target(name: "DataPhoto", dependencies: ["CoreModel", "CoreCommon", "Domain", "CoreNetwork"]),
        .target(name: "DataSpace", dependencies: ["CoreModel", "CoreCommon", "Domain", "CoreNetwork"]),
        .target(name: "DataRegion", dependencies: ["CoreModel", "Domain"], resources: [.process("Resources")]),
        .target(name: "FeatureSpace", dependencies: ["CoreModel", "CoreCommon", "Domain", "DesignSystem"]),
        .target(name: "FeatureMap", dependencies: ["CoreModel", "CoreCommon", "Domain", "DesignSystem"]),
        .target(name: "FeatureCalendar", dependencies: ["CoreModel", "CoreCommon", "Domain", "DesignSystem"]),
        .target(name: "FeatureUpload", dependencies: ["CoreModel", "CoreCommon", "Domain", "DesignSystem"]),

        .testTarget(name: "CoreNetworkTests", dependencies: ["CoreNetwork"]),
        .testTarget(name: "DomainTests", dependencies: ["Domain", "CoreModel", "CoreCommon"]),
        .testTarget(name: "DataPhotoTests", dependencies: ["DataPhoto", "CoreModel"]),
        .testTarget(name: "DataRegionTests", dependencies: ["DataRegion", "CoreModel"]),
        .testTarget(name: "FeatureSpaceTests", dependencies: ["FeatureSpace", "CoreModel", "CoreCommon"]),
        .testTarget(name: "FeatureMapTests", dependencies: ["FeatureMap", "CoreModel"]),
        .testTarget(name: "FeatureUploadTests", dependencies: ["FeatureUpload", "CoreModel", "Domain"]),
    ]
)
