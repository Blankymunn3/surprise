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
 * 큰 글자일수록 자간을 좁힙니다. 한글은 자간을 그대로 두면 크게 키웠을 때 헐거워 보입니다.
 */
object MemoryType {
    val Display = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 38.4.sp, letterSpacing = (-0.03).em,
    )
    val Title = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.02).em,
    )
    val Headline = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.01).em,
    )
    val Body = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 23.sp,
    )
    val Label = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 19.sp,
    )
    val Micro = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.04.em,
    )
    /** 세그먼트·버튼 라벨 */
    val Button = TextStyle(
        fontFamily = Pretendard, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.01).em,
    )
}

internal val MemoryTypography = Typography(
    displayLarge = MemoryType.Display,
    titleLarge = MemoryType.Title,
    titleMedium = MemoryType.Headline,
    bodyLarge = MemoryType.Body,
    labelLarge = MemoryType.Label,
    labelSmall = MemoryType.Micro,
)
