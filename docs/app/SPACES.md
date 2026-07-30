# 공간(Space)과 초대

셋로그처럼 **내 공간을 만들고, 초대한 사람들과 함께 채우는** 방식입니다.

지금 웹은 사진이 `regions/` 한 곳에 쌓이고 **주소를 아는 사람은 누구나** 읽고 쓸 수 있습니다.
공간으로 나누려면 "누가 어느 공간의 멤버인가"를 서버가 알아야 하고,
그러려면 **로그인이 필요합니다.** 이게 이 문서의 출발점입니다.

## 용어

| 말 | 뜻 |
|---|---|
| **공간(Space)** | 지도 하나. 사진이 이 단위로 모입니다 |
| **멤버(Member)** | 그 공간에 사진을 올릴 수 있는 사람 |
| **주인(Owner)** | 공간을 만든 사람. 멤버를 내보낼 수 있습니다 |
| **초대 코드(Invite)** | 공간에 들어오는 열쇠. 기한이 있습니다 |

## 화면 흐름

```
처음 열기
   │
   ├─ 로그인 (구글)
   │
   └─▶ 공간 목록  ← 앱의 메인 화면. 공간이 하나뿐이어도 여기서 시작합니다
          │
          ├─ [공간 만들기] → 이름을 정하면 초대 링크가 바로 생깁니다
          ├─ [초대 코드로 참여]
          │
          └─ 공간을 고르면 ─▶ [ 지도 | 달력 ] 탭
                                 │
                                 ├─ 사진 넣기 (멤버 누구나, 오른쪽 아래 + )
                                 ├─ 초대하기 → 코드·링크 공유
                                 └─ 멤버 보기 (주인은 내보내기 가능)
```

탭 안의 화면은 [SCREENS.md](SCREENS.md) 에 따로 적었습니다.

**공간이 하나뿐이어도 목록을 건너뛰지 않습니다.** 들어오는 자리가 늘 같아야
새 공간을 만들거나 초대를 받았을 때 어디로 가야 할지 헷갈리지 않습니다.
공간 개수에 따라 첫 화면이 바뀌면, 두 번째 공간이 생기는 순간 앱이
달라진 것처럼 느껴집니다.

지도에서 뒤로 가면 항상 공간 목록으로 돌아옵니다.

## 데이터 구조 (Firestore)

```
spaces/{spaceId}
  name          "우리 추억 지도"
  ownerUid      "abc123"
  createdAt

spaces/{spaceId}/members/{uid}
  role          "owner" | "member"
  displayName   "자기야"
  joinedAt

invites/{code}                     ← 코드 자체가 문서 ID (예: "K7QF2M")
  spaceId
  createdBy
  expiresAt                        기본 7일
  maxUses / usedCount              기본 1회
```

**멤버를 `spaces/{id}/members` 하위에 두는 이유**: 보안 규칙에서
`exists(/databases/../spaces/$(spaceId)/members/$(uid))` 한 줄로 확인할 수 있습니다.

**내가 속한 공간 목록**은 `users/{uid}/spaces/{spaceId}` 로 따로 적어 둡니다.
Firestore 는 하위 컬렉션을 가로질러 "내가 멤버인 공간"을 찾기 어렵기 때문입니다.

## 사진 저장 위치

```
지금:  regions/11140.jpg
바뀜:  spaces/{spaceId}/photos/{photoId}.jpg
```

**지역 코드 규칙은 그대로입니다.** 다만 한 지역에 사진을 여러 장 올릴 수 있게
되면서 파일 이름이 지역 코드에서 사진 ID 로 바뀌고, 지역·날짜는 사진 문서에
적힙니다. 자세한 건 [SCREENS.md](SCREENS.md) 의 데이터 구조.

## 보안 규칙

핵심은 **Storage 규칙에서 Firestore 를 읽어 멤버인지 확인**하는 것입니다.

```
// Storage
match /spaces/{spaceId}/photos/{file} {
  allow read:  if isMember(spaceId);
  allow write: if isMember(spaceId)
               && (request.resource == null
                   || (request.resource.size < 5 * 1024 * 1024
                       && request.resource.contentType == 'image/jpeg'));
}

function isMember(spaceId) {
  return request.auth != null
    && firestore.exists(/databases/(default)/documents/spaces/$(spaceId)/members/$(request.auth.uid));
}
```

이렇게 하면 **주소를 알아도 멤버가 아니면 사진을 못 봅니다.** 지금 웹의 약점이 함께 해결됩니다.

⚠️ 목록 조회(list)는 파일이 아니라 폴더 경로 권한을 봅니다.
`match /spaces/{spaceId}/photos` 에도 `allow read` 를 따로 두고,
클라이언트는 `delimiter` 를 붙여 요청해야 합니다. (웹에서 겪은 문제 — `FIREBASE.md` 참고)

## 초대 흐름

1. 공간을 **만들 때 코드가 자동으로 생깁니다.** 이름을 정하면 바로 공유할 링크가
   나오도록 — 셋로그가 그렇게 하고, 만들고 나서 "초대하기"를 다시 찾게 하지 않으려는
   것입니다. 나중에 다시 부를 때도 **초대하기**로 새 코드를 만듭니다
   (앱이 6자리 코드를 만들고 `invites/{code}` 를 씁니다)
2. 코드나 링크(`memorymap://join/K7QF2M`)를 카톡으로 보냅니다
3. 받은 사람이 로그인 후 코드를 넣으면:
   - 코드가 있는지, 기한이 안 지났는지, 사용 횟수가 남았는지 확인
   - `spaces/{spaceId}/members/{uid}` 와 `users/{uid}/spaces/{spaceId}` 를 씁니다
   - `usedCount` 를 올립니다

**이 과정은 Cloud Functions 로 처리합니다.** 클라이언트가 직접 멤버 문서를 쓰게 하면
코드를 몰라도 아무 공간에나 들어갈 수 있게 되기 때문입니다.
멤버 문서 쓰기는 규칙에서 막고, 함수만 쓸 수 있게 합니다.

## 정하지 않은 것 / 정한 것

| 항목 | 결정 | 이유 |
|---|---|---|
| 로그인 방법 | **구글 로그인**으로 시작 | 무료. 안드로이드에서 가장 매끄럽습니다 |
| 애플 로그인 | iOS 스토어 낼 때 추가 | 다른 소셜 로그인이 있으면 애플이 요구합니다. 유료 개발자 계정 필요 |
| 익명 로그인 | 안 씁니다 | 폰을 바꾸면 사진에 접근할 길이 사라집니다 |
| 공간 개수 | 사람당 여러 개 가능 | 가족용·친구용을 따로 만들 수 있게 |
| 권한 | 멤버는 모두 올리기·지우기 가능 | 둘이 쓰는 앱에서 권한을 나누면 불편하기만 합니다 |
| 주인 권한 | 멤버 내보내기, 공간 이름 바꾸기 | |

## 지금 웹 사진은 어떻게 되나

현재 `regions/` 에 있는 사진들은 **첫 공간으로 옮깁니다.**

1. 두 분이 로그인하고 공간을 하나 만듭니다
2. 기존 `regions/*.jpg` 한 장마다 사진 문서를 만들어 `spaces/{그 공간}/photos/` 로
   옮깁니다 (한 번만 도는 스크립트). 파일 이름에 있던 지역 코드가 `regionCode` 가 되고,
   날짜는 알 수 없으므로 **파일 올린 시각**을 `takenOn` 으로 씁니다.
   지역에 한 장씩뿐이므로 그 사진이 곧 지역 대표사진입니다
3. 웹도 같은 구조를 보도록 고칩니다 (웹은 지역 대표사진만 읽으면 됩니다)

**웹과 앱은 같은 Firebase 프로젝트를 계속 씁니다.** 한쪽에서 넣은 사진이
다른 쪽에서 보여야 한다는 규칙은 그대로입니다.
다만 웹에도 로그인이 붙어야 하므로, 웹 작업이 함께 필요합니다.

## 앱 구조에 미치는 영향

`ARCHITECTURE.md` 의 모듈에 아래가 더해집니다.

```
domain/           Space, Member, Invite 모델 + UseCase
data/auth/        구글 로그인
data/space/       공간·멤버·초대 (Firestore)
feature/space/    공간 목록·만들기·참여·초대 화면
```

지도 화면은 이제 **어느 공간의 지도인지**를 알아야 합니다.
`MapState` 에 `spaceId` 가 들어가고, 사진 경로에 그 값이 쓰입니다.
