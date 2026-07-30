/* 클라우드 사진 저장 — Firebase Storage
   ─────────────────────────────────────────────────────────────
   ① Firebase 콘솔에서 프로젝트를 만들고 Storage를 켠 뒤,
      '웹 앱 추가'에서 나오는 값을 아래 CONFIG에 그대로 붙여 넣으세요.
   ② 규칙(Rules)은 저장소 루트의 storage.rules 를 복사해 넣으면 됩니다.
   ③ CONFIG가 비어 있으면 이 파일은 아무 것도 하지 않고,
      각 페이지는 예전처럼 localStorage(내 기기에만 저장)로 동작합니다.
   자세한 순서는 FIREBASE.md 참고. */
(function () {
  var CONFIG = {
    apiKey: '',            // 예: 'AIzaSy...'
    authDomain: '',        // 예: 'our-surprise.firebaseapp.com'
    projectId: '',         // 예: 'our-surprise'
    storageBucket: '',     // 예: 'our-surprise.firebasestorage.app'
    appId: ''              // 예: '1:1234567890:web:abcdef'
  };

  var DIR = 'regions';                                  // 지역 사진이 쌓이는 폴더
  var SDK = 'https://www.gstatic.com/firebasejs/10.12.2/';
  var CACHE_KEY = 'surprise.cloud.index';               // {코드: 다운로드주소} 마지막 목록

  var configured = !!(CONFIG.apiKey && CONFIG.storageBucket);
  var loading = null, storage = null;

  function safe(code) { return String(code).replace(/[^A-Za-z0-9_-]/g, '_'); }
  function pathOf(code) { return DIR + '/' + safe(code) + '.jpg'; }
  function codeOf(name) { return name.replace(/\.jpg$/i, ''); }

  function cached() {
    try { return JSON.parse(localStorage.getItem(CACHE_KEY) || '{}') || {}; }
    catch (e) { return {}; }
  }
  function putCache(map) {
    try { localStorage.setItem(CACHE_KEY, JSON.stringify(map)); } catch (e) {}
  }

  function script(src) {
    return new Promise(function (ok, no) {
      var s = document.createElement('script');
      s.src = src; s.async = true;
      s.onload = ok;
      s.onerror = function () { no(new Error('SDK 로드 실패: ' + src)); };
      document.head.appendChild(s);
    });
  }

  /* Firebase SDK를 처음 필요할 때 한 번만 불러옵니다 */
  function ready() {
    if (!configured) return Promise.reject(new Error('not-configured'));
    if (loading) return loading;
    loading = script(SDK + 'firebase-app-compat.js')
      .then(function () { return script(SDK + 'firebase-storage-compat.js'); })
      .then(function () {
        if (!firebase.apps.length) firebase.initializeApp(CONFIG);
        storage = firebase.storage();
        return storage;
      })
      .catch(function (e) { loading = null; throw e; });
    return loading;
  }

  /* 올라와 있는 사진 전체 목록 → {코드: 주소} */
  function list() {
    return ready().then(function (st) {
      return st.ref(DIR).listAll();
    }).then(function (res) {
      return Promise.all(res.items.map(function (item) {
        return item.getDownloadURL().then(
          function (url) { return { code: codeOf(item.name), url: url }; },
          function () { return null; }
        );
      }));
    }).then(function (rows) {
      var map = {};
      rows.forEach(function (r) { if (r) map[r.code] = r.url; });
      putCache(map);
      return map;
    });
  }

  /* 사진 한 장 올리기 → 다운로드 주소 */
  function upload(code, blob) {
    return ready().then(function (st) {
      return st.ref(pathOf(code)).put(blob, {
        contentType: 'image/jpeg',
        cacheControl: 'public,max-age=31536000'
      });
    }).then(function (snap) {
      return snap.ref.getDownloadURL();
    }).then(function (url) {
      var map = cached(); map[safe(code)] = url; putCache(map);
      return url;
    });
  }

  /* 사진 한 장 지우기 (이미 없어도 성공으로 처리) */
  function remove(code) {
    return ready().then(function (st) {
      return st.ref(pathOf(code)).delete().catch(function (e) {
        if (e && e.code === 'storage/object-not-found') return;
        throw e;
      });
    }).then(function () {
      var map = cached(); delete map[safe(code)]; putCache(map);
    });
  }

  window.CloudPhotos = {
    configured: configured,   // config를 채웠는지
    cached: cached,           // 마지막으로 본 목록(즉시 사용, 네트워크 없음)
    list: list,
    upload: upload,
    remove: remove,
    key: safe                 // 지역 코드 → 저장소에서 쓰는 키
  };
})();
