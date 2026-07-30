package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .clip(MemoryShapes.Button)
            .background(if (enabled) MemoryColors.Accent else MemoryColors.Fill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MemoryType.Button,
            color = if (enabled) MemoryColors.OnAccent else MemoryColors.Ink3,
        )
    }
}

@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MemoryShapes.Button)
            .background(MemoryColors.Fill)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MemoryType.Button, color = MemoryColors.Ink)
    }
}

/** 지도 위에 떠 있는 동그란 아이콘 버튼 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.size(38.dp)) {
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
