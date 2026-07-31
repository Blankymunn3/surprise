import CoreModel
import CoreNetwork
import DataPhoto
import DataRegion
import DataSpace
import Domain
import FeatureCalendar
import FeatureMap
import FeatureSpace
import FeatureUpload
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
    private let regions = AssetRegionCatalog()

    /// 사진 저장소가 **둘**입니다. 혼자 짜국은 기기 안, 둘이 짜국은 서버 —
    /// 어느 쪽을 쓸지는 **여기서만** 정합니다. 화면과 도메인은 어느 쪽인지 모릅니다.
    private let remotePhotos: FirebasePhotoRepository
    private let localPhotos: LocalPhotoRepository

    private init() {
        spaces = SharedSpaceRepository(storage: storage)
        remotePhotos = FirebasePhotoRepository(storage: storage, uploaderUid: DeviceIdentity.uid)
        localPhotos = LocalPhotoRepository(uploaderUid: DeviceIdentity.uid)
    }

    private func photoRepository(_ kind: SpaceKind) -> any PhotoRepository {
        kind == .personal ? localPhotos : remotePhotos
    }

    func spaceListStore() -> SpaceListStore {
        SpaceListStore(
            observeSpaces: ObserveSpaces(spaces: spaces),
            refreshSpaces: RefreshSpaces(spaces: spaces),
            createSpace: CreateSpace(spaces: spaces),
            joinSpace: JoinSpace(spaces: spaces)
        )
    }

    func mapStore(_ spaceId: SpaceId, _ kind: SpaceKind) -> MapStore {
        let photos = photoRepository(kind)
        return MapStore(
            spaceId: spaceId,
            observeBoard: ObservePhotoBoard(photos: photos),
            searchRegions: SearchRegions(catalog: regions),
            setCoverPhoto: SetCoverPhoto(photos: photos),
            catalog: regions
        )
    }

    func uploadStore(_ spaceId: SpaceId, _ kind: SpaceKind) -> UploadStore {
        let catalog = regions
        let photos = photoRepository(kind)
        return UploadStore(
            spaceId: spaceId,
            // EXIF 는 파일만 읽고(PhotoFile), 좌표를 지역으로 바꾸는 건 도메인(catalog)이 합니다.
            // 지도를 눌렀을 때와 **같은 길**이라 두 경로가 어긋날 수 없습니다.
            readHints: { path in
                let hint = PhotoFile.hint(at: path)
                var code: RegionCode?
                if let (lat, lon) = hint.coordinate {
                    code = await catalog.regionAt(latitude: lat, longitude: lon)?.code
                }
                return UploadPlan.ExifHint(takenOn: hint.takenOn, regionCode: code)
            },
            toJpeg: { path in PhotoFile.jpeg(at: path) },
            uploadPhotos: UploadPhotos(photos: photos),
            searchRegions: SearchRegions(catalog: catalog),
            catalog: catalog,
            today: .today
        )
    }

    /// 사진을 올린 뒤 지도·달력이 새 사진을 보게 합니다.
    func refreshPhotos(_ spaceId: SpaceId, _ kind: SpaceKind) async {
        _ = await RefreshPhotos(photos: photoRepository(kind))(spaceId)
    }

    func calendarStore(_ spaceId: SpaceId, _ kind: SpaceKind) -> CalendarStore {
        let photos = photoRepository(kind)
        return CalendarStore(
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
