/* 저장소의 추억 지도 화면을 앱에 넣을 www/ 로 복사합니다.
   앱 안에서는 map/index.html 한 장만 쓰므로 폴더 구조를 한 단계 펴고
   '../assets/' 경로를 'assets/' 로 바꿔 줍니다.
   실행: npm run build:www  (npm run sync 하면 이것까지 같이 돕니다) */
import { readFileSync, writeFileSync, mkdirSync, cpSync, rmSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repo = join(here, '..');
const www = join(here, 'www');

if (existsSync(www)) rmSync(www, { recursive: true });
mkdirSync(www, { recursive: true });

/* 공용 자산 (base.css, gate.js, lockui.js, fx.js, countries-ko.js, firebase.js …) */
cpSync(join(repo, 'assets'), join(www, 'assets'), { recursive: true });

/* 시도 한글 이름표는 map/ 밑에 있으므로 따로 */
cpSync(join(repo, 'assets', 'subdivisions-ko.js'), join(www, 'assets', 'subdivisions-ko.js'));

/* 앱 아이콘 등 map/app 자산 */
if (existsSync(join(repo, 'map', 'app'))) {
  cpSync(join(repo, 'map', 'app'), join(www, 'app'), { recursive: true });
}

let html = readFileSync(join(repo, 'map', 'index.html'), 'utf8');

/* 경로 한 단계 올리기 */
html = html.replaceAll('../assets/', 'assets/');

/* 앱에서는 필요 없는 것들 정리
   - 목록으로 돌아가는 링크 (앱은 지도 전용)
   - 서비스워커 (앱은 파일이 이미 안에 들어 있음)
   - 웹 매니페스트 */
html = html.replace(/<a class="home"[\s\S]*?<\/a>\n?/, '');
html = html.replace(/<script>\n\/\* 앱으로 설치했을 때[\s\S]*?<\/script>\n/, '');
html = html.replace(/<link rel="manifest"[^>]*>\n?/, '');

/* 앱 안에서는 사진을 항상 클라우드에서 받아야 하므로 캐시 버전 표시는 그대로 둡니다. */
writeFileSync(join(www, 'index.html'), html);

console.log('www/ 준비 완료');
console.log('  index.html   ', html.length, 'bytes');
