package kr.surprise.memorymap.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 아이콘은 직접 그립니다. 디자인이 **선 두께 1.75** 로 정해져 있는데,
 * 기성 아이콘 세트는 2.0 이라 글씨 굵기와 어긋납니다.
 * 이모지를 쓰지 않는 이유도 같습니다 — 폰마다 모양이 달라 통제할 수 없습니다.
 *
 * `Icon(tint = ...)` 이 색을 입히므로 여기서는 검정으로 그려 둡니다.
 */
object MemoryIcons {

    private fun stroked(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()

    private fun filled(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black), pathBuilder = block)
        }.build()

    /** 원. 호 두 개로 그립니다. */
    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx + r, cy)
        arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
        arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
        close()
    }

    val Search = stroked("search") {
        circle(11f, 11f, 6.75f)
        moveTo(20f, 20f); lineTo(15.8f, 15.8f)
    }

    val Plus = stroked("plus") {
        moveTo(12f, 5.5f); lineTo(12f, 18.5f)
        moveTo(5.5f, 12f); lineTo(18.5f, 12f)
    }

    val Minus = stroked("minus") {
        moveTo(5.5f, 12f); lineTo(18.5f, 12f)
    }

    val MyLocation = stroked("my-location") {
        circle(12f, 12f, 6f)
        moveTo(12f, 2.6f); lineTo(12f, 5.8f)
        moveTo(12f, 18.2f); lineTo(12f, 21.4f)
        moveTo(2.6f, 12f); lineTo(5.8f, 12f)
        moveTo(18.2f, 12f); lineTo(21.4f, 12f)
        circle(12f, 12f, 1.9f)
    }

    val ChevronLeft = stroked("chevron-left") {
        moveTo(14f, 5.5f); lineTo(7.5f, 12f); lineTo(14f, 18.5f)
    }

    val ChevronRight = stroked("chevron-right") {
        moveTo(10f, 5.5f); lineTo(16.5f, 12f); lineTo(10f, 18.5f)
    }

    val ChevronDown = stroked("chevron-down") {
        moveTo(5.5f, 9.5f); lineTo(12f, 16f); lineTo(18.5f, 9.5f)
    }

    val ChevronUp = stroked("chevron-up") {
        moveTo(5.5f, 14.5f); lineTo(12f, 8f); lineTo(18.5f, 14.5f)
    }

    val Back = stroked("back") {
        moveTo(19.5f, 12f); lineTo(4.5f, 12f)
        moveTo(10.5f, 6f); lineTo(4.5f, 12f); lineTo(10.5f, 18f)
    }

    val Close = stroked("close") {
        moveTo(6.5f, 6.5f); lineTo(17.5f, 17.5f)
        moveTo(17.5f, 6.5f); lineTo(6.5f, 17.5f)
    }

    val Members = stroked("members") {
        circle(9.5f, 8.5f, 3.4f)
        moveTo(3.6f, 19.4f)
        curveTo(4.8f, 16f, 12.2f, 14.4f, 15.4f, 19.4f)
        moveTo(16.4f, 5.6f)
        curveTo(19.4f, 6.6f, 19.4f, 11.2f, 16.4f, 12.2f)
        moveTo(17.5f, 14.4f)
        curveTo(19.6f, 15f, 20.4f, 17f, 20.7f, 18.8f)
    }

    val More = filled("more") {
        circle(5.5f, 12f, 1.5f)
        circle(12f, 12f, 1.5f)
        circle(18.5f, 12f, 1.5f)
    }

    val Star = filled("star") {
        moveTo(12f, 3.6f)
        lineTo(14.5f, 8.9f); lineTo(20f, 9.7f); lineTo(16f, 13.7f)
        lineTo(16.95f, 19.4f); lineTo(12f, 16.7f); lineTo(7.05f, 19.4f)
        lineTo(8f, 13.7f); lineTo(4f, 9.7f); lineTo(9.5f, 8.9f)
        close()
    }

    val Sparkle = filled("sparkle") {
        moveTo(12f, 3f)
        lineTo(13.7f, 7.9f); lineTo(18.5f, 9.6f); lineTo(13.7f, 11.3f)
        lineTo(12f, 16.2f); lineTo(10.3f, 11.3f); lineTo(5.5f, 9.6f)
        lineTo(10.3f, 7.9f)
        close()
        moveTo(18.6f, 15f)
        lineTo(19.35f, 17.15f); lineTo(21.5f, 17.9f); lineTo(19.35f, 18.65f)
        lineTo(18.6f, 20.8f); lineTo(17.85f, 18.65f); lineTo(15.7f, 17.9f)
        lineTo(17.85f, 17.15f)
        close()
    }

    val Calendar = stroked("calendar") {
        moveTo(7f, 5.5f)
        lineTo(17f, 5.5f)
        arcToRelative(3.5f, 3.5f, 0f, false, true, 3.5f, 3.5f)
        lineTo(20.5f, 17f)
        arcToRelative(3.5f, 3.5f, 0f, false, true, -3.5f, 3.5f)
        lineTo(7f, 20.5f)
        arcToRelative(3.5f, 3.5f, 0f, false, true, -3.5f, -3.5f)
        lineTo(3.5f, 9f)
        arcToRelative(3.5f, 3.5f, 0f, false, true, 3.5f, -3.5f)
        close()
        moveTo(8f, 3f); lineTo(8f, 7.4f)
        moveTo(16f, 3f); lineTo(16f, 7.4f)
        moveTo(3.5f, 10.6f); lineTo(20.5f, 10.6f)
    }

    val Pin = stroked("pin") {
        moveTo(12f, 21f)
        curveTo(16.4f, 16.6f, 18.6f, 13.3f, 18.6f, 10.6f)
        curveTo(18.6f, 6.9f, 15.7f, 4f, 12f, 4f)
        curveTo(8.3f, 4f, 5.4f, 6.9f, 5.4f, 10.6f)
        curveTo(5.4f, 13.3f, 7.6f, 16.6f, 12f, 21f)
        close()
        circle(12f, 10.4f, 2.4f)
    }
}
