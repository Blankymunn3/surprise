import CoreModel
import CoreNetwork
import DataAuth
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
    /// 토큰을 **함수로** 넘기는 이유: 저장소가 만들어지는 시점에는 아직 로그인 전입니다.
    /// 요청할 때마다 물어봐야 그때의 토큰(필요하면 갱신된 것)이 실립니다.
    private let storage: FirebaseStorage

    /// `GoogleService-Info.plist` 에 들어 있는 값들입니다. **비밀이 아닙니다** — 실제 보안은
    /// 규칙이 합니다. 파일을 읽어 꺼내지 않고 여기 적어 두는 이유는 버킷과 같습니다:
    /// 조립하는 곳 한 군데만 보면 이 앱이 어디에 붙는지 알 수 있게 하려는 것입니다.
    private let apiKey = "AIzaSyBLC3qqFukg__VivJe2HkN23UI_X94ENEc"

    /// iOS 는 **자기 클라이언트 ID** 를 씁니다. 안드로이드가 web 클라이언트를 쓰는 것과
    /// 다릅니다 — 자주 헷갈리는 곳입니다.
    let googleClientID = "419812459548-4vruv826mfgfkfi3dppobg87c3du1vdr.apps.googleusercontent.com"

    private let spaces: SharedSpaceRepository
    private let regions = AssetRegionCatalog()
    let accounts: FirebaseAuthRepository

    /// 사진 저장소가 **둘**입니다. 혼자 짜국은 기기 안, 같이 쓰는 짜국은 서버 —
    /// 어느 쪽을 쓸지는 **여기서만** 정합니다. 화면과 도메인은 어느 쪽인지 모릅니다.
    private let remotePhotos: FirebasePhotoRepository
    private let localPhotos: LocalPhotoRepository

    private init() {
        let accounts = FirebaseAuthRepository(auth: FirebaseAuth(apiKey: apiKey))
        self.accounts = accounts
        storage = FirebaseStorage(
            bucket: "our-surprise.firebasestorage.app",
            token: { await accounts.idToken() }
        )
        spaces = SharedSpaceRepository(storage: storage)
        remotePhotos = FirebasePhotoRepository(storage: storage, uploaderUid: DeviceIdentity.uid)
        localPhotos = LocalPhotoRepository(uploaderUid: DeviceIdentity.uid)
    }

    private func photoRepository(_ kind: SpaceKind) -> any PhotoRepository {
        kind == .personal ? localPhotos : remotePhotos
    }

    func spaceListStore() -> SpaceListStore {
        let clientID = googleClientID
        return SpaceListStore(
            observeSpaces: ObserveSpaces(spaces: spaces),
            refreshSpaces: RefreshSpaces(spaces: spaces),
            createSpace: CreateSpace(spaces: spaces),
            joinSpace: JoinSpace(spaces: spaces),
            accounts: accounts,
            // 창을 띄우는 일만 껍데기가 합니다. 화면은 문자열 하나만 돌려받습니다.
            presentGoogleSignIn: {
                switch await GoogleSignInBridge.idToken(clientID: clientID) {
                case .token(let value): return value
                case .cancelled, .failed: return nil
                }
            }
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
