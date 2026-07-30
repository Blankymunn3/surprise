package kr.surprise.memorymap.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** 모서리는 여섯 값으로 고정합니다. 화면에서 새 값을 만들지 않습니다. */
object MemoryShapes {
    val DayCell = RoundedCornerShape(10.dp)
    val Thumb = RoundedCornerShape(12.dp)
    val Button = RoundedCornerShape(16.dp)
    val Card = RoundedCornerShape(20.dp)
    val Sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

/** 간격은 4의 배수만 씁니다. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}
