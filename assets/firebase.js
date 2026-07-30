/* 클라우드 사진 저장 — Firebase Storage (REST 직접 호출)
   ─────────────────────────────────────────────────────────────
   Firebase SDK를 받지 않고 Storage REST API만 씁니다.
   폰에서 100KB짜리 SDK를 매번 안 받아도 되고, 코드도 짧습니다.

   ⚠️ 이 방식은 Storage 규칙에서 regions/ 를 '공개 읽기'로 열어둔 전제입니다.
      (저장소 루트 storage.rules 참고 — 사진 주소를 아는 사람은 볼 수 있음)
   설정 방법은 FIREBASE.md 참고. */
(function () {
  var CONFIG = {
    storageBucket: 'our-surprise.firebasestorage.app',   // 실제로 쓰는 값
    projectId: 'our-surprise',                           // 참고용
    apiKey: 'AIzaSyAvDJkTuNWs6PqoEvMT0w1BmyFZT_gZNXs'    // 참고용(웹 공개 키, 비밀 아님)
  };

  var DIR = 'regions';                        // 지역 사진이 쌓이는 폴더
  var CACHE_KEY = 'surprise.cloud.index';     // {코드: 주소} 마지막 목록
  var configured = !!CONFIG.storageBucket;

  function base() {
    return 'https://firebasestorage.googleapis.com/v0/b/' +
           encodeURIComponent(CONFIG.storageBucket) + '/o';
  }
  function safe(code) { return String(code).replace(/[^A-Za-z0-9_-]/g, '_'); }
  function pathOf(code) { return DIR + '/' + safe(code) + '.jpg'; }
  function codeOf(name) { return name.replace(/^.*\//, '').replace(/\.jpg$/i, ''); }
  function objUrl(p) { return base() + '/' + encodeURIComponent(p); }
  function mediaUrl(p) { return objUrl(p) + '?alt=media'; }

  function cached() {
    try { return JSON.parse(localStorage.getItem(CACHE_KEY) || '{}') || {}; }
    catch (e) { return {}; }
  }
  function putCache(map) {
    try { localStorage.setItem(CACHE_KEY, JSON.stringify(map)); } catch (e) {}
  }

  function need() {
    return configured ? null : Promise.reject(new Error('not-configured'));
  }

  /* 신호가 약한 곳에서 하염없이 기다리지 않도록 시간 제한을 둡니다.
     시간이 넘으면 실패로 처리되고, 부르는 쪽에서 기기 저장으로 넘어갑니다. */
  function ask(url, opts, ms) {
    opts = opts || {};
    var ctl = (typeof AbortController !== 'undefined') ? new AbortController() : null;
    if (ctl) opts.signal = ctl.signal;
    var timer = setTimeout(function () { if (ctl) ctl.abort(); }, ms || 15000);
    return fetch(url, opts).then(function (r) {
      clearTimeout(timer); return r;
    }, function (e) {
      clearTimeout(timer);
      throw new Error(e && e.name === 'AbortError' ? 'timeout' : (e.message || 'network'));
    });
  }

  /* 올라와 있는 사진 전체 목록 → {코드: 주소} (여러 페이지도 이어서 받음) */
  function list() {
    var stop = need(); if (stop) return stop;
    var map = {};
    function page(token) {
      /* delimiter 를 반드시 같이 보냅니다.
         이게 없으면 Firebase가 '버킷 전체 목록'으로 보고 규칙에서 막습니다(403).
         붙이면 'regions 폴더 목록'이 되어 storage.rules 의 match /regions 가 적용됩니다. */
      var url = base() + '?prefix=' + encodeURIComponent(DIR + '/') +
                '&delimiter=' + encodeURIComponent('/') + '&maxResults=1000' +
                (token ? '&pageToken=' + encodeURIComponent(token) : '');
      return ask(url, null, 15000).then(function (r) {
        if (!r.ok) throw new Error('list ' + r.status);
        return r.json();
      }).then(function (d) {
        (d.items || []).forEach(function (it) {
          if (/\.jpg$/i.test(it.name)) map[codeOf(it.name)] = mediaUrl(it.name);
        });
        return d.nextPageToken ? page(d.nextPageToken) : map;
      });
    }
    return page(null).then(function (m) { putCache(m); return m; });
  }

  /* 사진 한 장 올리기 → 주소 */
  function upload(code, blob) {
    var stop = need(); if (stop) return stop;
    var p = pathOf(code);
    return ask(base() + '?name=' + encodeURIComponent(p), {
      method: 'POST',
      headers: { 'Content-Type': 'image/jpeg' },
      body: blob
    }, 25000).then(function (r) {
      if (!r.ok) throw new Error('upload ' + r.status);
      var url = mediaUrl(p);
      var map = cached(); map[safe(code)] = url; putCache(map);
      return url;
    });
  }

  /* 사진 한 장 지우기 (이미 없어도 성공으로 처리) */
  function remove(code) {
    var stop = need(); if (stop) return stop;
    return ask(objUrl(pathOf(code)), { method: 'DELETE' }, 15000).then(function (r) {
      if (!r.ok && r.status !== 404) throw new Error('delete ' + r.status);
      var map = cached(); delete map[safe(code)]; putCache(map);
    });
  }

  window.CloudPhotos = {
    configured: configured,   // 버킷이 설정돼 있는지
    cached: cached,           // 마지막으로 본 목록(즉시 사용, 네트워크 없음)
    list: list,
    upload: upload,
    remove: remove,
    key: safe                 // 지역 코드 → 저장소에서 쓰는 키
  };
})();
