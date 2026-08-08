package kr.jjaguk.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.jjaguk.core.designsystem.theme.MemoryColors
import kr.jjaguk.core.designsystem.theme.MemoryShapes
import kr.jjaguk.core.designsystem.theme.PlasticColors
import kr.jjaguk.core.designsystem.theme.PlasticShapes
import kr.jjaguk.core.designsystem.theme.PlasticSize
import kr.jjaguk.core.designsystem.theme.AppFont
import kr.jjaguk.core.designsystem.theme.pressable
import kr.jjaguk.core.designsystem.theme.raisedPlastic

/**
 * 주 동작. **글자는 왼끝에 맞추고 화살표가 오른끝에 섭니다** — 가운데 정렬이 아닙니다.
 *
 * 왼끝 맞춤인 이유: 이 버튼은 화면 가로를 꽉 채웁니다. 가운데에 두면 글자가
 * 어디서 시작하는지 매번 달라져서, 위에 쌓인 글줄들과 왼쪽 선이 어긋납니다.
 */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    // 패미컴 스타일에서는 하우징에 앉힌 **빨간 A 버튼**입니다. 화면 넷(만들기·참여·
    // 초대 코드·올리기)이 모두 이 부품으로 만들어져 있어서, 여기 한 곳만 바꾸면
    // 넷이 같이 따라옵니다. 화살표는 뺍니다 — 알약 안에서는 글자만으로 충분하고,
    // 넣으면 A 버튼이 아니라 목록의 한 줄처럼 보입니다.
    Box(modifier.raisedPlastic(PlasticShapes.Housing).padding(PlasticSize.ButtonInset)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(PlasticSize.Button)
                .clip(PlasticShapes.Pill)
                .background(if (enabled) PlasticColors.Red else PlasticColors.ButtonOff)
                .pressable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontFamily = AppFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (enabled) PlasticColors.OnRed else PlasticColors.OnButtonOff,
            )
        }
    }
}

/** 보조 동작. 흰 면에 1px 잉크 선. 여기도 왼끝 맞춤입니다. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // 패미컴 스타일에서는 하우징에 앉힌 **검은 고무 알약**입니다.
    Box(modifier.raisedPlastic(PlasticShapes.Housing).padding(PlasticSize.ButtonInset)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(PlasticSize.Button)
                .clip(PlasticShapes.Pill)
                .background(PlasticColors.Rubber)
                .pressable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontFamily = AppFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = PlasticColors.OnRubber,
            )
        }
    }
}

/** 종이 위에 놓이는 아이콘 버튼 (달력 탭처럼 떠 있지 않은 화면) */
@Composable
fun PlainIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MemoryColors.Ink,
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(MemoryShapes.Pill)
            .pressable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}
