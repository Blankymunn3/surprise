package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType

/**
 * 사진 올리기 버튼. **원, 지름 56, 단색.**
 * 지도처럼 배경이 복잡한 화면에서는 원이 형태로 먼저 읽힙니다.
 * 그라디언트와 안쪽 광택은 쓰지 않습니다 (2013년 문법).
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
            .size(56.dp)
            .shadow(12.dp, MemoryShapes.Pill, spotColor = MemoryColors.Accent)
            .clip(MemoryShapes.Pill)
            .background(MemoryColors.Accent)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MemoryColors.OnAccent, modifier = Modifier.size(26.dp))
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
    Row(
        modifier = modifier
            .clip(MemoryShapes.Button)
            .background(if (enabled) MemoryColors.Accent else MemoryColors.Fill)
            .clickable(enabled = enabled, onClick = onClick)
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
    Box(
        modifier = modifier
            .clip(MemoryShapes.Button)
            .background(MemoryColors.Surface)
            .border(MemoryStroke.Border, MemoryColors.Line, MemoryShapes.Button)
            .clickable(onClick = onClick)
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
                .clickable(onClick = onClick)
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
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}
