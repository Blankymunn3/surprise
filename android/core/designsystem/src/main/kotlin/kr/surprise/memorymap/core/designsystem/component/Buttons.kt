package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.PLASTIC_TRIAL
import kr.surprise.memorymap.core.designsystem.theme.PlasticColors
import kr.surprise.memorymap.core.designsystem.theme.PlasticShapes
import kr.surprise.memorymap.core.designsystem.theme.PlasticSize
import kr.surprise.memorymap.core.designsystem.theme.Pretendard
import kr.surprise.memorymap.core.designsystem.theme.pressable
import kr.surprise.memorymap.core.designsystem.theme.raisedPlastic

/**
 * 사진 올리기 버튼. **네모, 54, 단색 레드.**
 *
 * 동그라미가 아닌 이유: 이 디자인에는 둥근 것이 하나도 없습니다. 지도 위에서
 * 형태로 먼저 읽히게 하는 일은 모서리가 아니라 **레드 한 색**이 맡습니다 —
 * 화면에서 유일하게 꽉 찬 레드라 다른 것과 헷갈릴 수가 없습니다.
 */
@Composable
fun MemoryFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = MemoryIcons.Plus,
) {
    Box(
        modifier = modifier
            .size(54.dp)
            .shadow(8.dp, MemoryShapes.Square, spotColor = MemoryColors.Ink)
            .background(MemoryColors.Accent)
            .pressable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MemoryColors.OnAccent, modifier = Modifier.size(22.dp))
    }
}

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
    if (PLASTIC_TRIAL) {
        Box(modifier.raisedPlastic(PlasticShapes.Housing).padding(PlasticSize.ButtonInset)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(PlasticSize.Button)
                    .clip(PlasticShapes.Pill)
                    .background(if (enabled) PlasticColors.Red else PlasticColors.RedOff)
                    .pressable(enabled = enabled, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (enabled) PlasticColors.OnRed else PlasticColors.OnRedOff,
                )
            }
        }
        return
    }

    Row(
        modifier = modifier
            .clip(MemoryShapes.Button)
            .background(if (enabled) MemoryColors.Accent else MemoryColors.Fill)
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled) MemoryColors.OnAccent else MemoryColors.Ink3
        Text(text = text, style = MemoryType.Headline, color = fg, modifier = Modifier.weight(1f))
        Text(text = "→", style = MemoryType.Headline, color = fg)
    }
}

/** 보조 동작. 흰 면에 1px 잉크 선. 여기도 왼끝 맞춤입니다. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // 패미컴 스타일에서는 하우징에 앉힌 **검은 고무 알약**입니다.
    if (PLASTIC_TRIAL) {
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
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PlasticColors.OnRubber,
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .clip(MemoryShapes.Button)
            .background(MemoryColors.Surface)
            .border(MemoryStroke.Border, MemoryColors.Line, MemoryShapes.Button)
            .pressable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Text(text, style = MemoryType.Body, color = MemoryColors.Ink)
    }
}

/** 지도처럼 콘텐츠 위에 떠 있는 아이콘 버튼 */
@Composable
fun FloatingIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingSurface(modifier = modifier.size(38.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .pressable(onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MemoryColors.Ink, modifier = Modifier.size(20.dp))
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
