import CoreModel
import Foundation

/// 달력 격자. **일요일 시작**입니다 (디자인의 요일 줄이 일·월·화… 순서).
/// 안드로이드 `CalendarMonth` 와 같은 규칙 — 두 앱이 다른 달력을 그리면 안 됩니다.
public enum CalendarMonth {

    /// 그달의 칸 목록. 앞뒤 빈 칸은 `nil` 이고, 항상 7의 배수 길이입니다.
    public static func grid(year: Int, month: Int) -> [CalendarDate?] {
        var cells: [CalendarDate?] = []
        cells.reserveCapacity(42)

        let leading = weekdayIndex(year: year, month: month, day: 1)
        cells.append(contentsOf: Array(repeating: nil, count: leading))

        for day in 1...daysIn(year: year, month: month) {
            cells.append(CalendarDate(year: year, month: month, day: day))
        }

        while cells.count % 7 != 0 { cells.append(nil) }
        return cells
    }

    public static func isSunday(_ date: CalendarDate) -> Bool {
        weekdayIndex(year: date.year, month: date.month, day: date.day) == 0
    }

    public static func daysIn(year: Int, month: Int) -> Int {
        switch month {
        case 1, 3, 5, 7, 8, 10, 12: return 31
        case 4, 6, 9, 11: return 30
        default: return isLeap(year) ? 29 : 28
        }
    }

    public static func isLeap(_ year: Int) -> Bool {
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    }

    /// 일요일 0 … 토요일 6. Zeller 공식 — `Calendar` 를 쓰면 기기 설정(주 시작 요일)에
    /// 따라 결과가 달라져서 직접 계산합니다.
    static func weekdayIndex(year: Int, month: Int, day: Int) -> Int {
        var y = year
        var m = month
        if m < 3 {
            m += 12
            y -= 1
        }
        let k = y % 100
        let j = y / 100
        let h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7
        // Zeller: 0=토요일. 일요일 0 으로 옮깁니다.
        return (h + 6) % 7
    }
}
