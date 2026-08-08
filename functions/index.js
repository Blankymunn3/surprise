/**
 * 짜국의 서버 조각. 클라이언트가 못 하게 **해야** 안전해지는 일만 여기 둡니다 —
 * 나머지는 전부 앱이 Firestore/Storage REST 로 직접 합니다 (`docs/app/AUTH.md`).
 *
 * | 함수 | 왜 서버인가 |
 * |---|---|
 * | joinSpace | 규칙은 "코드가 맞는지"를 검사할 수 없습니다 — 멤버 문서를 만들 때
 * |           | 초대 문서를 견줘야 하는데, 그 대조를 클라이언트에 맡기면 코드 없이
 * |           | 짜국 ID 만 알아도 들어와집니다. 검증과 등록을 한 손에 쥐어야 합니다. |
 * | notifyPhoto | 다른 멤버의 기기로 보내는 일은 발신 권한(admin)이 필요합니다. |
 *
 * 앱은 SDK 없이 callable 규약(POST {"data": ...} + Bearer ID토큰)으로 부릅니다.
 */
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
// 서울. 사용자도 데이터(asia-northeast3)도 여기 있습니다.
setGlobalOptions({ region: "asia-northeast3", maxInstances: 3 });

// 앱의 InviteCode 와 같은 글자표 — 0/O, 1/I 처럼 헷갈리는 글자가 없습니다.
const CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
const CODE_LENGTH = 6;

/** 앱의 `InviteCode.normalize` 와 같은 규칙. 소문자·공백·하이픈을 받아 줍니다. */
function normalizeCode(raw) {
  const cleaned = String(raw ?? "")
    .toUpperCase()
    .split("")
    .filter((ch) => CODE_ALPHABET.includes(ch))
    .join("");
  return cleaned.length === CODE_LENGTH ? cleaned : null;
}

/**
 * 초대 코드로 짜국에 들어갑니다. 코드 검증과 멤버 등록이 **한 트랜잭션** 안에 있어
 * 클라이언트는 이 함수를 거치지 않고는 멤버가 될 수 없습니다(규칙이 직접 쓰기를 막습니다).
 *
 * 멤버 문서 필드는 앱이 만들던 것과 같습니다 — `displayName` · `owner`.
 */
exports.joinSpace = onCall(async (request) => {
  const auth = request.auth;
  if (!auth) throw new HttpsError("unauthenticated", "로그인해야 참여할 수 있습니다");

  const code = normalizeCode(request.data && request.data.code);
  if (!code) throw new HttpsError("not-found", "초대 코드가 올바르지 않습니다");

  const db = admin.firestore();
  const invite = await db.doc(`invites/${code}`).get();
  const spaceId = invite.exists ? invite.get("spaceId") : null;
  if (!spaceId) throw new HttpsError("not-found", "초대 코드가 올바르지 않습니다");

  const space = await db.doc(`spaces/${spaceId}`).get();
  if (!space.exists) throw new HttpsError("not-found", "짜국이 없습니다");

  // 애플 로그인은 토큰에 이름이 없을 수 있습니다. 앱과 같은 "?" 로 둡니다 —
  // 화면이 빈 이름을 "?" 로 그리는 것과 맞춥니다.
  const displayName = (auth.token.name || "").trim() || "?";
  const now = Math.floor(Date.now() / 1000);

  const batch = db.batch();
  batch.set(db.doc(`spaces/${spaceId}/members/${auth.uid}`), {
    displayName,
    owner: false,
  });
  batch.set(db.doc(`users/${auth.uid}/spaces/${spaceId}`), { joinedAt: now });
  await batch.commit();

  return { spaceId };
});

/**
 * 사진이 올라오면 **다른 멤버들**에게 알립니다. 올린 사람은 뺍니다 — 자기가 방금 한
 * 일을 알림으로 또 들으면 소음입니다.
 *
 * 여러 장을 한 번에 올리면 문서마다 한 번씩 불리므로, 짜국별 collapse 키로 묶어
 * 기기에서는 마지막 한 개만 남게 합니다.
 */
exports.notifyPhoto = onDocumentCreated("spaces/{spaceId}/photos/{photoId}", async (event) => {
  const { spaceId } = event.params;
  const uploadedBy = event.data && event.data.get("uploadedBy");

  const db = admin.firestore();
  const space = await db.doc(`spaces/${spaceId}`).get();
  const spaceName = (space.exists && space.get("name")) || "짜국";

  const members = await db.collection(`spaces/${spaceId}/members`).get();
  const others = members.docs.map((doc) => doc.id).filter((uid) => uid !== uploadedBy);
  if (others.length === 0) return;

  const tokens = [];
  for (const uid of others) {
    const registered = await db.collection(`users/${uid}/fcmTokens`).get();
    for (const doc of registered.docs) tokens.push({ uid, token: doc.id });
  }
  if (tokens.length === 0) return;

  const outcome = await admin.messaging().sendEachForMulticast({
    tokens: tokens.map((entry) => entry.token),
    notification: { title: spaceName, body: "새 사진이 올라왔어요" },
    android: { collapseKey: spaceId, notification: { tag: spaceId } },
    apns: {
      headers: { "apns-collapse-id": spaceId },
      payload: { aps: { sound: "default" } },
    },
    data: { spaceId },
  });

  // 지워진 기기의 토큰은 이때 알게 됩니다. 그대로 두면 보낼 때마다 또 실패합니다.
  const gone = [];
  outcome.responses.forEach((response, index) => {
    const code = response.error && response.error.code;
    if (code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-argument") {
      const entry = tokens[index];
      gone.push(db.doc(`users/${entry.uid}/fcmTokens/${entry.token}`).delete());
    }
  });
  await Promise.all(gone);
});
