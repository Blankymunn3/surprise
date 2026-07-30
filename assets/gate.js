/* 사이트 잠금 — 숫자 키패드로 4자리 입력.
   ⚠️ 정적 사이트라 소스를 뜯어보면 우회 가능한 '대문 잠금' 수준입니다.
      비밀번호를 바꾸려면 아래 HASH를 새 값의 SHA-256으로 교체하세요. */
(function () {
  var KEY = 'surprise.unlock.v1';
  var HASH = 'd83036ffb5b49a153bc391fd12b4a10df06bbb200f39fed97c331b03b6833ce7'; // 0412
  var HINT = '우리 기념일 4자리 💕';
  var LEN = 4;

  try { if (localStorage.getItem(KEY) === '1') return; } catch (e) {}

  var st = document.createElement('style');
  st.id = 'gate-style';
  st.textContent =
    'body>*{visibility:hidden!important}' +
    '#gate,#gate *{visibility:visible!important}' +
    '#gate{position:fixed;inset:0;z-index:99999;display:grid;place-items:center;padding:18px;' +
    'background:radial-gradient(120% 90% at 15% 0%,#ffe3ee,transparent 55%),' +
    'radial-gradient(120% 100% at 100% 100%,#ffd9c9,transparent 55%),' +
    'linear-gradient(160deg,#fff0f5 0%,#ffc2d6 60%,#ffb3c9 100%);' +
    'font-family:"Jua","Apple SD Gothic Neo",system-ui,sans-serif;color:#5a2740;' +
    '-webkit-tap-highlight-color:transparent;user-select:none;-webkit-user-select:none}' +
    '#gate .box{width:min(92vw,340px);background:rgba(255,255,255,.74);border:1px solid rgba(255,255,255,.9);' +
    'border-radius:30px;padding:26px 22px 22px;text-align:center;box-shadow:0 30px 70px -30px rgba(190,30,80,.5);' +
    'backdrop-filter:blur(18px);-webkit-backdrop-filter:blur(18px)}' +
    '#gate .lock{font-size:44px;line-height:1}' +
    '#gate h2{font-size:23px;margin:10px 0 3px;color:#e11d5b}' +
    '#gate p{font-family:"Gaegu","Apple SD Gothic Neo",sans-serif;font-size:15px;color:#96566f;margin:0 0 16px}' +
    '#gate .dots{display:flex;gap:14px;justify-content:center;margin-bottom:6px}' +
    '#gate .dot{width:16px;height:16px;border-radius:50%;background:#fff;border:2px solid #ffc2d6;transition:.18s}' +
    '#gate .dot.on{background:#e11d5b;border-color:#e11d5b;transform:scale(1.12)}' +
    '#gate .msg{min-height:22px;font-family:"Gaegu",sans-serif;font-size:14.5px;color:#e11d5b;margin-bottom:6px}' +
    '#gate .pad{display:grid;grid-template-columns:repeat(3,1fr);gap:9px}' +
    '#gate .k{font-family:inherit;font-size:24px;color:#5a2740;background:#fff;border:1px solid #ffdce7;' +
    'border-radius:18px;padding:14px 0;cursor:pointer;box-shadow:0 6px 14px -8px rgba(190,30,80,.5);' +
    'transition:transform .08s,background .15s}' +
    '#gate .k:active{transform:scale(.94);background:#ffe3ee}' +
    '#gate .k.fn{font-size:16px;color:#96566f;background:rgba(255,255,255,.7)}' +
    '#gate .k.ok{color:#fff;background:linear-gradient(135deg,#ff4d78,#e11d5b);border:none;' +
    'box-shadow:0 10px 22px -10px rgba(225,29,91,.8)}' +
    '#gate .shake{animation:gshake .4s}' +
    '@keyframes gshake{0%,100%{transform:translateX(0)}25%{transform:translateX(-8px)}75%{transform:translateX(8px)}}';
  (document.head || document.documentElement).appendChild(st);

  function sha256(txt) {
    if (window.crypto && crypto.subtle && window.TextEncoder && location.protocol !== 'file:') {
      return crypto.subtle.digest('SHA-256', new TextEncoder().encode(txt)).then(function (buf) {
        var a = Array.prototype.slice.call(new Uint8Array(buf));
        return a.map(function (b) { return ('00' + b.toString(16)).slice(-2); }).join('');
      }).catch(function () { return null; });
    }
    return Promise.resolve(null);
  }

  function build() {
    var pin = '';
    var g = document.createElement('div');
    g.id = 'gate';

    var keys = ['1','2','3','4','5','6','7','8','9','del','0','ok'];
    var padHtml = '';
    for (var i = 0; i < keys.length; i++) {
      var k = keys[i];
      if (k === 'del') padHtml += '<button class="k fn" data-k="del" type="button">지우기</button>';
      else if (k === 'ok') padHtml += '<button class="k ok" data-k="ok" type="button">열기</button>';
      else padHtml += '<button class="k" data-k="' + k + '" type="button">' + k + '</button>';
    }

    g.innerHTML =
      '<div class="box">' +
      '<div class="lock">🔒</div>' +
      '<h2>비밀번호를 입력해줘</h2>' +
      '<p>' + HINT + '</p>' +
      '<div class="dots" id="gdots"><span class="dot"></span><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>' +
      '<div class="msg" id="gmsg"></div>' +
      '<div class="pad" id="gpad">' + padHtml + '</div>' +
      '</div>';
    document.body.appendChild(g);

    var dots = g.querySelectorAll('.dot');
    var msg = document.getElementById('gmsg');
    var box = g.querySelector('.box');

    function render() {
      for (var i = 0; i < dots.length; i++) {
        if (i < pin.length) dots[i].className = 'dot on';
        else dots[i].className = 'dot';
      }
    }

    function open() {
      try { localStorage.setItem(KEY, '1'); } catch (e) {}
      g.remove();
      var s = document.getElementById('gate-style');
      if (s) s.remove();
    }

    function fail() {
      msg.textContent = '앗, 아니야. 다시 생각해봐 😝';
      box.classList.remove('shake'); void box.offsetWidth; box.classList.add('shake');
      pin = ''; render();
    }

    function check() {
      sha256(pin).then(function (hex) {
        if (hex ? (hex === HASH) : (pin === '0412')) open();
        else fail();
      });
    }

    function push(d) {
      if (pin.length >= LEN) return;
      msg.textContent = '';
      pin += d; render();
      if (pin.length === LEN) setTimeout(check, 140);
    }

    document.getElementById('gpad').addEventListener('click', function (e) {
      var b = e.target.closest ? e.target.closest('[data-k]') : null;
      if (!b) return;
      var k = b.getAttribute('data-k');
      if (k === 'del') { pin = pin.slice(0, -1); msg.textContent = ''; render(); }
      else if (k === 'ok') { if (pin.length === LEN) check(); else { msg.textContent = LEN + '자리를 눌러줘 🙂'; } }
      else push(k);
    });

    // 컴퓨터에서는 키보드로도 입력
    document.addEventListener('keydown', function (e) {
      if (!document.getElementById('gate')) return;
      if (e.key >= '0' && e.key <= '9') push(e.key);
      else if (e.key === 'Backspace') { pin = pin.slice(0, -1); render(); }
      else if (e.key === 'Enter' && pin.length === LEN) check();
    });

    render();
  }

  if (document.body) build();
  else document.addEventListener('DOMContentLoaded', build);
})();
