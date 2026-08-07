package kr.surprise.memorymap.core.designsystem.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * **누르면 내려앉습니다.**
 *
 * 플라스틱 버튼에는 물결(ripple)이 없습니다 — 실제 버튼은 빛이 번지는 것이 아니라
 * 그냥 **내려갑니다.** 그래서 머티리얼 기본 표시를 끄고 세로로 조금 옮깁니다.
 *
 * 색을 어둡게 하지 않는 이유: 빨간 A 버튼은 이미 진한 빨강이고 고무는 검정이라,
 * 더 어둡게 해도 거의 티가 안 납니다. **움직임이 훨씬 잘 읽힙니다.**
 */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // 뗄 때가 누를 때보다 조금 느립니다. 손가락이 떨어진 뒤 버튼이 올라오는 것이
    // 눈에 보여야 "눌렀다" 가 완결됩니다.
    val drop by animateDpAsState(
        targetValue = if (pressed && enabled) PressDrop else 0.dp,
        animationSpec = tween(if (pressed) 40 else 90),
        label = "press",
    )

    // ⚠️ **`offset` 을 맨 바깥에 둡니다.** 부르는 쪽은 보통 이렇게 씁니다:
    //
    //     Modifier.size(...).clip(...).background(...).pressable(...)
    //
    // 여기서 `this.offset(...)` 으로 뒤에 붙이면 offset 이 체인 **안쪽**이 되어
    // 배경보다 나중에 적용됩니다 — 버튼 면은 가만히 있고 **글자만 내려갑니다.**
    // `Modifier.offset(...).then(this)` 로 앞에 세워야 크기·모서리·배경까지
    // 통째로 내려갑니다. iOS 는 ButtonStyle 이 label 전체를 옮기므로 원래 이렇습니다.
    //
    // `clickable` 은 그대로 맨 뒤입니다. 누르는 자리는 크기가 정해진 뒤라야 합니다.
    return Modifier
        .offset(y = drop)
        .then(this)
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * 눌렸을 때 내려가는 깊이.
 *
 * 2dp 면 베벨(위 2 · 아래 3)만큼이라, 버튼이 제 그림자 속으로 들어가는 것처럼 보입니다.
 * 더 깊게 하면 눌린 것이 아니라 화면이 흔들린 것으로 읽힙니다.
 */
private val PressDrop = 2.dp
