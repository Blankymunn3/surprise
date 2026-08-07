package kr.surprise.memorymap.feature.upload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap

/**
 * 사진 올리기. 두 탭에서 같은 시트를 엽니다.
 *
 * 사진마다 어디·언제를 보고 고치는 일이라 **거의 다 펴진 시트**로 뜹니다.
 * 높이는 부르는 쪽이 정합니다 — 시트 안에서는 스스로 화면을 다 쓸 수 없습니다.
 */
@Composable
fun UploadSheet(
    state: UploadState,
    onIntent: (UploadIntent) -> Unit,
    onPickPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 몸통 위에 화면을 끼우고 조작은 화면 밖으로 냅니다.
    Box(modifier.fillMaxWidth()) { PlasticUploadBody(state, onIntent, onPickPhotos) }
}

@Composable
private fun Divider(inset: Boolean = true) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (inset) Gap.xl else 0.dp)
            .height(MemoryStroke.Divider)
            .background(MemoryColors.Line2)
    )
}
