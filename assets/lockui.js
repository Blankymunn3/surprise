/* 앱처럼 쓰이게 — 우클릭 메뉴·끌기·화면 확대 제스처 막기
   ⚠️ 이건 '실수로 눌리는 것'을 막는 편의 기능입니다. 개발자도구나 소스보기로는
      얼마든지 우회되니 사진·내용을 지키는 보안 수단이 아닙니다.
   지도(#map) 안에서는 지도가 직접 드래그·확대를 처리하므로 건드리지 않습니다. */
(function () {
  function inMap(el) {
    return !!(el && el.closest && el.closest('#map'));
  }
  function block(e) { e.preventDefault(); }

  /* 우클릭 메뉴 + 모바일 길게 눌러 나오는 메뉴 */
  document.addEventListener('contextmenu', block, { passive: false });

  /* 이미지·텍스트 끌어서 빼내기 */
  document.addEventListener('dragstart', block, { passive: false });

  /* 사파리 핀치 확대 (지도 안은 지도가 처리) */
  ['gesturestart', 'gesturechange', 'gestureend'].forEach(function (t) {
    document.addEventListener(t, function (e) {
      if (!inMap(e.target)) e.preventDefault();
    }, { passive: false });
  });

  /* 데스크톱 Ctrl(⌘)+휠 확대 — 지도 휠 확대는 지도가 따로 처리 */
  document.addEventListener('wheel', function (e) {
    if ((e.ctrlKey || e.metaKey) && !inMap(e.target)) e.preventDefault();
  }, { passive: false });

  /* 두 손가락 이상은 확대 시도로 보고 무시 (지도 제외) */
  document.addEventListener('touchmove', function (e) {
    if (e.touches && e.touches.length > 1 && !inMap(e.target)) e.preventDefault();
  }, { passive: false });

  /* 두 번 눌러 확대되는 건 base.css 의 touch-action 이 막습니다.
     여기서 touchend 를 preventDefault 하면 안 됩니다 —
     빠르게 연달아 누를 때 click 이 취소돼서 잠금화면 키패드가 씹힙니다. */
})();
