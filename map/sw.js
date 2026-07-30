/* 추억 지도 앱(PWA) 오프라인 캐시
   ─────────────────────────────────────────────────────────────
   - 앱 껍데기(화면·스크립트·아이콘)는 미리 받아두고
   - 지도 경계 데이터·지도 타일은 쓰면서 캐시
   - 사진(Firebase Storage)은 캐시하지 않습니다. 항상 최신이어야 하고
     상대 폰에서 새로 올린 사진이 안 보이면 안 되니까요.
   파일을 고칠 때는 아래 VER 을 올려야 이전 캐시가 정리됩니다. */
var VER = 'v1';
var SHELL = 'shell-' + VER;
var RUNTIME = 'runtime-' + VER;
var TILES = 'tiles-' + VER;
var TILE_MAX = 400;                      // 타일은 이 개수까지만 보관

var SHELL_FILES = [
  './',
  '../assets/base.css?v=3',
  '../assets/gate.js?v=3',
  '../assets/lockui.js?v=3',
  '../assets/fx.js?v=3',
  '../assets/countries-ko.js?v=3',
  '../assets/firebase.js?v=3',
  './manifest.webmanifest',
  './app/icon-192.png',
  './app/icon-512.png',
  './app/apple-touch-icon.png'
];

self.addEventListener('install', function (e) {
  e.waitUntil(
    caches.open(SHELL).then(function (c) {
      /* 하나가 실패해도 나머지는 받도록 개별 처리 */
      return Promise.all(SHELL_FILES.map(function (u) {
        return c.add(new Request(u, { cache: 'reload' })).catch(function () {});
      }));
    }).then(function () { return self.skipWaiting(); })
  );
});

self.addEventListener('activate', function (e) {
  e.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(keys.map(function (k) {
        if (k !== SHELL && k !== RUNTIME && k !== TILES) return caches.delete(k);
      }));
    }).then(function () { return self.clients.claim(); })
  );
});

/* 타일 캐시가 너무 커지지 않게 오래된 것부터 정리 */
function trim(name, max) {
  caches.open(name).then(function (c) {
    c.keys().then(function (keys) {
      if (keys.length <= max) return;
      for (var i = 0; i < keys.length - max; i++) c.delete(keys[i]);
    });
  });
}

self.addEventListener('fetch', function (e) {
  var req = e.request;
  if (req.method !== 'GET') return;

  var url;
  try { url = new URL(req.url); } catch (err) { return; }

  /* 사진은 건드리지 않음 — 항상 서버에서 최신으로 */
  if (url.hostname === 'firebasestorage.googleapis.com') return;

  /* 지도 타일: 캐시 먼저, 없으면 받아서 저장 */
  if (url.hostname.indexOf('tile.openstreetmap.org') >= 0) {
    e.respondWith(
      caches.open(TILES).then(function (c) {
        return c.match(req).then(function (hit) {
          if (hit) return hit;
          return fetch(req).then(function (res) {
            if (res && res.status === 200) { c.put(req, res.clone()); trim(TILES, TILE_MAX); }
            return res;
          });
        });
      })
    );
    return;
  }

  /* 화면 이동: 네트워크 먼저(업데이트 반영), 안 되면 캐시된 화면 */
  if (req.mode === 'navigate') {
    e.respondWith(
      fetch(req).then(function (res) {
        var copy = res.clone();
        caches.open(SHELL).then(function (c) { c.put('./', copy); });
        return res;
      }).catch(function () {
        return caches.match('./', { ignoreSearch: true });
      })
    );
    return;
  }

  /* 나머지(스크립트·CSS·경계 데이터): 캐시를 바로 주고 뒤에서 갱신 */
  e.respondWith(
    caches.match(req).then(function (hit) {
      var net = fetch(req).then(function (res) {
        if (res && res.status === 200 && (url.protocol === 'http:' || url.protocol === 'https:')) {
          var copy = res.clone();
          caches.open(RUNTIME).then(function (c) { c.put(req, copy); });
        }
        return res;
      }).catch(function () { return hit; });
      return hit || net;
    })
  );
});
