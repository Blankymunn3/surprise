# 추억 지도 앱 — 아키텍처

안드로이드(Compose)와 iOS(SwiftUI)를 **같은 계층 구조와 같은 MVI 규칙**으로 만듭니다.
두 플랫폼의 코드는 공유하지 않지만, **모듈 이름·타입 이름·데이터 흐름은 일부러 똑같이** 맞춥니다.
한쪽을 고칠 때 다른 쪽에서 어디를 고쳐야 하는지 바로 찾을 수 있어야 하기 때문입니다.

## 큰 그림

```
        ┌──────────────────────────────────────────┐
        │  presentation   Compose / SwiftUI + MVI  │  화면·상태
        └───────────────────┬──────────────────────┘
                            │ UseCase 만 호출
        ┌───────────────────▼──────────────────────┐
        │  domain         순수 코틀린 / 순수 스위프트 │  규칙·모델
        └───────────────────▲──────────────────────┘
                            │ Repository 인터페이스 구현
        ┌───────────────────┴──────────────────────┐
        │  data      네트워크·저장소·지도 데이터     │  바깥 세상
        └──────────────────────────────────────────┘
```

**의존 방향은 항상 안쪽(domain)으로만.** domain 은 어떤 프레임워크도 모릅니다.
Compose·SwiftUI·MapLibre·Firebase 는 domain 에서 import 하지 않습니다.

### 계층별 책임

| 계층 | 넣는 것 | 넣지 않는 것 |
|---|---|---|
| **domain** | 모델(`Region`, `MemoryPhoto`), UseCase, Repository **인터페이스** | 안드로이드/iOS SDK, 네트워크, JSON |
| **data** | Repository 구현, API 클라이언트, GeoJSON 파서, 로컬 저장 | 화면 상태, UI 모델 |
| **presentation** | State/Intent/Effect, Reducer, ViewModel/Store, Composable/View | 비즈니스 규칙(도메인으로) |

## 모듈 구성

### 안드로이드 (Gradle 멀티모듈)

```
android/
  app/                     앱 조립·DI 그래프·네비게이션
  core/model/              도메인 모델 (순수 코틀린)
  core/common/             Result, 디스패처, 확장함수
  core/designsystem/       색·타이포·공용 Composable (웹의 핑크 글라스와 맞춤)
  domain/                  UseCase + Repository 인터페이스
  data/auth/               구글 로그인
  data/space/              공간·멤버·초대 (Firestore)
  data/region/             지역 경계·한글 이름 (GeoJSON, 에셋)
  data/photo/              사진 (Firebase Storage)
  feature/space/           공간 목록·만들기·참여·초대 (MVI)
  feature/map/             지도 화면 (MVI)
```

### iOS (Swift Package Manager)

```
ios/
  App/                     앱 진입점·DI 조립
  Packages/
    CoreModel/             도메인 모델 (순수 스위프트)
    CoreCommon/
    DesignSystem/
    Domain/                UseCase + Repository 프로토콜
    DataAuth/
    DataSpace/
    DataRegion/
    DataPhoto/
    FeatureSpace/          공간 목록·만들기·참여·초대 (MVI)
    FeatureMap/            지도 화면 (MVI)
```

**같은 이름을 쓰는 이유**: `data/photo` 를 고쳤으면 iOS `DataPhoto` 도 봐야 한다는 걸
이름만 보고 알 수 있게 하려는 것입니다.

## 기술 선택

| 항목 | 안드로이드 | iOS |
|---|---|---|
| 로그인 | Firebase Auth — 구글 (iOS 스토어 낼 때 애플 추가) | 〃 |
| UI | Jetpack Compose (Material 3) | SwiftUI |
| 상태 관리 | MVI + `StateFlow` | MVI + `@Observable` (Observation) |
| 비동기 | Coroutines / Flow | async-await / AsyncSequence |
| DI | Hilt | 수동 조립 (Composition Root) |
| 직렬화 | kotlinx.serialization | Codable |
| 네트워크 | OkHttp | URLSession |
| 지도 | MapLibre Native Android | MapLibre Native iOS |
| 테스트 | JUnit5 + Turbine | swift-testing |

**iOS DI 를 수동으로 하는 이유**: 화면이 하나뿐이라 프레임워크를 들일 이유가 없습니다.
앱 진입점에서 한 번 조립해 내려보냅니다.

## 지도를 MapLibre 로 정한 이유

- **API 키·결제 계정이 필요 없습니다.** Google Maps 는 키와 결제 등록이 필요합니다.
- 지금 웹에서 쓰는 **OSM 타일과 GeoJSON 을 그대로** 씁니다. 데이터를 새로 만들 필요가 없습니다.
- **지역을 사진으로 채우는 기능**을 두 플랫폼에서 같은 방식(`fill-pattern` 에 런타임 이미지 등록)으로
  구현할 수 있습니다. MapKit 은 오버레이 렌더러를 직접 만들어야 해서 안드로이드와 코드가 갈라집니다.

## 데이터 출처 (웹과 동일)

| 데이터 | 출처 |
|---|---|
| 국내 시군구 경계 | `southkorea-maps` (시군구 GeoJSON) |
| 세계 국가 경계 | `world.geo.json` |
| 해외 시도 경계 | geoBoundaries ADM1 (이탈리아만 ADM2) |
| 나라 한글 이름 | `assets/countries-ko.js` 를 JSON 으로 변환해 앱에 동봉 |
| 시도 한글 이름 | `assets/subdivisions-ko.js` 를 JSON 으로 변환해 앱에 동봉 |
| 사진 | Firebase Storage (`spaces/<공간ID>/regions/<지역코드>.jpg`) |
| 계정·공간·멤버·초대 | Firebase Auth(구글) + Firestore |

**지역 코드 규칙은 웹과 반드시 같아야 합니다.** 웹에서 넣은 사진이 앱에서도 보여야 하니까요.

사진은 **공간(Space) 단위로** 모입니다. 내 공간을 만들고 초대한 사람들과 함께 채우는
방식이라, 사진 경로 앞에 공간 ID 가 붙습니다. 자세한 규칙은 [SPACES.md](SPACES.md).

**앱의 시작 화면은 공간 목록입니다.** 공간이 하나뿐이어도 건너뛰지 않습니다
(이유는 [SPACES.md](SPACES.md) 화면 흐름 참고). 지도는 목록에서 공간을 고른 뒤 들어갑니다.

| 코드 | 뜻 |
|---|---|
| `11140` | 국내 시군구 (행정구역 코드) |
| `C-JPN` | 나라 (ISO alpha-3) |
| `P-JPN-12` | 해외 시도 (나라 + 원본 순번) |
| `bali` | 경계 없이 좌표만 있는 추억 장소 |

## 데이터 흐름

```
사용자 조작 ─▶ Intent ─▶ ViewModel/Store ─▶ UseCase ─▶ Repository ─▶ 네트워크
                             │                                          │
                             ◀────────────── 도메인 모델 ───────────────┘
                             │
                     Reducer(현재 State, 결과) ─▶ 새 State ─▶ 화면 다시 그림
                             │
                             └─▶ Effect (한 번만 일어나는 일: 안내 문구, 사진 선택창)
```

## 오프라인·실패 규칙

웹에서 이미 정한 규칙을 그대로 따릅니다. 이건 사용자와 한 약속이라 바꾸지 않습니다.

- 사진 업로드가 실패하면 **기기에 저장하고 그 사실을 알립니다.** 사진을 잃지 않습니다.
- 목록 조회 15초, 업로드 25초를 넘기면 실패로 봅니다.
- 업로드가 성공하면 기기 사본은 지웁니다.
- 사진은 **캐시하지 않습니다.** 상대가 새로 올린 사진이 늦게 보이면 안 됩니다.

## 참고

- 공간·초대·보안 규칙 → [SPACES.md](SPACES.md)
- MVI 세부 규칙 → [MVI.md](MVI.md)
- 코딩 규칙 → [CONVENTIONS.md](CONVENTIONS.md)
