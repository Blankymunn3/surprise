# 우리 추억 지도 — 앱으로 만들기

추억 지도(`map/`) 화면을 안드로이드·iOS 앱으로 감싼 것입니다. [Capacitor](https://capacitorjs.com) 를 씁니다.

앱 안에 지도 화면이 **들어 있어서**(웹사이트를 불러오는 방식이 아님) 앱을 새로 만들어야 최신 내용이 반영됩니다.
지도 타일과 사진은 그대로 인터넷에서 받아옵니다.

| 항목 | 값 |
|---|---|
| 앱 이름 | 우리 추억 지도 |
| 앱 ID | `app.surprise.memorymap` |
| 아이콘 원본 | `assets/icon.png` (다른 크기는 자동 생성) |

---

## 안드로이드 — APK 직접 설치 (계정·비용 없음)

**제가 손댈 필요 없이 GitHub 이 만들어 줍니다.**

1. 저장소 **Actions** 탭 → `안드로이드 앱 빌드` → 최근 실행 클릭
2. 맨 아래 **Artifacts** 의 `apk` 를 내려받아 압축을 풉니다
3. 그 `.apk` 파일을 폰으로 보내서 엽니다
4. "출처를 알 수 없는 앱" 경고가 나오면 이 앱만 허용해 주세요

지도나 앱 설정을 고쳐서 `main` 에 올리면 APK 가 자동으로 다시 만들어집니다.
버전을 남기고 싶으면 `v1.0.0` 처럼 태그를 붙여 올리세요. **Releases** 에도 올라갑니다.

## 안드로이드 — Google Play 출시

Play 는 **서명된 AAB** 를 요구합니다. 서명 키를 한 번 만들어 저장소에 넣어두면 워크플로가 알아서 만듭니다.

```bash
# 1) 서명 키 만들기 (한 번만, 절대 잃어버리면 안 됩니다 — 잃으면 업데이트를 못 올립니다)
keytool -genkey -v -keystore release.keystore -alias memorymap \
        -keyalg RSA -keysize 2048 -validity 10000

# 2) 저장소에 넣을 형태로 바꾸기
base64 -w0 release.keystore > keystore.txt
```

저장소 **Settings → Secrets and variables → Actions** 에 4개를 넣습니다.

| 이름 | 값 |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `keystore.txt` 내용 |
| `ANDROID_KEYSTORE_PASSWORD` | 키 만들 때 정한 비밀번호 |
| `ANDROID_KEY_ALIAS` | `memorymap` |
| `ANDROID_KEY_PASSWORD` | 키 비밀번호 |

넣고 나면 빌드할 때 `aab` 도 같이 만들어집니다. 그걸 Play Console 에 올리면 됩니다.

Play Console 에서 따로 준비해야 하는 것:
- 개발자 계정 등록 (1회 $25)
- 앱 아이콘·스크린샷·짧은 설명
- 개인정보처리방침 주소 — **사진을 저장하므로 필요합니다**
- 둘만 쓸 거면 **비공개 테스트(내부 테스트) 트랙**으로 올리면 심사 부담이 적습니다

## iOS

**맥과 Xcode 가 필요합니다.** 리눅스에서는 빌드가 안 돼서 여기까지가 제가 준비할 수 있는 전부입니다.

```bash
cd app
npm install
npm run build:www
npx cap sync ios
cd ios/App && pod install        # CocoaPods 필요: sudo gem install cocoapods
open App.xcworkspace            # Xcode 가 열립니다
```

Xcode 에서:
1. **Signing & Capabilities** → 본인 Apple ID 로 팀 선택
2. 아이폰을 케이블로 연결하고 ▶︎ 실행 → 폰에 바로 설치됩니다

무료 Apple ID 로도 설치는 되지만 **7일마다 다시 설치**해야 합니다.
계속 쓰려면 Apple Developer Program(연 $99)이 필요합니다.

App Store 정식 출시는 심사를 받아야 하는데, 웹사이트를 감싼 앱은
심사 지침 4.2(최소 기능)로 거절되는 사례가 많습니다. 둘만 쓸 목적이면
**TestFlight** 로 배포하는 쪽이 현실적입니다.

---

## 지도를 고친 뒤 앱에 반영하기

```bash
cd app
npm run sync        # 지도 화면을 www/ 로 복사하고 두 플랫폼에 동기화
```

안드로이드는 `main` 에 올리면 GitHub 이 자동으로 새 APK 를 만듭니다.
iOS 는 맥에서 위 명령 후 Xcode 로 다시 설치하면 됩니다.

## 앱에서 빠지는 것

`build-www.mjs` 가 앱용으로 만들 때 아래를 덜어냅니다.

- `← 목록` 링크 — 앱은 지도 전용이라 돌아갈 목록이 없습니다
- 서비스워커·웹 매니페스트 — 앱은 파일을 이미 갖고 있어서 필요 없습니다

## 권한

| 기능 | 안드로이드 | iOS |
|---|---|---|
| 내 위치 | `ACCESS_FINE_LOCATION` 등 | `NSLocationWhenInUseUsageDescription` |
| 사진 넣기 | WebView 기본 처리 | `NSPhotoLibraryUsageDescription` |

둘 다 이미 넣어 뒀습니다. 사진은 웹과 같은 Firebase Storage 를 쓰므로
**앱에서 넣은 사진도 웹에서 보이고, 반대도 됩니다.**
