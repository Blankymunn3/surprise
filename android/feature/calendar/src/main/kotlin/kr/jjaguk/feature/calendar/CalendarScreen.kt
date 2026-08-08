package kr.jjaguk.feature.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource

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
