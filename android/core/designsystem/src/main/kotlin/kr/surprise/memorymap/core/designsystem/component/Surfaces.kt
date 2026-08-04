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
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke

/**
 * 콘텐츠 **위에 떠 있는** 조작 층 — 지도 위 검색칸·버튼 같은 것.
 *
 * **유리가 아닙니다.** 예전에는 반투명 흰색으로 뒤가 비치게 했는데,
 * 새 디자인은 유리를 아예 쓰지 않습니다 — 꽉 찬 흰 면과 1px 잉크 선으로만
 * 떠 있음을 나타냅니다. 사진 위에 반투명을 얹으면 사진 색이 그대로 올라와
 * 글자가 읽히는 정도가 사진마다 달라지기 때문입니다.
 *
 * 이름에 Glass 를 다시 붙이지 마세요. 반투명을 되살리는 첫걸음이 됩니다.
 */
@Composable
fun FloatingSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MemoryShapes.Square,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = shape, ambientColor = MemoryColors.Ink, spotColor = MemoryColors.Ink)
            .clip(shape)
            .background(MemoryColors.Surface)
            .border(MemoryStroke.Border, MemoryColors.Line, shape),
        content = content,
    )
}

/** 콘텐츠 층. 떠 있지 않아 그림자도 테두리도 없습니다. */
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
