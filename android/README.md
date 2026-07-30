# 안드로이드 앱 — 빌드하고 폰에 넣기

## 폰에 설치만 하고 싶다면 (제일 쉬움)

빌드할 필요 없습니다. CI 가 만들어 둔 파일을 받으면 됩니다.

1. **폰 브라우저**로 [릴리스 페이지](https://github.com/Blankymunn3/surprise/releases) 를 엽니다
2. 맨 위 `추억 지도 apk-…` 를 열고 `memorymap-….apk` 를 누릅니다
3. "이 출처의 앱 설치 허용" 을 켜라고 하면 켭니다 (그 브라우저에 한 번만)
4. 설치 후 열기

파일 이름의 날짜와 커밋으로 어느 시점 것인지 알 수 있습니다.

**새 APK 는 부를 때만 만들어집니다.** 저장소 → [Actions](https://github.com/Blankymunn3/surprise/actions/workflows/apk.yml)
→ `apk` → **Run workflow** 를 누르면 2 분쯤 뒤 릴리스에 올라옵니다.

> ⚠️ 디버그 빌드입니다. 스토어 배포본이 아니고, 서명도 개발용 키로 되어 있습니다.
> 나중에 스토어에 올릴 때는 서명 키를 따로 만들어야 합니다.

**아이폰은 아직 설치본을 못 만듭니다.** 코드(SwiftPM 패키지)는 다 있지만 Xcode 앱
타깃이 없고, 아이폰에 넣으려면 맥과 애플 개발자 계정이 필요합니다
([STATUS.md](../docs/app/STATUS.md)).

## PC 에서 직접 빌드하려면

```bash
cd android
./gradlew assembleDebug          # APK 만들기
./gradlew test                   # 단위 테스트
./gradlew installDebug           # USB 로 연결된 폰에 바로 설치
```

만들어진 파일: `app/build/outputs/apk/debug/app-debug.apk`

필요한 것: **JDK 17**. 안드로이드 SDK 는 안드로이드 스튜디오를 깔면 같이 옵니다.
스튜디오로 열려면 이 `android` 폴더를 열면 됩니다 (저장소 최상위가 아닙니다).

## 이 저장소의 작업 컨테이너에서는 빌드가 안 됩니다

`dl.google.com` 이 막혀 있어 안드로이드 의존성을 받을 수 없습니다.
그래서 컴파일 확인은 CI 에서 합니다 ([android.yml](../.github/workflows/android.yml)).

로컬에서 확인할 수 있는 것은 **순수 코틀린 모듈**뿐입니다 — `core/model`,
`core/common`, `domain`. 달력 격자·대표사진·올리기 기본값·지역 검색처럼
앱의 규칙이 여기 모여 있어서, 중요한 로직은 대부분 로컬에서도 테스트됩니다.

## 모듈

```
app/                     앱 조립 · 네비게이션 · AppContainer(수동 DI)
core/model               도메인 모델 (순수 코틀린)
core/common              Outcome · 실패 종류 · 웹과 맞춘 제한값 (순수 코틀린)
core/network             Firebase Storage REST (순수 코틀린)
core/ui                  MVI 뼈대
core/designsystem        색 · 글씨 · 공용 Composable (Pretendard 동봉)
domain                   UseCase + Repository 인터페이스 (순수 코틀린)
data/photo               사진 (Storage REST) · EXIF · 사진 줄이기
data/space               공간 · 초대 코드
data/region              지역 이름 · 경계 · 좌표→지역 판정
feature/space            공간 목록
feature/map              지도 탭
feature/calendar         달력 탭
feature/upload           사진 올리기 시트
```

자세한 규칙은 [docs/app/](../docs/app/) 에 있습니다.
