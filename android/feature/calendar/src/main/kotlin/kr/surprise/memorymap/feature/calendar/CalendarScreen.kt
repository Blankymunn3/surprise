package kr.surprise.memorymap.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap

/**
 * 요일 이름. 리소스 배열에서 읽습니다 — **순서가 뜻을 가지므로** 배열이어야 합니다
 * (요일 번호로 꺼내 씁니다). 일요일부터입니다.
 *
 * 패미컴 스타일 달력(`CalendarPlastic.kt`)도 같은 것을 쓰므로 `internal` 입니다.
 */
@Composable
internal fun weekdays(): List<String> = stringArrayResource(R.array.calendar_weekdays).toList()

/**
 * 달력 탭. 지도 탭과 **같은 밝은 바탕**입니다 —
 * 탭 하나 옮겼다고 앱이 뒤집히면 안 됩니다.
 */
@Composable
fun CalendarScreen(
    state: CalendarState,
    onIntent: (CalendarIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 격자는 몸통에 끼운 화면 안에 들어가고 조작은 화면 밖에 섭니다.
    Box(modifier.fillMaxSize()) { PlasticCalendarBody(state, onIntent) }
}

/** 구획선은 2px 입니다. 테두리(1px)보다 굵어야 '나누는 선' 으로 읽힙니다. */
@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Gap.xl)
            .height(MemoryStroke.Divider)
            .background(MemoryColors.Line2)
    )
}

/** ±100년. 넘기다 끝에 닿을 일은 없습니다. */
private const val PAGE_COUNT = 2401