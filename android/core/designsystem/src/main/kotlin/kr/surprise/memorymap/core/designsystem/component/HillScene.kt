package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors

/**
 * 언덕과 나무, 그 사이로 난 길. `docs/app/design.html` 표지의 그림입니다.
 *
 * **선이 아니라 면으로** 그립니다 — 테두리도 그림자도 없습니다. 그림은 사진이 없는
 * 자리를 채우는 것이지 눈길을 끌려는 것이 아니라서요.
 *
 * 좌표는 0~1 로 적고 그릴 때 크기를 곱합니다. 어떤 크기로 놓아도 같은 그림이 됩니다.
 * iOS `HillScene` 과 **같은 좌표**를 씁니다 — 다르면 두 앱의 그림이 달라집니다.
 */
@Composable
fun HillScene(modifier: Modifier = Modifier, showPath: Boolean = true) {
    // **비율을 스스로 정하지 않습니다.** 여기서 크기를 정하면 부모보다 커져 화면이
    // 밀려납니다. 홀로 놓을 때는 부르는 쪽에서 `aspectRatio(SCENE_RATIO)` 를 겁니다.
    Canvas(modifier) {
        drawRect(MemoryColors.MossSoft)
        drawScene(showPath)
    }
}

/** 그림의 가로:세로. 두 앱이 같아야 같은 자리에서 잘립니다. */
const val SCENE_RATIO = 300f / 160f

private val FarHill = Color(0xFFC7DEC9)
private val NearHill = Color(0xFFA9CDAF)
private val Trail = Color(0xFFEFE3CB)
private val Bark = Color(0xFFB98C63)
private val Leaf1 = Color(0xFF8FBE94)
private val Leaf2 = Color(0xFF7FB489)
private val Leaf3 = Color(0xFF9CC79F)

/**
 * 배경을 뺀 그림만. 카드처럼 **이미 바탕이 있는 자리**에 겹쳐 그릴 때 씁니다.
 */
internal fun DrawScope.drawScene(showPath: Boolean) {
    val w = size.width
    val h = size.height
    fun x(v: Float) = v * w
    fun y(v: Float) = v * h

    // 먼 언덕
    drawPath(
        Path().apply {
            moveTo(0f, y(0.58f))
            cubicTo(x(0.16f), y(0.40f), x(0.34f), y(0.52f), x(0.52f), y(0.47f))
            cubicTo(x(0.72f), y(0.41f), x(0.86f), y(0.54f), w, y(0.48f))
            lineTo(w, h); lineTo(0f, h); close()
        },
        FarHill,
    )

    // 가까운 언덕
    drawPath(
        Path().apply {
            moveTo(0f, y(0.74f))
            cubicTo(x(0.20f), y(0.62f), x(0.38f), y(0.76f), x(0.58f), y(0.70f))
            cubicTo(x(0.78f), y(0.64f), x(0.90f), y(0.76f), w, y(0.72f))
            lineTo(w, h); lineTo(0f, h); close()
        },
        NearHill,
    )

    if (showPath) {
        // 길 — 점선을 겹쳐 밟고 간 자국처럼 보이게 합니다
        val trail = Path().apply {
            moveTo(x(0.10f), y(0.97f))
            cubicTo(x(0.26f), y(0.86f), x(0.30f), y(0.76f), x(0.42f), y(0.72f))
            cubicTo(x(0.56f), y(0.68f), x(0.66f), y(0.76f), x(0.80f), y(0.74f))
        }
        drawPath(trail, Trail, style = Stroke(width = h * 0.055f, cap = StrokeCap.Round))
        drawPath(
            trail,
            Color(0xFFDFCEAE),
            alpha = 0.7f,
            style = Stroke(
                width = h * 0.055f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(h * 0.012f, h * 0.09f)),
            ),
        )
    }

    tree(x(0.16f), y(0.56f), h * 0.20f)
    tree(x(0.62f), y(0.44f), h * 0.26f)
    tree(x(0.89f), y(0.62f), h * 0.15f)

    // 작은 것들 — 열매·꽃. 아주 좁게만 씁니다.
    drawCircle(MemoryColors.Honey, radius = h * 0.022f, center = Offset(x(0.33f), y(0.88f)))
    drawCircle(Color(0xFFF5D68F), radius = h * 0.015f, center = Offset(x(0.36f), y(0.92f)))
    drawCircle(MemoryColors.Accent, alpha = 0.8f, radius = h * 0.018f, center = Offset(x(0.70f), y(0.89f)))
}

/** 나무 하나 — 줄기 하나에 잎 덩어리 셋. */
private fun DrawScope.tree(cx: Float, cy: Float, r: Float) {
    drawRect(
        color = Bark,
        topLeft = Offset(cx - r * 0.12f, cy),
        size = Size(r * 0.24f, r * 1.2f),
    )
    drawCircle(Leaf1, radius = r, center = Offset(cx, cy - r * 0.30f))
    drawCircle(Leaf2, radius = r * 0.66f, center = Offset(cx - r * 0.68f, cy + r * 0.10f))
    drawCircle(Leaf3, radius = r * 0.58f, center = Offset(cx + r * 0.66f, cy + r * 0.14f))
}
