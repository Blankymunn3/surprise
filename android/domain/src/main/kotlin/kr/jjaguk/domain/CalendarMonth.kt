package kr.jjaguk.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * 달력 격자. **일요일 시작**입니다 (디자인의 요일 줄이 일·월·화… 순서).
 *
 * 순수 함수라 테스트가 제일 쉽습니다. 달력이 하루라도 밀리면 사진이 엉뚱한 칸에
 * 붙기 때문에 여기부터 테스트합니다.
 */
object CalendarMonth {

    /**
     * 그달의 칸 목록. 앞뒤 빈 칸은 `null` 이고, 항상 7의 배수 길이입니다.
     * 3월 1일이 일요일이면 앞 빈 칸이 없습니다.
     */
    fun grid(month: YearMonth): List<LocalDate?> {
        val cells = ArrayList<LocalDate?>(42)

        // DayOfWeek 는 월=1 … 일=7. 일요일 시작으로 바꾸면 일=0 이 됩니다.
        val leading = month.atDay(1).dayOfWeek.value % 7
        repeat(leading) { cells.add(null) }

        for (day in 1..month.lengthOfMonth()) cells.add(month.atDay(day))

        while (cells.size % 7 != 0) cells.add(null)
        return cells
    }

    /** 일요일인가. 디자인에서 일요일 숫자만 로즈로 칠합니다. */
    fun isSunday(date: LocalDate): Boolean = date.dayOfWeek.value % 7 == 0
}
