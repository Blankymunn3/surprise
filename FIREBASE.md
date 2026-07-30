# 사진을 클라우드에 저장하기 (Firebase Storage)

추억 지도(`map/`)에서 넣는 사진은 원래 **그 기기 브라우저(localStorage)** 에만 저장돼서
내 폰에서 넣은 사진이 아내 폰에서는 안 보였습니다.
Firebase Storage를 연결하면 **누가 어디서 넣어도 둘 다 바로** 보입니다.

설정 전까지는 예전과 똑같이 동작하니, 급하지 않으면 그냥 둬도 됩니다.

---

## 1. Firebase 프로젝트 만들기 (5분, 무료)

1. https://console.firebase.google.com 접속 → **프로젝트 추가**
2. 이름은 아무거나 (예: `our-surprise`). Google 애널리틱스는 **끄기** 선택해도 됩니다.
3. 왼쪽 메뉴 **빌드 → Storage** → **시작하기**
   - 위치는 `asia-northeast3 (서울)` 추천
   - 규칙은 일단 아무거나 골라도 됩니다 (3단계에서 바꿉니다)

> Storage를 켤 때 결제(Blaze) 계정을 요구하면 카드 등록이 필요할 수 있습니다.
> 우리 사진 몇십 장 수준은 무료 한도 안이라 요금은 사실상 0원이지만,
> 걱정되면 콘솔에서 **예산 알림**을 1달러쯤으로 걸어 두세요.

## 2. 웹 앱 등록하고 config 붙여넣기

1. 콘솔 홈에서 **웹 앱 추가(`</>` 아이콘)** → 별명 입력 → 등록
2. 화면에 나오는 `firebaseConfig` 값을 복사
3. `assets/firebase.js` 맨 위 `CONFIG`에 그대로 채우기

```js
var CONFIG = {
  apiKey: 'AIzaSy...',
  authDomain: 'our-surprise.firebaseapp.com',
  projectId: 'our-surprise',
  storageBucket: 'our-surprise.firebasestorage.app',
  appId: '1:1234567890:web:abcdef'
};
```

`apiKey`와 `storageBucket` 두 개만 채워져 있으면 켜집니다.
(웹 apiKey는 비밀번호가 아니라 프로젝트 식별자라 공개돼도 괜찮습니다. 실제 보호는 3단계 규칙이 합니다.)

## 3. 보안 규칙 넣기

콘솔 **Storage → Rules** 탭에 이 저장소의 [`storage.rules`](storage.rules) 내용을 붙여 넣고 **게시**.

- `regions/` 폴더만 열려 있고 나머지는 전부 차단
- 5MB 이하 JPEG만 업로드 가능

## 4. 도메인 허용

콘솔 **Storage → 설정(CORS)** 은 건드릴 필요 없고,
**Authentication → Settings → 승인된 도메인** 에 `blankymunn3.github.io` 가 있는지만 확인하세요
(익명 로그인을 쓰지 않으면 대개 필요 없습니다).

## 5. 확인

`map/` 페이지에서 지역을 누르고 사진을 넣었을 때
`"… 도장 깼다! 📸 자기야 폰에서도 보여 💗"` 가 뜨면 성공입니다.
콘솔 **Storage → 파일** 에 `regions/11140.jpg` 같은 파일이 생깁니다.

---

## 동작 방식

| 상황 | 저장 위치 |
|---|---|
| config 안 채움 | 예전처럼 이 기기 localStorage |
| config 채움 + 인터넷 O | Firebase Storage (`regions/<지역코드>.jpg`) → 양쪽 기기 모두 |
| config 채움 + 업로드 실패 | 이 기기 localStorage로 폴백 + "인터넷이 안 돼서…" 안내 |

- 사진은 넣을 때 **가로·세로 최대 760px, JPEG 품질 0.72** 로 줄여서 올립니다 (장당 100KB 안팎).
- 클라우드 업로드가 성공하면 그 지역의 localStorage 사본은 지웁니다 → 기기 저장 공간 부족 문제도 같이 해결됩니다.
- 마지막으로 받아온 목록은 localStorage에 캐시돼서, 다음 방문 때 사진이 먼저 뜨고 뒤이어 최신 목록으로 갱신됩니다.
- `photos/region-<코드>.jpg` 로 직접 올려둔 사진은 그대로 동작합니다 (클라우드 사진이 있으면 그쪽이 우선).

## 더 안전하게 (선택)

지금 규칙은 **주소를 아는 사람은 누구나** `regions/` 에 사진을 올릴 수 있습니다.
사이트가 검색에 안 잡히고(noindex) 4자리 잠금이 있긴 하지만, 그 잠금은 브라우저 안에서만 도는 대문 잠금이라
서버 규칙에서는 확인할 수 없습니다. 더 잠그고 싶다면:

- **익명 로그인 + App Check**: Authentication에서 익명 로그인을 켜고, 규칙을 `allow write: if request.auth != null` 로 바꾼 뒤
  `assets/firebase.js` 에서 `signInAnonymously()` 호출. reCAPTCHA App Check까지 붙이면 남의 사이트에서 우리 버킷을 못 씁니다.
- **읽기까지 잠그기**: `allow read: if request.auth != null` 로 바꾸면 사진 주소를 알아도 로그인 없이는 못 봅니다.

필요해지면 말씀해 주세요. 그때 붙여 드리겠습니다.
