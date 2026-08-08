package kr.jjaguk.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * **모서리는 0 입니다.** 카드·버튼·칩·시트 전부 직각입니다 (`docs/app/design.html`).
 *
 * 이름을 남겨 둔 이유는 자리마다 뜻이 다르기 때문입니다 — 나중에 한 자리만
 * 둥글게 하고 싶어지면 여기서 그 자리만 바꾸면 됩니다. 지금은 전부 같은 값입니다.
 */
object MemoryShapes {
    val Square = RoundedCornerShape(0.dp)

    val DayCell = Square
    val Thumb = Square
    val Button = Square
    val Card = Square
    val Sheet = Square

    /** 멤버 이니셜 칩도 네모입니다. 동그라미를 쓰지 않습니다. */
    val Pill = Square
}

/** 간격은 4·8·12·16·20 의 배수만 씁니다. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

/** 테두리 1px 잉크 · 구획선 2px. 두께도 디자인이 정한 값입니다. */
object MemoryStroke {
    val Border = 1.dp
    val Divider = 2.dp
}
