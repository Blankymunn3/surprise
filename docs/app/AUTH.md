# 로그인 붙이기

지금은 로그인이 없어서 **초대 코드를 아는 사람은 누구나** 그 짜국의 사진을 보고 넣을 수
있습니다. 이 문서는 그걸 없애는 계획입니다.

## 왜 Firestore 가 같이 와야 하나

"이 사람이 이 짜국의 멤버인가" 를 **서버가** 판단해야 합니다. 그런데 Storage 규칙은
**다른 파일의 내용을 읽지 못합니다.** 그래서 멤버 목록을 Storage 에 두면 규칙이 그걸 볼 수
없고, 결국 지금과 똑같이 "경로를 아는 사람은 통과" 가 됩니다.

Firestore 규칙은 다른 문서를 읽을 수 있습니다(`get()`). 멤버 목록이 거기 있어야
Storage 규칙이 `firestore.get(...)` 으로 물어볼 수 있습니다.

**즉 로그인만 붙이는 것은 반쪽입니다.** 로그인 + Firestore 가 한 묶음입니다.

## 혼자 쓰는 짜국은 서버에 안 올립니다

짜국을 만들 때 **혼자** 쓸지 **같이** 쓸지 고릅니다.

| | 혼자 | 같이 |
|---|---|---|
| 사진 | **기기 안에만** | Storage 에 올림 |
| 목록·지역·날짜 | 기기 안 파일 | Firestore |
| 로그인 | **필요 없음** | 필요 |
| 다른 기기에서 | 안 보임 | 보임 |

> 같이 쓰는 짜국은 **두 명 전용이 아닙니다.** 초대 코드를 받은 사람은 몇이든 들어옵니다 —
> 멤버 목록도, 규칙도 인원을 막지 않습니다.

혼자 쓰는 짜국은 서버를 아예 안 씁니다. 그래서 **로그인 없이도 앱이 돌아갑니다** —
로그인은 "같이" 를 고를 때만 물어봅니다.

이렇게 나누는 이유가 셋입니다. 요금이 안 나가고, 남에게 사진이 나갈 일이 원천적으로
없고, 인터넷이 없어도 됩니다.

**나중에 '같이' 로 바꿀 수 있어야 합니다** — 혼자 쓰다가 같이 쓰고 싶어지는 게 자연스러운
순서라서요. 그때 기기 안 사진을 올리고 Firestore 문서를 만듭니다. 반대(같이 → 혼자)는
남이 올린 사진을 어떻게 할지 답이 없어서 만들지 않습니다.

바꿀 곳: `Space` 에 종류가 하나 붙고, `PhotoRepository` 구현이 **둘**이 됩니다
(기기 / 서버). 화면은 그대로입니다 — 어느 쪽을 쓸지는 조립하는 곳이 정합니다.

## 요금 — Firebase 냐 Cloudflare(R2) 냐

**몇 사람이 쓰는 규모에서는 어느 쪽이든 사실상 0원입니다.** 요금이 결정 근거가 되지
않습니다.

사진을 760px / 품질 0.72 로 줄여 올리므로 한 장이 대략 **60~100KB** 입니다.
1만 장을 올려도 1GB 가 안 됩니다.

| | Firebase Storage | Cloudflare R2 |
|---|---|---|
| 무료 한도 | 5GB 저장 · 하루 1GB 내려받기 | 10GB 저장 · 내려받기 **무제한** |
| 저장 (넘으면) | 약 $0.026/GB·월 | $0.015/GB·월 |
| 내려받기 (넘으면) | 약 **$0.12/GB** | **$0** |

R2 가 싼 것은 맞습니다. 특히 **내려받기가 공짜**라 사진을 많이 보는 앱에서는 커지면
차이가 큽니다. 하지만 지금 규모에서는 둘 다 무료 한도 안에 있습니다.

**그래서 Firebase 를 권합니다.** 이유는 값이 아니라 **손이 덜 가서**입니다:

- R2 에는 "이 사람이 이 짜국의 멤버인가" 를 볼 규칙이 없습니다. 그걸 하려면
  **Cloudflare Worker 를 직접 짜서 올려야** 합니다 — 서버가 하나 생기는 셈입니다
- Firebase 는 규칙 파일 한 장이면 됩니다. 지금 쓰는 인증과 그대로 이어집니다

바꿀 만한 때는 이럴 때입니다: 사진이 수만 장이 되거나, **내려받기가 하루 1GB 를 넘기
시작할 때**. 그때는 **Storage 만** R2 로 옮기면 됩니다 — 사진 주소를 어디서 만드는지가
한 곳(`FirebaseStorage`)에 모여 있어서 그 파일만 갈아 끼우면 되고, 멤버 판정은
Firestore 에 그대로 둡니다.

**사람이 늘면 저장은 그대로인데 내려받기만 배로 늡니다.** 사진은 한 번 올라가지만 보는
사람마다 받아 가서요. 저장 5GB 보다 **하루 1GB 내려받기**가 먼저 걸릴 자리라, 인원이
늘어날 때 봐야 할 숫자는 이쪽입니다.

> 값은 바뀝니다. 실제로 옮기기 전에 그때 가격표를 다시 보세요.

## 순서

| | 하는 일 | 누가 | 상태 |
|---|---|---|---|
| 0 | 혼자/둘이 나누기 (로그인 없이 되는 부분) | 제가 | |
| 1 | Firebase 설정 (CLI) | **맥의 Claude** | ✅ 끝 |
| 2 | 보안 규칙 (Storage · Firestore) | 제가 씀 → **맥에서 deploy** | ✅ 게시됨 |
| 3 | 로그인 화면 · 토큰 관리 | 제가 | |
| 4 | 공간·사진을 Firestore 로 옮기기 | 제가 | |
| 5 | 기존 데이터 옮기기 | 아래 참고 | |

1번이 끝나야 3번이 돌아갑니다. **1번부터 해 주세요** — 맥에서 Claude 를 열면 됩니다.

> **1번은 다 끝났습니다.** 구글 로그인도 켜졌고 설정 파일 두 개에 OAuth 클라이언트가
> 들어와 있습니다. 이제 3번(로그인 화면)이 돌아갑니다.

## 1. Firebase 설정 — 맥에서 Claude 로

> ✅ **이 절은 전부 끝났습니다** (2026-07-31, 맥에서). 아래 내용은 기록으로 남겨 둡니다.
> 실제로 만들어진 것:
>
> | | |
> |---|---|
> | 프로젝트 | `our-surprise` (`.firebaserc` 에 고정) |
> | Firestore | `(default)` · `asia-northeast3` (서울) |
> | 규칙 | `firestore.rules` · `storage.rules` 게시됨 |
> | 안드로이드 앱 | `1:419812459548:android:ccab28c6f8dce6eefd4bd9` |
> | iOS 앱 | `1:419812459548:ios:d2e5a8506cbaa89ffd4bd9` |
> | 디버그 SHA-1 | `DF:DF:FB:EF:D0:EA:60:40:46:1A:88:CB:88:AB:0F:8D:C3:31:CC:5B` |
> | 구글 로그인 | 켜짐 |
>
> **3번(로그인 화면)에서 쓸 값** — 설정 파일 안에 다 들어 있지만 어느 것이 어느 쪽인지
> 헷갈리기 쉬워서 적어 둡니다:
>
> | 어디에 | 값 |
> |---|---|
> | 안드로이드 `serverClientId` (**web 클라이언트**, type 3) | `419812459548-ldmh2hi0vb5lmjase8jpctqei4sk4mbp.apps.googleusercontent.com` |
> | iOS `CLIENT_ID` | `419812459548-4vruv826mfgfkfi3dppobg87c3du1vdr.apps.googleusercontent.com` |
> | iOS URL Types 에 넣을 `REVERSED_CLIENT_ID` | `com.googleusercontent.apps.419812459548-4vruv826mfgfkfi3dppobg87c3du1vdr` |
>
> ⚠️ 안드로이드에서 ID 토큰을 받으려면 **android 클라이언트(type 1)가 아니라 web
> 클라이언트(type 3)** 를 `serverClientId` 로 줘야 합니다. 자주 틀리는 곳입니다.
>
> iOS 의 URL Types 는 **아직 안 넣었습니다** — Xcode 프로젝트 수정이라 3번에서 같이 합니다.

**웹에서 도는 Claude 는 이 저장소만 볼 수 있고 님 계정으로 Firebase 에 손대지 못합니다.**
그래서 이 부분은 **맥에서 Claude Code 를 열어** 시키는 것이 가장 빠릅니다. 거기에는
이미 `firebase login` 이 되어 있어서 CLI 가 그대로 돕니다.

```bash
cd <이 저장소>
claude
```

그 세션에 "AUTH.md 1번 해 줘" 라고 하면 아래를 그대로 실행하면 됩니다.

> ⚠️ **마지막 하나(구글 로그인 켜기)만 CLI 로 안 됩니다.** 콘솔에서 눌러야 합니다.

### 미리 확인

```bash
firebase --version
firebase login:list          # 로그인 계정 확인
firebase use our-surprise    # 프로젝트 고르기
```

### 1-1. Firestore 만들기

```bash
# 위치는 한 번 정하면 못 바꿉니다. 서울로 둡니다.
firebase firestore:databases:create "(default)" --location asia-northeast3
```

이미 있으면 오류가 납니다 — 그러면 넘어가면 됩니다.

### 1-2. 규칙 게시

규칙 파일은 저장소에 있습니다 (`storage.rules`, `firestore.rules`).
`firebase.json` 이 어느 파일을 올릴지 가리킵니다.

```bash
firebase deploy --only firestore:rules,storage
```

**이게 되면 앞으로 콘솔에 붙여넣을 일이 없습니다.** 규칙을 고칠 때마다 이 한 줄입니다.

### 1-3. 안드로이드 앱 등록 + 설정 파일

```bash
# 이미 있으면 apps:list 로 앱 ID 만 확인하면 됩니다
firebase apps:create ANDROID "짜국" --package-name kr.surprise.memorymap
firebase apps:list ANDROID

# 구글 로그인은 SHA-1 지문이 등록돼 있어야 됩니다 (디버그 키)
keytool -list -v -keystore ~/.android/debug.keystore \
        -alias androiddebugkey -storepass android -keypass android | grep SHA1
firebase apps:android:sha:create <앱ID> <위에서 나온 SHA1>

firebase apps:sdkconfig ANDROID <앱ID> --out android/app/google-services.json
```

> `google-services.json` 안에는 비밀이 없습니다(웹 API 키와 같은 성격). 커밋해도 됩니다.
> 실제 보안은 **규칙**이 합니다.

### 1-4. iOS 앱 등록 + 설정 파일

```bash
firebase apps:create IOS "짜국" --bundle-id kr.surprise.memorymap
firebase apps:list IOS
firebase apps:sdkconfig IOS <앱ID> --out ios/App/GoogleService-Info.plist
```

받은 plist 의 `REVERSED_CLIENT_ID` 를 Xcode → 타깃 → Info → URL Types 에 넣어야
구글 로그인 창이 앱으로 돌아옵니다. (3번에서 다시 안내합니다.)

### 1-5. 구글 로그인 켜기 — **여기만 콘솔**

[콘솔](https://console.firebase.google.com) → **Authentication** → Sign-in method
→ **Google** → 사용 설정 → 지원 이메일 고르기 → 저장

CLI 에 이걸 켜는 명령이 없습니다. 한 번만 하면 끝입니다.

**켠 다음에는 설정 파일을 다시 받아야 합니다.** 구글 로그인을 켜는 순간 OAuth 클라이언트가
생기는데, 켜기 전에 받은 파일에는 그게 없어서(`oauth_client` 가 빈 배열, plist 에
`REVERSED_CLIENT_ID` 없음) 그대로 두면 로그인 창이 안 뜹니다.

```bash
# 파일이 있으면 덮어쓰지 않고 오류가 납니다. 지우고 다시 받으세요.
rm android/app/google-services.json ios/App/GoogleService-Info.plist
firebase apps:sdkconfig ANDROID 1:419812459548:android:ccab28c6f8dce6eefd4bd9 \
        --out android/app/google-services.json
firebase apps:sdkconfig IOS 1:419812459548:ios:d2e5a8506cbaa89ffd4bd9 \
        --out ios/App/GoogleService-Info.plist

# 들어왔는지 확인 — 둘 다 값이 나와야 합니다
grep -o 'client_id[^,]*' android/app/google-services.json | head -3
grep -A1 REVERSED_CLIENT_ID ios/App/GoogleService-Info.plist
```

디버그 키가 아닌 **release 키로 서명해서 돌릴 때가 오면 그 키의 SHA-1 도
`apps:android:sha:create` 로 넣어야 합니다.** 안 넣으면 릴리스 빌드에서만 구글 로그인이
조용히 실패합니다.

### 끝났으면

```bash
git add android/app/google-services.json ios/App/GoogleService-Info.plist
git commit -m "Firebase 설정 파일"
git push
```

푸시해 주시면 이어서 로그인 화면을 붙이겠습니다.

## 일을 나누는 법

두 곳에서 Claude 를 쓰게 되므로 **같은 파일을 동시에 고치지 않도록** 나눕니다.

| | 맥의 Claude | 웹의 Claude |
|---|---|---|
| Firebase CLI · 설정 파일 | ✅ | ❌ (권한 없음) |
| 규칙 게시 | ✅ | ❌ |
| 앱 코드 | 안 하는 게 좋음 | ✅ |

맥에서 커밋·푸시하면 웹 쪽이 받아서 이어갑니다. 반대도 같습니다.

## 2. 규칙 (제가 쓰고, 맥에서 `firebase deploy`)

규칙은 **저장소 파일이 원본**입니다. 문서에 사본을 두면 둘이 어긋나서요.

| 파일 | 무엇 |
|---|---|
| [`firestore.rules`](../../firestore.rules) | 짜국·멤버·사진 정보 — **멤버 판정이 여기서** |
| [`storage.rules`](../../storage.rules) | 사진 파일 — Firestore 를 보고 멤버인지 확인 |
| [`firebase.json`](../../firebase.json) | 어느 파일을 올릴지 |

핵심은 이 한 줄입니다:

```
firestore.exists(/databases/(default)/documents/spaces/$(spaceId)/members/$(request.auth.uid))
```

Storage 규칙이 Firestore 를 **건너다보는** 부분입니다. 이게 되기 때문에 사진 파일에
"이 사람이 멤버인가" 를 물을 수 있습니다. Storage 안에만 멤버 목록을 두면 이게 안 됩니다.

`firestore.rules` 는 지금 만들어만 뒀고 **앱은 아직 안 씁니다.** 로그인이 붙어야
쓰이기 시작합니다. `storage.rules` 는 로그인이 붙는 시점에 위 규칙으로 바꿉니다 —
지금 바꾸면 로그인 없는 현재 앱이 바로 멈춥니다.

## 3~4. 앱에서 바뀌는 것

지금 임시로 하고 있는 두 가지가 사라집니다 (`STATUS.md`):

```
지금:  spaces/<코드>/photos/2026-03-05_11140_a1b2c3.jpg   ← 이름에 지역·날짜
목표:  spaces/<짜국ID>/photos/<사진ID>.jpg + Firestore 문서

지금:  초대 코드 = 짜국 ID (코드를 알면 경로를 앎)
목표:  invites/<코드> 문서 → 짜국 ID. 코드로는 문 앞까지만.
```

**화면과 도메인은 그대로입니다.** 바뀌는 곳은 데이터 계층뿐입니다 —
`SharedSpaceRepository`, `FirebasePhotoRepository`, `PhotoObjectName` (양쪽).
그렇게 되도록 처음부터 나눠 뒀습니다.

로그인은 **네이티브 SDK 로 구글 토큰만 받고**, 그 뒤는 지금처럼 REST 로 씁니다.
Firebase SDK 를 통째로 넣지 않는 이유는 지금 Storage 를 REST 로 쓰는 것과 같습니다 —
받을 것이 적고, 두 앱이 같은 방식으로 움직입니다.

```
구글 로그인 SDK  →  ID 토큰
      ↓
identitytoolkit.googleapis.com/v1/accounts:signInWithIdp   (REST)
      ↓
Firebase ID 토큰 → Storage · Firestore 요청 헤더에 얹음
```

> ✅ **3번은 여기까지 됐습니다** — 토큰 교환·보관·갱신, 양쪽 구글 로그인 SDK,
> 로그인 시트, `FirebaseStorage` 의 `Authorization` 헤더.

### 사진 요청에도 토큰이 실립니다

헤더는 처음에 **REST 요청에만** 실렸습니다. 사진을 화면에 그릴 때는 `downloadUrl()` 을
이미지 로더에 넘기는데 그 요청은 우리 헤더를 안 타서, 규칙을 조이면 목록은 나오는데
사진만 안 뜨는 상태가 됩니다. 양쪽 다 손봤습니다:

| | 어떻게 |
|---|---|
| 안드로이드 | `SingletonImageLoader.Factory` 로 Coil 기본 로더를 갈아 끼우고 **OkHttp 인터셉터**로 헤더를 얹음 |
| iOS | `AsyncImage` 는 헤더를 못 붙입니다 → `RemotePhoto` 를 만들어 URLSession 으로 직접 받음 |

둘 다 **우리 버킷으로 가는 요청에만** 답니다. 지도 타일처럼 남의 서버로 가는 요청에
우리 토큰을 실어 보내면 안 됩니다.

> 안드로이드에서 지도 대표사진은 `SingletonImageLoader.get(context)` 로 **같은 로더**를
> 씁니다. 거기서 `ImageLoader(context)` 를 새로 만들면 인터셉터가 빠져 대표사진만
> 조용히 안 뜹니다.

### 짜국은 Firestore 로 옮겼습니다

```
spaces/{짜국ID}                 이름 · 주인
spaces/{짜국ID}/members/{uid}   누가 멤버인가  ← 규칙이 보는 곳
invites/{코드}                  코드 → 짜국ID
users/{uid}/spaces/{짜국ID}     내가 어느 짜국에 속하나
```

**초대 코드가 더 이상 짜국 ID 가 아닙니다.** 코드를 알아도 경로를 모르고, 경로를 알아도
멤버가 아니면 못 읽습니다.

`users/{uid}/spaces` 를 둔 이유: 기기 안에만 목록이 있으면 **새 폰에서 로그인해도 짜국이
하나도 안 보입니다.** '다른 기기에서 보임' 이 같이 쓰는 짜국의 값이라 서버에 둡니다.

그래서 **`storage.rules` 를 조였습니다** — `spaces/` 는 이제 멤버만 봅니다.
`regions/`(웹이 쓰는 자리)는 로그인이 없어 그대로 열려 있습니다.

> ⚠️ **규칙을 게시하는 순간 옛 버전 앱은 사진을 못 봅니다.** 로그인이 없어서요.
> 게시는 `firebase deploy --only firestore:rules,storage`.

### 아직 남은 것

사진 **문서**는 아직 Firestore 로 안 갔습니다. 파일 이름에 지역·날짜가 들어 있는
방식(`PhotoObjectName`)을 그대로 씁니다 — 규칙에는 영향이 없고(멤버 판정은 이미 되므로),
`spaces/<짜국ID>/photos/<사진ID>.jpg` + 문서로 바꾸는 것은 그다음 정리입니다.

## 5. 기존 데이터

지금 짜국에 올린 사진들은 **규칙이 바뀌는 순간 안 보이게 됩니다.** 멤버 문서가 없어서요.

> ✅ **정해졌습니다 (2026-07-31): 새로 시작합니다.** 옮기는 스크립트는 만들지 않습니다.
> 시험 삼아 넣은 사진들이라 새로 만들어 다시 올리는 편이 빠릅니다.
> 옛 사진은 Storage 의 `spaces/<옛코드>/` 에 그대로 남아 있지만 앱에서는 안 보입니다 —
> 지우고 싶으면 콘솔에서 그 폴더를 지우면 됩니다.

**웹(`map/`)은 로그인이 없어서 `spaces/` 를 못 읽게 됩니다.** 웹은 `regions/` 만 쓰므로
당장 깨지지는 않지만, 앱과 웹이 같은 사진을 보는 일은 웹에도 로그인이 붙어야 합니다.

---

# 부록: 0번 — 혼자/같이 나누기 (구현 계획)

로그인과 무관해서 **지금 바로 할 수 있는** 부분입니다. 파일 단위로 적어 둡니다.

> ✅ **0번은 다 끝났습니다** (2026-07-31). 1~4 는 `d0710ac`, 5 는 디자인 `fd314dd`·`f189ade`
> → 구현. 양쪽 빌드·테스트 통과.

## 무엇이 달라지나

`Space` 에 종류가 하나 붙고, 사진 저장소 구현이 **둘**이 됩니다. 화면과 도메인은
그대로입니다 — 어느 저장소를 쓸지는 **조립하는 곳**이 정합니다.

## 손대는 파일

### 1. 모델 (양쪽)

`core/model/Space.kt` · `CoreModel/Space.swift`

```kotlin
/** 혼자 쓰는 짜국은 사진이 기기 안에만 있습니다. 서버도, 로그인도 안 씁니다. */
enum class SpaceKind { Personal, Shared }

data class Space(..., val kind: SpaceKind = SpaceKind.Personal, ...)
```

기본값을 `Personal` 로 두는 이유: 새 값이 없는 옛 데이터를 읽었을 때 **서버로 나가지
않는 쪽**이 안전합니다. 반대로 두면 옛 짜국이 조용히 공유로 취급됩니다.

### 2. 기기 안 사진 저장소 (새 파일, 양쪽)

`data/photo/LocalPhotoRepository.kt` · `DataPhoto/LocalPhotoRepository.swift`

`PhotoRepository` 를 그대로 구현합니다. 서버 대신 앱 폴더에 씁니다.

```
안드로이드:  filesDir/spaces/<짜국ID>/photos/<파일이름>.jpg
iOS:        Application Support/spaces/<짜국ID>/photos/<파일이름>.jpg
```

- 파일 이름 규칙은 **지금 것을 그대로** 씁니다 (`PhotoObjectName`) — 지역·날짜가
  이름에 들어 있어 목록 한 번으로 다 알 수 있습니다. 로그인이 붙어도 **혼자 짜국은
  이 방식 그대로** 둡니다. 기기 안에는 Firestore 가 없으니까요.
- `downloadUrl` 자리에는 `file://` 경로를 넣습니다. Coil 과 AsyncImage 둘 다 그대로 읽습니다.
- 대표사진(`covers.json`)도 같은 폴더에 파일로 둡니다.

### 3. 공간 저장소 (양쪽)

`data/space/SharedSpaceRepository.kt` · `DataSpace/SharedSpaceRepository.swift`

- `create(name, kind)` 로 종류를 받습니다
- **`kind == Personal` 이면 네트워크를 아예 안 탑니다.** 기기 안 목록에만 적습니다
- `kind == Shared` 일 때만 지금처럼 `space.json` 을 올립니다
- 종류는 기기 안 목록(`spaces.json` / UserDefaults)에 같이 적습니다

### 4. 조립 (양쪽)

`app/AppContainer.kt` · `App/AppContainer.swift`

```kotlin
fun photoRepository(kind: SpaceKind): PhotoRepository =
    if (kind == SpaceKind.Personal) local else remote
```

`mapFactory` · `calendarFactory` · `uploadFactory` 가 짜국의 종류를 보고 고릅니다.

### 5. 화면

> ✅ **다 만들었습니다.** 디자인 원본은 `design.html` 의 '짜국 만들기'(시트)와
> '공간 목록'(카드 표시)입니다.

**만들기 시트** — 이름 입력 위에 두 갈래를 놓습니다.

```
혼자 쓸래요        사진이 이 폰에만 있어요
같이 볼래요        초대한 사람들과 같이 봐요 · 로그인이 필요해요
```

기본은 **혼자**입니다. 같이를 고를 때만 로그인을 물어봅니다(3번이 붙은 뒤).
**"둘이" 라고 쓰지 않습니다** — 인원이 두 명으로 정해져 있지 않습니다.

**짜국 카드** — 혼자면 작은 표시 하나. 어느 것이 폰에만 있는지 한눈에 보여야 합니다.

**초대 코드** — 혼자 짜국에는 없습니다. 초대할 사람이 없으니까요.

## 순서

1. 모델에 `SpaceKind` (양쪽) — 여기부터 하면 나머지가 컴파일 오류로 안내해 줍니다
2. `LocalPhotoRepository` (양쪽)
3. `SharedSpaceRepository` 에 종류 반영
4. 조립에서 갈라 쓰기
5. 만들기 시트 · 카드 표시

## 확인할 것

- 혼자 짜국을 만들고 사진을 올린 뒤 **비행기 모드**에서 그대로 보이는지
- 앱을 껐다 켜도 남아 있는지
- 같이 짜국은 지금처럼 동작하는지 (되돌아간 것이 없는지)
- **세 명 이상**이 같은 짜국에 들어와도 멤버가 다 보이는지 (카드는 넷까지 얼굴, 그 뒤는 `+N`)
