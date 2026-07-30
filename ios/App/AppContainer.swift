import CoreModel
import CoreNetwork
import DataPhoto
import DataRegion
import DataSpace
import Domain
import FeatureCalendar
import FeatureSpace
import Foundation

/**
 조립하는 곳. 안드로이드 `AppContainer` 와 같은 역할이고, 마찬가지로 **손으로** 조립합니다.
 (DI 라이브러리를 넣을 만큼 그래프가 크지 않습니다.)

 저장소는 한 벌만 만들어 돌려씁니다 — `FirebasePhotoRepository` 는 받아 둔 사진을
 자기 안에 들고 있어서, 화면마다 새로 만들면 매번 다시 받아오게 됩니다.
 */
@MainActor
final class AppContainer {
    static let shared = AppContainer()

    /// 웹(`assets/firebase.js`)·안드로이드와 **같은 버킷**이어야 셋이 같은 사진을 봅니다.
    private let storage = FirebaseStorage(bucket: "our-surprise.firebasestorage.app")

    private let spaces: SharedSpaceRepository
    private let photos: FirebasePhotoRepository
    private let regions = AssetRegionCatalog()

    private init() {
        spaces = SharedSpaceRepository(storage: storage)
        photos = FirebasePhotoRepository(storage: storage, uploaderUid: DeviceIdentity.uid)
    }

    func spaceListStore() -> SpaceListStore {
        SpaceListStore(
            observeSpaces: ObserveSpaces(spaces: spaces),
            refreshSpaces: RefreshSpaces(spaces: spaces),
            createSpace: CreateSpace(spaces: spaces),
            joinSpace: JoinSpace(spaces: spaces)
        )
    }

    func calendarStore(_ spaceId: SpaceId) -> CalendarStore {
        CalendarStore(
            spaceId: spaceId,
            today: .today,
            observeBoard: ObservePhotoBoard(photos: photos),
            refreshPhotos: RefreshPhotos(photos: photos),
            setCoverPhoto: SetCoverPhoto(photos: photos),
            catalog: regions
        )
    }
}

extension CalendarDate {
    /// 기기의 달력 기준 오늘. `Date` 를 화면까지 들고 가지 않으려고 여기서 끊습니다.
    static var today: CalendarDate {
        let parts = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        return CalendarDate(
            year: parts.year ?? 2026,
            month: parts.month ?? 1,
            day: parts.day ?? 1
        )
    }
}
