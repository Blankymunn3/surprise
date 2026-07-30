#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
photos/inbox/ 에 넣은 사진을 지역에 맞춰 사이트에 올리는 도구.

쓰는 법
  1) 사진 파일 이름을 '지역 이름'으로 바꾼다   예: 마포구.jpg / 강릉시.jpg / 일본.jpg / 인도네시아.png
  2) photos/inbox/ 폴더에 넣는다
  3) '사진올리기.command' 를 더블클릭

하는 일
  - 지역 이름 → 코드 변환 (국내 시군구 251곳 + 해외 국가)
  - 사진 축소·압축 (가로 1200px, JPEG)
  - photos/region-<코드>.jpg 로 저장
  - 지도 페이지의 SITE_CODES 목록 자동 갱신
  - git commit & push (아내 폰에서도 바로 보임)
"""
import json, os, re, shutil, subprocess, sys, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INBOX = os.path.join(ROOT, 'photos', 'inbox')
OUT = os.path.join(ROOT, 'photos')
CACHE = os.path.join(ROOT, 'tools', '.cache')
MAP_HTML = os.path.join(ROOT, 'map', 'index.html')

SIDO_URL = 'https://cdn.jsdelivr.net/gh/southkorea/southkorea-maps@master/kostat/2013/json/skorea_municipalities_geo_simple.json'
WORLD_URL = 'https://cdn.jsdelivr.net/gh/johan/world.geo.json@master/countries.geo.json'

# 해외 국가: 한글 이름 → ISO3
COUNTRY_KO = {
    '일본':'JPN','인도네시아':'IDN','발리':'IDN','베트남':'VNM','태국':'THA','대만':'TWN','타이완':'TWN',
    '필리핀':'PHL','중국':'CHN','싱가포르':'SGP','말레이시아':'MYS','인도':'IND','미국':'USA','캐나다':'CAN',
    '멕시코':'MEX','브라질':'BRA','프랑스':'FRA','이탈리아':'ITA','스페인':'ESP','스위스':'CHE','독일':'DEU',
    '영국':'GBR','네덜란드':'NLD','오스트리아':'AUT','체코':'CZE','크로아티아':'HRV','그리스':'GRC',
    '포르투갈':'PRT','튀르키예':'TUR','터키':'TUR','호주':'AUS','뉴질랜드':'NZL',
}

def log(msg): print(msg, flush=True)

def fetch_json(url, cache_name):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, cache_name)
    if os.path.exists(path):
        try:
            with open(path, encoding='utf-8') as f: return json.load(f)
        except Exception: pass
    log('  경계 데이터 받는 중… (%s)' % cache_name)
    with urllib.request.urlopen(url, timeout=90) as r:
        data = json.loads(r.read().decode('utf-8'))
    with open(path, 'w', encoding='utf-8') as f: json.dump(data, f)
    return data

def build_name_map():
    """지역 이름 → 코드"""
    m = {}
    sido = fetch_json(SIDO_URL, 'sido.json')
    dup = {}
    for f in sido['features']:
        nm = f['properties']['name']; code = f['properties']['code']
        dup.setdefault(nm, []).append(code)
    for nm, codes in dup.items():
        if len(codes) == 1:
            m[nm] = codes[0]
        else:   # 같은 이름(예: 강서구)이 여러 곳 → 시도 접두어로 구분
            for c in codes:
                pre = {'11':'서울','21':'부산','22':'대구','23':'인천','24':'광주','25':'대전','26':'울산',
                       '29':'세종','31':'경기','32':'강원','33':'충북','34':'충남','35':'전북','36':'전남',
                       '37':'경북','38':'경남','39':'제주'}.get(c[:2], '')
                m[pre + ' ' + nm] = c
                m[pre + nm] = c
    for ko, iso in COUNTRY_KO.items():
        m[ko] = 'C-' + iso
    return m

def optimize(src, dst, max_w=1200, quality=82):
    try:
        from PIL import Image, ImageOps
    except ImportError:
        shutil.copy(src, dst); log('    (Pillow 없어 원본 복사)'); return
    im = Image.open(src)
    im = ImageOps.exif_transpose(im)          # 폰 사진 회전 보정
    if im.mode not in ('RGB', 'L'): im = im.convert('RGB')
    if im.width > max_w:
        im = im.resize((max_w, round(im.height * max_w / im.width)), Image.LANCZOS)
    im.save(dst, 'JPEG', quality=quality, optimize=True, progressive=True)

def update_site_codes(codes):
    h = open(MAP_HTML, encoding='utf-8').read()
    arr = "var SITE_CODES=[" + ",".join("'%s'" % c for c in sorted(codes)) + "];"
    new = re.sub(r"var SITE_CODES=\[[^\]]*\];", arr, h, count=1)
    if new != h:
        open(MAP_HTML, 'w', encoding='utf-8').write(new)
        return True
    return False

def main():
    os.makedirs(INBOX, exist_ok=True)
    files = [f for f in sorted(os.listdir(INBOX))
             if not f.startswith('.') and os.path.splitext(f)[1].lower() in ('.jpg','.jpeg','.png','.heic','.webp')]
    if not files:
        log('📂 photos/inbox/ 에 사진이 없어요.')
        log('   사진 이름을 지역 이름으로 바꿔서 넣어주세요.  예: 마포구.jpg, 강릉시.jpg, 일본.jpg')
        return 0

    log('🗺️  지역 이름을 코드로 바꾸는 중…')
    name_map = build_name_map()

    ok, fail = [], []
    for f in files:
        stem = os.path.splitext(f)[0].strip()
        key = stem
        code = name_map.get(key)
        if not code:      # '서울 마포구' 처럼 공백 제거 후 재시도
            code = name_map.get(key.replace(' ', ''))
        if not code:
            fail.append((f, '지역 이름을 못 찾음')); continue
        dst = os.path.join(OUT, 'region-%s.jpg' % code)
        try:
            optimize(os.path.join(INBOX, f), dst)
            size = os.path.getsize(dst) // 1024
            log('  ✅ %-18s → %s (%dKB)' % (stem, os.path.basename(dst), size))
            ok.append(code)
            os.remove(os.path.join(INBOX, f))
        except Exception as e:
            fail.append((f, str(e)))

    for f, why in fail:
        log('  ❌ %s — %s' % (f, why))
    if fail:
        log('     (이름 예시: 마포구 / 서울 강서구 / 강릉시 / 제주시 / 일본 / 인도네시아)')

    if not ok:
        return 1

    # 이미 올려둔 지역 + 새로 올린 지역 합치기
    existing = set()
    for f in os.listdir(OUT):
        m = re.match(r'region-(.+)\.jpg$', f)
        if m: existing.add(m.group(1))
    existing.update(ok)
    if update_site_codes(existing):
        log('📝 지도 페이지 목록 갱신 (%d곳)' % len(existing))

    log('🚀 사이트에 올리는 중…')
    subprocess.run(['git', 'add', '-A'], cwd=ROOT, check=False)
    subprocess.run(['git', '-c', 'user.email=upload@local', '-c', 'user.name=photo-upload',
                    'commit', '-q', '-m', '추억 지도 사진 추가 (%d곳)' % len(ok)], cwd=ROOT, check=False)
    r = subprocess.run(['git', 'push', 'origin', 'main'], cwd=ROOT, capture_output=True, text=True)
    if r.returncode == 0:
        log('')
        log('🎉 완료! 1~2분 뒤 아내 폰에서도 보여요')
        log('   https://blankymunn3.github.io/surprise/map')
    else:
        log('⚠️ 업로드 실패: %s' % (r.stderr.strip()[:200]))
        log('   터미널에서 `git push` 를 한 번 해보세요.')
    return 0

if __name__ == '__main__':
    sys.exit(main())
