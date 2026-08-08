import CoreModel
import CoreNetwork
import FirebaseAnalytics
import FirebaseAppCheck
import FirebaseCrashlytics
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

    /// 이 빌드가 붙는 서버 한 벌. `GoogleService-Info.plist` 의 값들이고 **비밀이
    /// 아닙니다** — 실제 보안은 규칙이 합니다. 파일을 읽지 않고 여기 적어 두는 이유는
    /// 조립하는 곳 한 군데만 보면 이 앱이 어디에 붙는지 알 수 있게 하려는 것입니다.
    ///
    /// dev 와 prod 는 **빌드 구성이 고릅니다** (`Jjaguk-Dev` 스킴 = `DEV` 플래그,
    /// 안드로이드의 flavor 와 같은 자리). 지금은 dev 전용 Firebase 프로젝트가 아직
    /// 없어 둘이 같은 값입니다 — dev 프로젝트를 만들면 `dev` 의 네 값만 바꾸면 됩니다.
    ///
    /// dev 번들(`kr.jjaguk.app.dev`)도 같은 프로젝트에 등록돼 있습니다(2026-08-08) —
    /// 클라이언트 ID 만 번들마다 달라서 dev 는 제 것을 씁니다.
    private struct FirebaseEnv {
        let projectId: String
        let bucket: String
        let apiKey: String
        /// iOS 는 **자기 클라이언트 ID** 를 씁니다. 안드로이드가 web 클라이언트를
        /// 쓰는 것과 다릅니다 — 자주 헷갈리는 곳입니다.
        let googleClientID: String
        /// Cloud Functions 가 사는 곳. 함수는 `joinSpace` 하나입니다 (`functions/index.js`).
        let functionsOrigin: String

        static let prod = FirebaseEnv(
            projectId: "our-surprise",
            bucket: "our-surprise.firebasestorage.app",
            apiKey: "AIzaSyBLC3qqFukg__VivJe2HkN23UI_X94ENEc",
            googleClientID: "419812459548-nsun9ha7faersg7hlp0gmp27gpj6em8j.apps.googleusercontent.com",
            functionsOrigin: "https://asia-northeast3-our-surprise.cloudfunctions.net"
        )

        /// 서버는 prod 와 같고 **클라이언트 ID 만 dev 번들 것**입니다.
        /// dev 전용 Firebase 프로젝트가 생기면 네 값을 다 바꾼다.
        static let dev = FirebaseEnv(
            projectId: prod.projectId,
            bucket: prod.bucket,
            apiKey: prod.apiKey,
            googleClientID: "419812459548-5o4p7j8m1i6farj1m7klm61sfh8viaud.apps.googleusercontent.com",
            functionsOrigin: prod.functionsOrigin
        )

        static var current: FirebaseEnv {
            #if DEV
            dev
            #else
            prod
            #endif
        }
    }

    private let env = FirebaseEnv.current

    var googleClientID: String { env.googleClientID }

    /**
     스토어들이 받는 기록 클로저 — Analytics(GA4)로 흘러갑니다. Firebase 는
     여기서만 압니다. 이벤트 이름이 `_failed` 로 끝나면 Crashlytics 에도
     비치명으로 남깁니다 — 조용히 지나친 실패를 대시보드에서 셀 수 있어야 합니다.
     안드로이드 `FirebaseTracker` 와 같은 규칙입니다.
     */
    nonisolated static let track: @Sendable (String, [String: String]) -> Void = { event, params in
        Analytics.logEvent(event, parameters: params)
        if event.hasSuffix("_failed") {
            Crashlytics.crashlytics().log("\(event) \(params)")
            Crashlytics.crashlytics().record(
                error: NSError(domain: event, code: 0, userInfo: params)
            )
        }
    }

    private let spaces: SharedSpaceRepository
    private let regions = AssetRegionCatalog()
    let accounts: FirebaseAuthRepository

    /// 알림 받을 기기 등록. FCM 토큰이 나올 때마다 `PushDelegate` 가 부릅니다.
    let pushTokens: PushTokens

    /// 사진 저장소가 **둘**입니다. 혼자 짜국은 기기 안, 같이 쓰는 짜국은 서버 —
    /// 어느 쪽을 쓸지는 **여기서만** 정합니다. 화면과 도메인은 어느 쪽인지 모릅니다.
    private let remotePhotos: FirebasePhotoRepository
    private let localPhotos: LocalPhotoRepository

    private init() {
        let env = FirebaseEnv.current
        let accounts = FirebaseAuthRepository(auth: FirebaseAuth(apiKey: env.apiKey))
        self.accounts = accounts

        // App Check 토큰. "진짜 우리 앱인가"의 증명이라 모든 REST 요청에 얹습니다.
        // **못 받으면 `nil`** — 증명이 없다고 데이터 길이 막히면 안 됩니다
        // (강제는 콘솔에서 따로 켭니다. 그전까지는 지표만 쌓입니다).
        let appCheckToken: @Sendable () async -> String? = {
            (try? await AppCheck.appCheck().token(forcingRefresh: false))?.token
        }

        storage = FirebaseStorage(
            bucket: env.bucket,
            token: { await accounts.idToken() },
            appCheck: appCheckToken
        )
        // 멤버 판정이 사는 곳. 규칙이 여기를 봅니다 (`firestore.rules`).
        let firestore = Firestore(
            projectId: env.projectId,
            token: { await accounts.idToken() },
            appCheck: appCheckToken
        )
        let functions = Functions(
            origin: env.functionsOrigin,
            token: { await accounts.idToken() },
            appCheck: appCheckToken
        )
        spaces = SharedSpaceRepository(firestore: firestore, functions: functions, accounts: accounts)
        pushTokens = PushTokens(firestore: firestore, accounts: accounts)
        remotePhotos = FirebasePhotoRepository(
            storage: storage, firestore: firestore, accounts: accounts
        )
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
            },
            presentAppleSignIn: {
                switch await AppleSignInBridge.payload() {
                case .payload(let value): return value
                case .cancelled, .failed: return nil
                }
            },
            track: Self.track
        )
    }

    func spaceMenuStore(_ spaceId: SpaceId) -> SpaceMenuStore {
        SpaceMenuStore(
            spaceId: spaceId,
            observeSpaces: ObserveSpaces(spaces: spaces),
            newInvite: NewInvite(spaces: spaces),
            renameSpace: RenameSpace(spaces: spaces)
        )
    }

    func mapStore(_ spaceId: SpaceId, _ kind: SpaceKind) -> MapStore {
        let photos = photoRepository(kind)
        return MapStore(
            spaceId: spaceId,
            observeBoard: ObservePhotoBoard(photos: photos),
            searchRegions: SearchRegions(catalog: regions),
            setCoverPhoto: SetCoverPhoto(photos: photos),
            catalog: regions,
            track: Self.trackWith(kind)
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
            today: .today,
            track: Self.trackWith(kind)
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
            catalog: regions,
            track: Self.trackWith(kind)
        )
    }

    /// 짜국 종류를 아는 팩토리는 기록마다 `kind` 를 자동으로 얹는다 — 안드로이드와 같은 규칙.
    nonisolated private static func trackWith(_ kind: SpaceKind) -> @Sendable (String, [String: String]) -> Void {
        { event, params in
            var merged = params
            merged["kind"] = kind == .personal ? "Personal" : "Shared"
            track(event, merged)
        }
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
