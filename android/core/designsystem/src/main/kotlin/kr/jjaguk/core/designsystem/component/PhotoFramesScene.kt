package kr.jjaguk.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kr.jjaguk.core.designsystem.theme.PlasticColors

/** 원본 그림의 좌표계. iOS 와 **같은 수를 씁니다** — 다르면 두 앱의 그림이 달라집니다. */
private const val W = 150f
private const val H = 110f

/** 가로세로 비. 홀로 놓을 때 부르는 쪽에서 `aspectRatio(FRAMES_RATIO)` 를 겁니다. */
const val FRAMES_RATIO = W / H

/**
 * 빈 목록에 놓는 그림 — **겹쳐 놓은 사진틀 셋**입니다.
 *
 * 앞의 한 장만 흰 면이고 뒤의 둘은 비었거나 회색인 이유: 아직 사진이 없다는 것을
 * 말이 아니라 그림으로 먼저 알리려는 것입니다. 레드 사각 하나가 구성을 잡아 줍니다.
 *
 * 지도·달력 화면에는 지도 자체가 그림 역할을 하므로 이 그림을 쓰지 않습니다.
 *
 * **패미컴 스타일에서는 검정 판 위에 놓입니다.** 그래서 색을 뒤집습니다 — 선이 잉크면
 * 검정 위의 검정이라 아예 안 보이고, 면이 흰색이면 판 위에서 혼자 번쩍입니다.
 * 모양(좌표·굵기)은 그대로라 두 스타일이 같은 그림입니다.
 */
@Composable
fun PhotoFramesScene(modifier: Modifier = Modifier) {
    val stroke = PlasticColors.OnPlateDim
    val back = PlasticColors.Plate
    val front = PlasticColors.PlateHi
    val mark = PlasticColors.Red

    Canvas(modifier) {
        val k = size.width / W
        fun x(v: Float) = v * k
        fun y(v: Float) = v * k

        val line = Stroke(width = 2f * k)

        fun frame(left: Float, top: Float, fill: androidx.compose.ui.graphics.Color?) {
            val at = Offset(x(left), y(top))
            val wh = Size(x(76f), y(58f))
            if (fill != null) drawRect(color = fill, topLeft = at, size = wh)
            drawRect(color = stroke, topLeft = at, size = wh, style = line)
        }

        // 뒤에서 앞으로. 앞의 것이 뒤의 것을 가려야 겹쳐 보입니다.
        frame(6f, 18f, null)
        frame(26f, 34f, back)
        frame(46f, 50f, front)

        drawRect(
            color = mark,
            topLeft = Offset(x(98f), y(10f)),
            size = Size(x(22f), y(22f)),
        )

        // 앞장 안의 산등성이
        drawPath(
            path = Path().apply {
                moveTo(x(60f), y(79f))
                lineTo(x(74f), y(64f))
                lineTo(x(86f), y(74f))
                lineTo(x(98f), y(60f))
            },
            color = stroke,
            style = line,
        )
    }
}
