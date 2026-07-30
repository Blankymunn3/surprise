import CoreModel
import DesignSystem
import SwiftUI

private let weekdays = ["일", "월", "화", "수", "목", "금", "토"]

/// 달력 탭. 지도 탭과 **같은 밝은 바탕**입니다 — 탭 하나 옮겼다고 앱이 뒤집히면 안 됩니다.
public struct CalendarView: View {
    @State private var store: CalendarStore

    public init(store: CalendarStore) {
        self._store = State(initialValue: store)
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header
                if !store.state.collapsed {
                    weekdayRow
                    grid
                }
                collapseBar
                ForEach(store.state.visibleDays) { group in
                    daySection(group)
                }
            }
            .padding(.bottom, 110)
        }
        .background(MemoryColor.paper)
        .task { await store.refresh() }
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline, spacing: MemorySpace.s) {
            // "3월" 크게, "2026" 작게 — 대부분 올해를 보고 연도는 확인용입니다
            Text("\(store.state.month)월").memoryDisplay()
            Text(String(store.state.year)).memoryBody().foregroundStyle(MemoryColor.ink3)
            Spacer()
            Button { store.move(by: -1) } label: {
                Image(systemName: "chevron.left").foregroundStyle(MemoryColor.ink2)
            }
            .accessibilityLabel("이전 달")
            Button { store.move(by: 1) } label: {
                Image(systemName: "chevron.right").foregroundStyle(MemoryColor.ink2)
            }
            .accessibilityLabel("다음 달")
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 22)
        .padding(.top, MemorySpace.l)
    }

    private var weekdayRow: some View {
        HStack(spacing: MemorySpace.xs) {
            ForEach(weekdays.indices, id: \.self) { index in
                Text(weekdays[index])
                    .memoryMicro()
                    .foregroundStyle(index == 0 ? MemoryColor.accent : MemoryColor.ink3)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, MemorySpace.s)
    }

    private var grid: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: MemorySpace.xs), count: 7),
                  spacing: MemorySpace.xs) {
            ForEach(store.state.cells) { cell in
                if let date = cell.date {
                    DayCell(
                        day: date.day, photoURL: cell.coverURL,
                        isToday: cell.isToday, isSunday: cell.isSunday
                    )
                    .onTapGesture { store.select(date) }
                } else {
                    Color.clear.aspectRatio(1, contentMode: .fit)
                }
            }
        }
        .padding(.horizontal, 18)
    }

    private var collapseBar: some View {
        Button { store.toggleCollapse() } label: {
            HStack(spacing: 6) {
                Text(store.state.collapsed ? "달력 펼치기" : "달력 접기")
                    .memoryLabel().foregroundStyle(MemoryColor.ink2)
                Image(systemName: store.state.collapsed ? "chevron.down" : "chevron.up")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(MemoryColor.ink2)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
        }
        .buttonStyle(.plain)
        .overlay(alignment: .top) { Rectangle().fill(MemoryColor.line).frame(height: 1) }
        .padding(.horizontal, 18)
        .padding(.top, MemorySpace.l)
    }

    private func daySection(_ group: DayGroup) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(spacing: MemorySpace.s) {
                Text("\(group.date.month)월 \(group.date.day)일").memoryHeadline()
                Spacer()
                // 달력은 "언제" 를 보는 화면이지만 그 사진이 어디였는지가 늘 따라옵니다
                if let place = group.placeName {
                    Text(place)
                        .memoryMicro()
                        .foregroundStyle(MemoryColor.accent)
                        .padding(.horizontal, 11).padding(.vertical, 4)
                        .background(Capsule().fill(MemoryColor.accentTint))
                }
            }

            // 한 줄에 3장 — 4장을 넣으면 썸네일이 작아져 얼굴이 안 보입니다
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: MemorySpace.xs), count: 3),
                      spacing: MemorySpace.xs) {
                ForEach(group.photos) { photo in
                    PhotoThumb(url: photo.downloadURL, isCover: photo.id == group.coverId)
                        .onLongPressGesture {
                            Task { await store.setCover(photo.id, on: group.date) }
                        }
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, MemorySpace.s)
    }
}
