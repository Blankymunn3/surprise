import CoreModel
import Foundation
import Testing
@testable import Domain

@Suite("달력 격자")
struct CalendarMonthTests {

    @Test("1일이 일요일이면 앞 빈 칸이 없다")
    func noLeadingWhenSunday() {
        // 2026년 3월 1일은 일요일입니다 (디자인 시안이 이 달을 씁니다)
        let grid = CalendarMonth.grid(year: 2026, month: 3)
        #expect(grid.first == CalendarDate(year: 2026, month: 3, day: 1))
    }

    @Test("1일이 목요일이면 앞 빈 칸이 넷이다")
    func leadingFour() {
        let grid = CalendarMonth.grid(year: 2026, month: 1)
        #expect(grid[0] == nil && grid[1] == nil && grid[2] == nil && grid[3] == nil)
        #expect(grid[4] == CalendarDate(year: 2026, month: 1, day: 1))
    }

    @Test("칸 수는 항상 7의 배수다")
    func multipleOfSeven() {
        for month in 1...12 {
            #expect(CalendarMonth.grid(year: 2026, month: month).count % 7 == 0)
        }
    }

    @Test("그달의 모든 날이 빠짐없이 한 번씩 들어간다")
    func allDaysOnce() {
        let days = CalendarMonth.grid(year: 2026, month: 3).compactMap { $0 }
        #expect(days.count == 31)
        #expect(Set(days).count == 31)
    }

    @Test("윤년 2월은 29일까지 나온다")
    func leapFebruary() {
        #expect(CalendarMonth.daysIn(year: 2028, month: 2) == 29)
        #expect(CalendarMonth.daysIn(year: 2026, month: 2) == 28)
        #expect(CalendarMonth.daysIn(year: 2100, month: 2) == 28)
        #expect(CalendarMonth.daysIn(year: 2000, month: 2) == 29)
    }

    @Test("안드로이드와 같은 요일을 낸다")
    func sameWeekdayAsAndroid() {
        // 안드로이드는 java.time 의 DayOfWeek 를 씁니다. 몇 개를 손으로 맞춰 둡니다.
        #expect(CalendarMonth.isSunday(CalendarDate(year: 2026, month: 3, day: 1)))
        #expect(CalendarMonth.isSunday(CalendarDate(year: 2026, month: 3, day: 8)))
        #expect(!CalendarMonth.isSunday(CalendarDate(year: 2026, month: 3, day: 2)))
        #expect(CalendarMonth.weekdayIndex(year: 2026, month: 3, day: 25) == 3)  // 수요일
        #expect(CalendarMonth.weekdayIndex(year: 2026, month: 7, day: 30) == 4)  // 목요일
    }
}
