# 아이폰 앱 — Xcode 에서 열고 빌드하기

## 여는 법

```bash
open ios/MemoryMap.xcodeproj
```

Xcode 16 이상이 필요합니다. 열면 `Packages/MemoryMap` 이 **로컬 패키지**로 딸려 옵니다 —
따로 받아올 게 없어서 인터넷 없이도 빌드됩니다.

**시뮬레이터로 돌려보기**는 그냥 ▶︎ 를 누르면 됩니다. 서명이 필요 없습니다.

**내 아이폰에 넣으려면** 한 번만 설정해야 합니다:

1. 프로젝트 → `MemoryMap` 타깃 → **Signing & Capabilities**
2. **Team** 에서 본인 Apple ID 를 고릅니다 (무료 계정으로도 됩니다)
3. 안 되면 **Bundle Identifier** 를 `kr.surprise.memorymap.내이름` 처럼 바꿉니다
   — 남이 이미 쓴 이름이면 등록이 안 됩니다
4. 아이폰을 USB 로 연결하고 위쪽에서 기기를 고른 뒤 ▶︎

무료 Apple ID 로 넣은 앱은 **7일 뒤 만료**됩니다. 그때는 다시 ▶︎ 하면 됩니다.
처음 실행할 때 아이폰에서 *설정 → 일반 → VPN 및 기기 관리* 로 들어가 개발자를 신뢰해야 합니다.

## 짜임새

```
ios/
  MemoryMap.xcodeproj      앱 껍데기 (이 파일만 Xcode 전용)
  MemoryMap-Info.plist     앱 이름·권한 문구·글꼴 등록
  App/                     @main · 조립(AppContainer) · 첫 화면
  Packages/MemoryMap/      화면과 규칙 전부 (SwiftPM, 순수 스위프트)
```

**App/ 은 얇게 둡니다.** 화면·상태·저장소는 전부 패키지 안에 있고, 껍데기는 조립과
iOS 전용인 것(글꼴 등록, 사진 고르기)만 맡습니다. 안드로이드 `app/` 모듈과 같은 자리입니다.

`App/` 폴더는 Xcode 의 **동기화 그룹**이라 파일을 넣기만 하면 프로젝트에 자동으로 들어갑니다.
`.xcodeproj` 를 건드릴 일이 거의 없습니다.

글꼴은 안드로이드가 쓰는 **같은 파일**을 그대로 가리킵니다
(`android/core/designsystem/src/main/res/font/`). 복사본을 두면 두 앱의 글씨가
조용히 달라질 수 있어서요.

## 패키지만 확인하기

Xcode 없이도 됩니다 (맥/리눅스 공통):

```bash
cd ios/Packages/MemoryMap
swift build
swift test
```

## 지금 되는 것

공간 목록 · **지도** · **달력** · **사진 올리기** 까지 돕니다.

지도는 **MapKit** 입니다 — iOS 에 이미 들어 있어 키도, 받을 것도 없습니다. 안드로이드는
MapLibre 를 쓰는데, 지도만 다르고 **"누른 곳이 어느 지역인지" 판정은 두 앱이 같은 코드**로
합니다. 사진 EXIF 로 지역을 알아내는 길도 같습니다.

남은 것은 [docs/app/STATUS.md](../docs/app/STATUS.md).

## 이 저장소의 작업 컨테이너에서는

Xcode 가 없어서 **앱 껍데기는 여기서 빌드되지 않습니다.** 대신 CI 가 맥에서
`xcodebuild` 로 확인합니다 ([ios.yml](../.github/workflows/ios.yml)).
