package kr.jjaguk.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.jjaguk.core.designsystem.R

/**
 * 갈무리11 — **앱 전체 서체**이자 각인 서체입니다 (갈무리 = OFL, 한글 11,172자 전부).
 *
 * 처음(2026-08-09 아침 시안)에는 '기계 각인 자리'에만 쓰고 본문은 Pretendard 를
 * 남겼는데, 같은 날 사용자가 **"혼용이 별로"** 라고 해 전면 전환했습니다.
 * 굵기는 둘뿐이라 Medium·SemiBold 자리도 Regular 파일로 갑니다 —
 * 시스템이 굵기를 흉내 내 픽셀을 뭉개는 것보다 낫습니다.
 */
val Galmuri11 = FontFamily(
    Font(R.font.galmuri11, FontWeight.Normal),
    Font(R.font.galmuri11, FontWeight.Medium),
    Font(R.font.galmuri11, FontWeight.SemiBold),
    Font(R.font.galmuri11_bold, FontWeight.Bold),
)

/**
 * 화면 전체가 쓰는 이름. 갈무리11 의 별칭입니다 — 서체를 다시 바꾸게 되면
 * 여기 한 줄만 고칩니다. Pretendard 파일(`pretendard_*.ttf`)은 돌아올 길로
 * 남겨 뒀습니다.
 */
val AppFont = Galmuri11

/** 아주 작은 각인(딱지 등)용. 9px 그리드라 9·18 크기로만. */
val Galmuri9 = FontFamily(Font(R.font.galmuri9))

/** 라틴·숫자 전용(Press Start 2P, OFL) — 한글이 오면 시스템 글꼴로 떨어지니
 *  숫자·영문 자리에만. 크기는 8의 배수. */
val PressStart = FontFamily(Font(R.font.press_start_2p))

/**
 * 여섯 단만 씁니다 — **한 화면에 세 단 이상 섞지 않습니다** (`docs/app/design.html`).
 *
 * 크기 단(25/17/15/13.5/12.5/11)은 Pretendard 시절 그대로입니다. 갈무리11 의
 * 배수(11·22)가 아니라 어중간한 크기에서는 픽셀이 살짝 무릅니다 — 전면 전환
 * 직후라 **위계를 흔들지 않는 쪽**을 골랐고, 실기기에서 거슬리는 단이 나오면
 * 그 단만 배수로 옮깁니다.
 */
object MemoryType {
    /** 25 / 800 — 화면 제목 ("짜국") */
    val Display = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Bold,
        fontSize = 25.sp, lineHeight = 30.sp, letterSpacing = (-0.02).em,
    )
    /** 17 / 800 — 상단바 */
    val Title = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Bold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.02).em,
    )
    /** 15 / 800 — 버튼 · 본문 강조 */
    val Headline = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.01).em,
    )
    /** 13.5 / 700 — 보조 버튼 · 필드값 */
    val Body = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp, lineHeight = 19.sp, letterSpacing = (-0.01).em,
    )
    /** 12.5 / 400 — 설명 */
    val Label = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp, lineHeight = 20.sp,
    )
    /** 11 / 700 — 딱지 · 캡션 */
    val Micro = TextStyle(
        fontFamily = AppFont, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 15.sp,
    )

    /** 버튼 라벨은 [Headline] 과 같은 단입니다. 따로 두지 않습니다. */
    val Button = Headline
}

internal val MemoryTypography = Typography(
    displayLarge = MemoryType.Display,
    titleLarge = MemoryType.Title,
    titleMedium = MemoryType.Headline,
    bodyLarge = MemoryType.Body,
    labelLarge = MemoryType.Label,
    labelSmall = MemoryType.Micro,
)
