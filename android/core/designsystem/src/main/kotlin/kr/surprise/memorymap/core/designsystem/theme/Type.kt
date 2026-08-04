package kr.surprise.memorymap.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.surprise.memorymap.core.designsystem.R

/**
 * **Pretendard 한 벌.** 굵기와 자간만으로 위계를 만듭니다.
 * 서체를 섞지 않는 것이 이 앱 디자인의 첫 번째 규칙입니다 (`docs/app/design.html`).
 *
 * 폰트 파일은 한글·라틴만 남기고 잘라 넣었습니다 (네 굵기 합쳐 약 5MB).
 * 공간 이름은 사용자가 직접 쓰므로 한글 전체(11,172자)를 덜어내면 안 됩니다.
 */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

/**
 * 여섯 단만 씁니다 — **한 화면에 세 단 이상 섞지 않습니다** (`docs/app/design.html`).
 *
 * 굵기가 800(ExtraBold)인 자리가 많은 것이 이 서체 체계의 특징입니다. 잉크 선과
 * 흰 면만으로 만든 화면이라, 글자가 굵어야 위계가 섭니다.
 *
 * 디자인 문서는 라틴·숫자에 Archivo 를 섞지만, **우리는 Pretendard 한 벌로 갑니다**
 * (2026-08-04 결정). 숫자와 영문의 인상이 시안과 조금 다른 대신, 서체 파일이
 * 한 벌로 끝나고 한글·라틴의 굵기가 한 줄 안에서 어긋나지 않습니다.
 */
object MemoryType {
    /** 25 / 800 — 화면 제목 ("짜국") */
    val Display = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.Bold,
        fontSize = 25.sp, lineHeight = 30.sp, letterSpacing = (-0.02).em,
    )
    /** 17 / 800 — 상단바 */
    val Title = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.Bold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.02).em,
    )
    /** 15 / 800 — 버튼 · 본문 강조 */
    val Headline = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.01).em,
    )
    /** 13.5 / 700 — 보조 버튼 · 필드값 */
    val Body = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp, lineHeight = 19.sp, letterSpacing = (-0.01).em,
    )
    /** 12.5 / 400 — 설명 */
    val Label = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp, lineHeight = 20.sp,
    )
    /** 11 / 700 — 딱지 · 캡션 */
    val Micro = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.SemiBold,
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
