package kr.surprise.memorymap.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * **시험용 토큰 — 패미컴 컨트롤러 스타일.**
 *
 * 지금 앱의 기준은 [MemoryColors] (웜 그레이 + 잉크 + 레드, 모서리 0) 입니다.
 * 이 파일은 그것과 **나란히** 두는 다른 한 벌이고, 짜국 목록 한 화면에서만 씁니다
 * (`SpaceListScreen` 의 `PLASTIC_TRIAL`). 채택되지 않으면 이 파일과 그 화면만 지우면 됩니다.
 *
 * 색은 NES 컨트롤러 실물에서 땄습니다 — 회색 플라스틱 몸통, 검정 페이스플레이트,
 * 빨간 A·B 버튼, 검은 십자키.
 */
object PlasticColors {
    /** 몸통 플라스틱. 화면 바탕입니다. */
    val Body = Color(0xFFDCD9D3)
    /** 위·왼쪽 베벨 — 빛 받는 쪽 */
    val BodyHi = Color(0xFFF0EEEA)
    /** 아래·오른쪽 베벨 — 그늘 지는 쪽 */
    val BodyLo = Color(0xFFBEBBB4)
    /** 기기가 바닥에 드리우는 그림자 */
    val BodySh = Color(0xFF9E9B94)

    /** 검정 페이스플레이트. 사진이 놓이는 판입니다. */
    val Plate = Color(0xFF3B3B3B)
    val PlateHi = Color(0xFF4A4A4A)
    val PlateLo = Color(0xFF262626)

    /** 몸통에 새긴 회색 줄무늬 */
    val Trim = Color(0xFF9C9C9C)
    val TrimLo = Color(0xFF7E7E7E)

    /** A·B 버튼의 빨강. 주 동작에만 씁니다. */
    val Red = Color(0xFFD8342A)
    val RedHi = Color(0xFFE85A4C)
    val RedLo = Color(0xFF9E1F17)

    /** 십자키·고무 버튼의 검정 */
    val Ink = Color(0xFF1B1B1B)
    val Rubber = Color(0xFF3A3A3A)
    val RubberHi = Color(0xFF4C4C4C)
    val OnRubber = Color(0xFFC8C5C0)

    /** 검정 판 위의 글자 */
    val OnPlate = Color(0xFFDCD9D3)
    val OnPlateDim = Color(0xFF9E9B96)
    val OnRed = Color(0xFFFFFFFF)
}

/** 이 스타일에서는 모서리가 **둥급니다** — 지금 기준(모서리 0)과 정반대입니다. */
object PlasticShapes {
    val Device = RoundedCornerShape(10.dp)
    /** 버튼을 감싼 사각 하우징. 사진 액자도 이것입니다. */
    val Housing = RoundedCornerShape(5.dp)
    /** 몸통에 끼운 화면 */
    val Screen = RoundedCornerShape(4.dp)
    /** 고무 알약 (SELECT · START) */
    val Pill = RoundedCornerShape(50)
    val Knob = RoundedCornerShape(4.dp)
    val Chip = RoundedCornerShape(3.dp)
}

/**
 * **이 스타일에만 있는 치수.**
 *
 * 나머지 여백은 앱의 [Space] 단(4·8·12·16·20·24·32)을 그대로 씁니다 —
 * 화면 하나 때문에 여백 체계를 새로 만들지 않습니다. 여기 있는 것은 그 단으로는
 * 표현되지 않는, **형태가 뜻을 갖는** 값들뿐입니다.
 */
object PlasticSize {
    /** 하우징이 사진을 감싸는 두께. 이게 곧 액자 테의 굵기입니다. */
    val HousingInset = 5.dp
    /** 하우징이 버튼을 감싸는 두께 */
    val ButtonInset = 7.dp
    /** 고무 알약과 A 버튼의 높이. **둘이 같아야** 나란히 섰을 때 어긋나지 않습니다. */
    val Button = 46.dp
    /** 카드 사진 높이 */
    val Photo = 108.dp
    /** 멤버 이니셜 칩 */
    val Chip = 22.dp
    /** 칩끼리 겹치는 폭 */
    val ChipOverlap = 2.dp
    /** 몸통에 새긴 줄무늬 한 줄의 두께 */
    val Stripe = 5.dp
}

/** 베벨 두께. 위·왼쪽보다 아래·오른쪽을 한 겹 두껍게 해야 두께가 느껴집니다. */
private val BevelLight = 2.dp
private val BevelDark = 3.dp

/**
 * **볼록한 플라스틱** — 몸통·하우징처럼 튀어나온 것.
 *
 * `box-shadow: inset` 이 없어서 색을 세 겹으로 깔아 만듭니다. 바깥이 밝은 색,
 * 그 안이 어두운 색, 맨 안이 본체 — 여백을 한쪽씩만 줘서 밝은 테는 위·왼쪽에,
 * 어두운 테는 아래·오른쪽에 남습니다.
 *
 * **베벨은 기기와 콘텐츠의 경계에만 줍니다.** 콘텐츠 안(달력 칸, 목록 줄)에는
 * 주지 않습니다 — 칸마다 두르면 자글자글해져서 정작 사진이 안 보입니다.
 */
fun Modifier.raisedPlastic(
    shape: Shape,
    face: Color = PlasticColors.Body,
): Modifier = this
    .clip(shape)
    .background(PlasticColors.BodyHi)
    .padding(start = BevelLight, top = BevelLight)
    .clip(shape)
    .background(PlasticColors.BodyLo)
    .padding(end = BevelLight, bottom = BevelDark)
    .clip(shape)
    .background(face)

/**
 * **움푹 팬 자리** — 끼워 넣은 화면, 파 놓은 홈.
 *
 * 볼록한 것과 **빛 방향이 반대**입니다. 위·왼쪽이 어둡고 아래·오른쪽이 밝습니다.
 */
fun Modifier.sunken(
    shape: Shape,
    face: Color = PlasticColors.Plate,
    rim: Dp = 2.dp,
): Modifier = this
    .clip(shape)
    .background(PlasticColors.PlateLo)
    .padding(start = rim, top = rim)
    .clip(shape)
    .background(PlasticColors.BodyHi)
    .padding(end = 1.dp, bottom = 1.dp)
    .clip(shape)
    .background(face)
