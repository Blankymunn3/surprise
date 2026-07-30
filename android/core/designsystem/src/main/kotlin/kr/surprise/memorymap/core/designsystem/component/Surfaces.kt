package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes

/**
 * 콘텐츠 **위에 떠 있는** 조작 층. 유리는 여기에만 씁니다.
 *
 * iOS 26 Liquid Glass 의 규칙이 "유리는 떠 있는 층에만, 콘텐츠 자체에는 쓰지 말 것" 입니다.
 * 시트·카드에 이걸 쓰면 안 됩니다 — 뒤가 비쳐 사진이 지저분해 보입니다.
 *
 * 뒤 배경을 실제로 흐리는 것(backdrop blur)은 API 31 부터 가능합니다.
 * 지금은 반투명 흰색 + 실선 테두리로 같은 인상을 냅니다. 흐림은 나중에 얹습니다.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MemoryShapes.Pill,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 10.dp, shape = shape, ambientColor = MemoryColors.Ink, spotColor = MemoryColors.Ink)
            .clip(shape)
            .background(MemoryColors.Glass)
            .border(0.5.dp, MemoryColors.GlassBorder, shape),
        content = content,
    )
}

/** 콘텐츠 층. **불투명**합니다. */
@Composable
fun ContentSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MemoryShapes.Card,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MemoryColors.Surface),
        content = content,
    )
}
