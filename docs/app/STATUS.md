# 지금 만들어진 것 / 아직 아닌 것

이 문서는 **약속이 아니라 현재 상태**입니다. 설계 문서(`ARCHITECTURE.md`, `SCREENS.md`,
`SPACES.md`)는 목표를 적은 것이고, 여기는 그 중 어디까지 실제로 돌아가는지를 적습니다.

## 만들어진 것

| | 안드로이드 | iOS |
|---|---|---|
| 멀티모듈 · 클린 아키텍처 | ✅ 12개 모듈 | ✅ 11개 타깃 |
| MVI (State/Intent/Reducer/Store) | ✅ | ✅ |
| 디자인 토큰 (design.html 그대로) | ✅ Pretendard 동봉 | ✅ 토큰만 (폰트는 앱 껍데기에서 등록) |
| 공간 목록 화면 | ✅ | ✅ |
| 지도 \| 달력 탭 | ✅ | ✅ |
| 달력 화면 | ✅ | ✅ |
| 사진 올리기 시트 | ✅ | ✅ |
| 지역 검색 | ✅ | ✅ |
| 좌표 → 지역 (기기 안에서) | ✅ | ✅ |
| EXIF 날짜·위치 읽기 | ✅ | ✅ |
| 사진 줄이기 (760px / 품질 72) | ✅ | ✅ |
| Firebase Storage REST | ✅ | ✅ |
| 실제 지도 | ✅ MapLibre | ✅ MapKit (iOS 기본 제공) |
| 앱 껍데기 (설치되는 앱) | ✅ | ✅ Xcode 프로젝트 (`ios/MemoryMap.xcodeproj`) |

## 지금 방식과 목표 방식이 다른 곳

**로그인이 아직 없습니다.** 그래서 두 가지를 임시로 다르게 하고 있습니다.
둘 다 데이터 계층 안에만 있어서, 로그인이 붙으면 화면과 도메인은 그대로 두고
그 파일들만 갈아 끼우면 됩니다.

### 1. 사진의 지역·날짜를 파일 이름에 적습니다

```
지금:  spaces/<공간ID>/photos/2026-03-05_11140_a1b2c3.jpg
목표:  spaces/<공간ID>/photos/<사진ID>.jpg  +  Firestore 문서에 지역·날짜
```

Firestore 를 쓰려면 로그인이 필요합니다. 지금은 사진 문서를 둘 곳이 없어서,
목록 조회 한 번으로 지역·날짜까지 알 수 있게 이름에 적었습니다.
파싱은 **자리로** 합니다(앞 10글자가 날짜, 마지막 `_` 뒤가 사진 ID) — 지역 코드에
`_` 가 들어 있어도 깨지지 않습니다.

바꿀 곳: `PhotoObjectName.kt` / `PhotoObjectName.swift` 와 각 `FirebasePhotoRepository`.

### 2. 초대 코드가 곧 공간 ID 입니다

```
지금:  코드 K7QF2M → 저장소 경로 spaces/K7QF2M/
목표:  invites/{code} 문서 → spaceId 로 바꿔치기, 멤버 문서는 Cloud Functions 가 씀
```

같은 코드를 아는 두 폰이 같은 경로를 보게 하는 가장 단순한 방법입니다.

⚠️ **이 방식은 코드를 아는 사람은 누구나 그 공간의 사진을 보고 넣을 수 있다는 뜻입니다.**
지금 웹과 같은 수준의 약점이고(웹의 4자리 비밀번호도 브라우저 안에서만 확인합니다),
로그인이 붙어야 해결됩니다. `SPACES.md` 의 보안 규칙이 그 해결책입니다.

바꿀 곳: `SharedSpaceRepository` (양쪽) 와 `storage.rules`.

## 아직 안 한 것 (순서대로)

1. **지도에 지역을 사진으로 칠하고 표시 찍기** — 지금 지도는 기본 지도를 그리고
   "누른 곳이 어느 지역인지"까지만 합니다. 지역을 대표사진으로 칠하려면 MapLibre 에
   런타임 이미지를 `fill-pattern` 으로 등록해야 하고, 표시(마커)는 annotation 플러그인이
   따로 필요합니다. 지역 판정은 지도가 아니라 도메인이 하므로 화면 흐름은 이미 완성입니다.
2. **iOS 달력 옆으로 넘기기** — 안드로이드는 넘겨서 달을 바꿉니다. iOS 는 아직
   화살표로만 바꿉니다.
3. **로그인(구글) + Firestore** — 위 두 임시 방식이 사라집니다.
4. **해외 시도(ADM1) 경계** — 웹은 '시도 나누기' 를 누를 때 받아옵니다. 앱도 같은 방식으로.
5. **웹을 새 구조에 맞추기** — 웹은 아직 `regions/<코드>.jpg` 한 장만 봅니다.

## 확인 방법

- 안드로이드: `cd android && ./gradlew test assembleDebug` (CI: `.github/workflows/android.yml`)
- iOS: `cd ios/Packages/MemoryMap && swift build && swift test` (CI: `.github/workflows/ios.yml`)

이 저장소의 작업 컨테이너에서는 `dl.google.com` 이 막혀 있어 **안드로이드는 로컬에서
컴파일되지 않습니다.** 순수 코틀린 모듈(`core/model`, `core/common`, `domain`)만
따로 빌드해 확인할 수 있고, 나머지는 CI 에서 봅니다.
