/* 공통 효과 — 폭죽(confetti) + 하트 풍선. window.FX 로 노출.
   사용법: <script src="../assets/fx.js"></script> 를 페이지 스크립트보다 먼저 넣으면
   FX.burst(x,y,n) / FX.heart() / FX.party(ms) 사용 가능. 레이어는 자동 생성됨. */
window.FX = (function () {
  var reduce = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  // 효과 레이어 자동 생성
  var hearts = document.getElementById("hearts");
  if (!hearts) { hearts = document.createElement("div"); hearts.id = "hearts"; hearts.setAttribute("aria-hidden", "true"); document.body.appendChild(hearts); }
  var canvas = document.getElementById("confetti");
  if (!canvas) { canvas = document.createElement("canvas"); canvas.id = "confetti"; canvas.setAttribute("aria-hidden", "true"); document.body.appendChild(canvas); }

  var ctx = canvas.getContext("2d");
  var DPR = Math.min(window.devicePixelRatio || 1, 2);
  var parts = [], raf = null;
  var COLORS = ["#ff4d78", "#e11d5b", "#ff8fab", "#ffd166", "#ff5da2", "#ffffff", "#ff9770"];
  var EMOJI = ["❤️","💕","💖","💘","💗","🩷","😍","🥰","💞","💓"];

  function size() {
    canvas.width = window.innerWidth * DPR;
    canvas.height = window.innerHeight * DPR;
    canvas.style.width = window.innerWidth + "px";
    canvas.style.height = window.innerHeight + "px";
    ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
  }
  size();
  window.addEventListener("resize", size);

  function burst(x, y, amount) {
    var n = reduce ? Math.min(amount, 40) : amount;
    for (var i = 0; i < n; i++) {
      var a = Math.random() * Math.PI * 2, s = 4 + Math.random() * 11;
      parts.push({
        x: x, y: y, vx: Math.cos(a) * s, vy: Math.sin(a) * s - 6,
        g: 0.16 + Math.random() * 0.12, size: 7 + Math.random() * 10,
        rot: Math.random() * Math.PI, vr: (Math.random() - 0.5) * 0.3,
        color: COLORS[(Math.random() * COLORS.length) | 0],
        heart: Math.random() < 0.4, life: 0, max: 120 + Math.random() * 60
      });
    }
    if (!raf) raf = requestAnimationFrame(tick);
  }

  function tick() {
    ctx.clearRect(0, 0, window.innerWidth, window.innerHeight);
    for (var i = parts.length - 1; i >= 0; i--) {
      var p = parts[i];
      p.vy += p.g; p.x += p.vx; p.y += p.vy; p.vx *= 0.995; p.rot += p.vr; p.life++;
      ctx.save();
      ctx.globalAlpha = Math.max(0, 1 - p.life / p.max);
      ctx.translate(p.x, p.y); ctx.rotate(p.rot); ctx.fillStyle = p.color;
      if (p.heart) { ctx.font = p.size * 1.7 + "px serif"; ctx.textAlign = "center"; ctx.textBaseline = "middle"; ctx.fillText("♥", 0, 0); }
      else ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 0.6);
      ctx.restore();
      if (p.life >= p.max || p.y > window.innerHeight + 40) parts.splice(i, 1);
    }
    if (parts.length) raf = requestAnimationFrame(tick);
    else { raf = null; ctx.clearRect(0, 0, window.innerWidth, window.innerHeight); }
  }

  function heart() {
    var s = document.createElement("div");
    s.className = "heart-b";
    s.textContent = EMOJI[(Math.random() * EMOJI.length) | 0];
    s.style.left = (Math.random() * 100) + "%";
    s.style.fontSize = (26 + Math.random() * 42) + "px";
    s.style.setProperty("--dur", (4.5 + Math.random() * 4) + "s");
    s.style.setProperty("--sway", (Math.random() * 180 - 90) + "px");
    s.style.setProperty("--rot", (Math.random() * 120 - 60) + "deg");
    hearts.appendChild(s);
    s.addEventListener("animationend", function () { s.remove(); });
  }

  var timer = null;
  function party(ms) {
    var W = window.innerWidth, H = window.innerHeight;
    burst(W / 2, H * 0.4, 170);
    setTimeout(function () { burst(W * 0.2, H * 0.5, 80); }, 220);
    setTimeout(function () { burst(W * 0.8, H * 0.5, 80); }, 420);
    for (var i = 0; i < 20; i++) setTimeout(heart, i * 90);
    if (timer) clearInterval(timer);
    timer = setInterval(function () { heart(); if (Math.random() < 0.5) heart(); }, 320);
    setTimeout(function () { clearInterval(timer); timer = null; }, ms || 6000);
  }

  return { burst: burst, heart: heart, party: party, emoji: EMOJI, reduce: reduce };
})();
