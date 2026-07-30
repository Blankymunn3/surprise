/* 사이트 잠금 — 비밀번호 4자리(기념일).
   ⚠️ 정적 사이트라 소스를 뜯어보면 우회 가능한 '대문 잠금' 수준입니다.
      진짜 비밀은 올리지 마세요. 비밀번호를 바꾸려면 아래 HASH를 새 값의 SHA-256으로 교체. */
(function () {
  var KEY = 'surprise.unlock.v1';
  var HASH = 'd83036ffb5b49a153bc391fd12b4a10df06bbb200f39fed97c331b03b6833ce7'; // 0412
  var HINT = '우리 기념일 4자리 💕';

  try { if (localStorage.getItem(KEY) === '1') return; } catch (e) {}

  // 잠금 해제 전에는 내용이 안 보이게
  var st = document.createElement('style');
  st.id = 'gate-style';
  st.textContent =
    'body>*{visibility:hidden!important}' +
    '#gate,#gate *{visibility:visible!important}' +
    '#gate{position:fixed;inset:0;z-index:99999;display:grid;place-items:center;padding:24px;' +
    'background:radial-gradient(120% 90% at 15% 0%,#ffe3ee,transparent 55%),' +
    'radial-gradient(120% 100% at 100% 100%,#ffd9c9,transparent 55%),' +
    'linear-gradient(160deg,#fff0f5 0%,#ffc2d6 60%,#ffb3c9 100%);' +
    'font-family:"Jua","Apple SD Gothic Neo",system-ui,sans-serif;color:#5a2740}' +
    '#gate .box{width:min(92vw,380px);background:rgba(255,255,255,.72);border:1px solid rgba(255,255,255,.9);' +
    'border-radius:30px;padding:34px 26px;text-align:center;box-shadow:0 30px 70px -30px rgba(190,30,80,.5);' +
    'backdrop-filter:blur(18px);-webkit-backdrop-filter:blur(18px)}' +
    '#gate .lock{font-size:52px;line-height:1}' +
    '#gate h2{font-size:26px;margin:12px 0 4px;color:#e11d5b}' +
    '#gate p{font-family:"Gaegu","Apple SD Gothic Neo",sans-serif;font-size:16px;color:#96566f;margin:0 0 20px}' +
    '#gate input{width:190px;font-family:inherit;font-size:30px;letter-spacing:.4em;text-align:center;' +
    'padding:12px 10px 12px 20px;border-radius:16px;border:1px solid #ffc2d6;background:#fff;color:#5a2740;outline:none}' +
    '#gate input:focus{border-color:#ff4d78}' +
    '#gate button{margin-top:16px;display:block;width:100%;font-family:inherit;font-size:19px;color:#fff;border:none;' +
    'cursor:pointer;border-radius:999px;padding:14px 0;background:linear-gradient(135deg,#ff4d78,#e11d5b);' +
    'box-shadow:0 14px 30px -10px rgba(225,29,91,.7)}' +
    '#gate button:active{transform:scale(.97)}' +
    '#gate .msg{min-height:22px;margin-top:12px;font-family:"Gaegu",sans-serif;font-size:15px;color:#e11d5b}' +
    '#gate .shake{animation:gshake .4s}' +
    '@keyframes gshake{0%,100%{transform:translateX(0)}25%{transform:translateX(-8px)}75%{transform:translateX(8px)}}';
  (document.head || document.documentElement).appendChild(st);

  function sha256(txt) {
    if (window.crypto && crypto.subtle && window.TextEncoder) {
      return crypto.subtle.digest('SHA-256', new TextEncoder().encode(txt)).then(function (buf) {
        var a = Array.prototype.slice.call(new Uint8Array(buf));
        return a.map(function (b) { return ('00' + b.toString(16)).slice(-2); }).join('');
      });
    }
    return Promise.resolve(null);   // 구형 브라우저: 아래에서 평문 비교로 폴백
  }

  function build() {
    var g = document.createElement('div');
    g.id = 'gate';
    g.innerHTML =
      '<div class="box">' +
      '<div class="lock">🔒</div>' +
      '<h2>비밀번호를 입력해줘</h2>' +
      '<p>' + HINT + '</p>' +
      '<input id="gpw" type="tel" inputmode="numeric" maxlength="4" autocomplete="off" placeholder="••••" />' +
      '<button id="gbtn" type="button">열기 💗</button>' +
      '<div class="msg" id="gmsg"></div>' +
      '</div>';
    document.body.appendChild(g);

    var input = document.getElementById('gpw');
    var msg = document.getElementById('gmsg');
    var box = g.querySelector('.box');
    setTimeout(function () { input.focus(); }, 200);

    function open() {
      try { localStorage.setItem(KEY, '1'); } catch (e) {}
      g.remove();
      var s = document.getElementById('gate-style');
      if (s) s.remove();
    }

    function tryOpen() {
      var v = (input.value || '').trim();
      if (!v) { return; }
      sha256(v).then(function (hex) {
        var ok = hex ? (hex === HASH) : (v === '0412');
        if (ok) { open(); return; }
        msg.textContent = '앗, 아니야. 다시 생각해봐 😝';
        box.classList.remove('shake'); void box.offsetWidth; box.classList.add('shake');
        input.value = ''; input.focus();
      });
    }

    document.getElementById('gbtn').addEventListener('click', tryOpen);
    input.addEventListener('keydown', function (e) { if (e.key === 'Enter') tryOpen(); });
    input.addEventListener('input', function () { if (input.value.length === 4) tryOpen(); });
  }

  if (document.body) build();
  else document.addEventListener('DOMContentLoaded', build);
})();
