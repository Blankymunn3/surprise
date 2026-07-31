import CoreModel
import Foundation
import SwiftUI

/// 콘텐츠 **위에 떠 있는** 조작 층. 유리는 여기에만 씁니다 —
/// iOS 26 Liquid Glass 의 규칙이 "콘텐츠 자체에는 쓰지 말 것" 입니다.
public struct GlassBackground: ViewModifier {
    public func body(content: Content) -> some View {
        content
            .background(.ultraThinMaterial, in: Capsule())
            .overlay(Capsule().strokeBorder(MemoryColor.ink.opacity(0.07), lineWidth: 0.5))
            .shadow(color: MemoryColor.ink.opacity(0.14), radius: 12, y: 8)
    }
}

public extension View {
    func glass() -> some View { modifier(GlassBackground()) }
}

/// 지도 | 달력 탭. 밑줄이 아니라 알약 세그먼트 — 지도 위에서 밑줄은 보이지 않습니다.
public struct Segmented: View {
    let options: [String]
    @Binding var selection: Int
    let floating: Bool

    public init(options: [String], selection: Binding<Int>, floating: Bool) {
        self.options = options
        self._selection = selection
        self.floating = floating
    }

    public var body: some View {
        HStack(spacing: 2) {
            ForEach(options.indices, id: \.self) { index in
                let on = index == selection
                Text(options[index])
                    .memoryLabel()
                    .fontWeight(.semibold)
                    .foregroundStyle(on ? MemoryColor.ink : MemoryColor.ink2)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 7)
                    .background {
                        if on {
                            Capsule().fill(MemoryColor.surface)
                                .shadow(color: MemoryColor.ink.opacity(0.14), radius: 2, y: 1)
                        }
                    }
                    .contentShape(Capsule())
                    .onTapGesture { selection = index }
                    .accessibilityAddTraits(on ? [.isSelected, .isButton] : .isButton)
            }
        }
        .padding(3)
        .background { if !floating { Capsule().fill(MemoryColor.fill) } }
        .modifier(GlassIfNeeded(floating: floating))
    }
}

private struct GlassIfNeeded: ViewModifier {
    let floating: Bool
    func body(content: Content) -> some View {
        if floating { content.glass() } else { content }
    }
}

/// 사진 올리기 버튼. **원, 지름 56, 단색.** 그라디언트도 광택도 쓰지 않습니다.
public struct MemoryFab: View {
    let action: () -> Void
    let label: String

    public init(label: String = "사진 올리기", action: @escaping () -> Void) {
        self.label = label
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Image(systemName: "plus")
                .font(.system(size: 24, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 56, height: 56)
                .background(Circle().fill(MemoryColor.accent))
                .shadow(color: MemoryColor.accent.opacity(0.55), radius: 12, y: 10)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

public struct PrimaryButton: View {
    let title: String
    let enabled: Bool
    let action: () -> Void

    public init(_ title: String, enabled: Bool = true, action: @escaping () -> Void) {
        self.title = title
        self.enabled = enabled
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Text(title)
                .memoryHeadline()
                .foregroundStyle(enabled ? Color.white : MemoryColor.ink3)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(
                    RoundedRectangle(cornerRadius: MemoryRadius.button, style: .continuous)
                        .fill(enabled ? MemoryColor.accent : MemoryColor.fill)
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

public struct DayCell: View {
    let day: Int
    let photoURL: String?
    let isToday: Bool
    let isSunday: Bool

    public init(day: Int, photoURL: String?, isToday: Bool, isSunday: Bool) {
        self.day = day
        self.photoURL = photoURL
        self.isToday = isToday
        self.isSunday = isSunday
    }

    /// 숫자는 **늘 왼쪽 위**, 배경은 **사진이 있을 때만**. 안드로이드 `DayCell` 과 같습니다.
    /// 빈 날까지 회색을 깔면 달력이 격자무늬가 되어 정작 사진이 묻힙니다.
    public var body: some View {
        ZStack(alignment: .topLeading) {
            Color.clear

            if let photoURL, let url = URL(string: photoURL) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    MemoryColor.fill
                }
                .clipShape(RoundedRectangle(cornerRadius: MemoryRadius.dayCell, style: .continuous))
                // 밝은 사진 위에서도 흰 숫자가 읽히도록 왼쪽 위만 어둡게
                .overlay(alignment: .topLeading) {
                    RadialGradient(
                        colors: [MemoryColor.ink.opacity(0.62), .clear],
                        center: .topLeading, startRadius: 0, endRadius: 46
                    )
                    .clipShape(RoundedRectangle(cornerRadius: MemoryRadius.dayCell, style: .continuous))
                }
            }

            Text("\(day)")
                .memoryLabel()
                .foregroundStyle(textColor)
                .padding(.leading, 7)
                .padding(.top, 5)
        }
        .aspectRatio(1, contentMode: .fit)
        .overlay {
            if isToday {
                RoundedRectangle(cornerRadius: MemoryRadius.dayCell, style: .continuous)
                    .strokeBorder(MemoryColor.accent, lineWidth: 2)
            }
        }
    }

    private var textColor: Color {
        if photoURL != nil { return .white }
        return isSunday ? MemoryColor.accent : MemoryColor.ink2
    }
}

/// 사진 썸네일. 대표사진은 로즈 2 테두리 + ★.
public struct PhotoThumb: View {
    let url: String?
    let isCover: Bool
    let dateLabel: String?

    public init(url: String?, isCover: Bool = false, dateLabel: String? = nil) {
        self.url = url
        self.isCover = isCover
        self.dateLabel = dateLabel
    }

    public var body: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: MemoryRadius.thumb, style: .continuous)
                .fill(MemoryColor.fill)

            if let url, let parsed = URL(string: url) {
                AsyncImage(url: parsed) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    MemoryColor.fill
                }
                .clipShape(RoundedRectangle(cornerRadius: MemoryRadius.thumb, style: .continuous))
            }

            if isCover {
                Image(systemName: "star.fill")
                    .font(.system(size: 10))
                    .foregroundStyle(.white)
                    .frame(width: 20, height: 20)
                    .background(Circle().fill(MemoryColor.accent))
                    .padding(6)
                    .accessibilityLabel("대표사진")
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .overlay(alignment: .bottomLeading) {
            if let dateLabel {
                Text(dateLabel)
                    .memoryMicro()
                    .foregroundStyle(.white)
                    .shadow(color: MemoryColor.ink.opacity(0.7), radius: 3)
                    .padding(.leading, 8)
                    .padding(.bottom, 6)
            }
        }
        .overlay {
            if isCover {
                RoundedRectangle(cornerRadius: MemoryRadius.thumb, style: .continuous)
                    .strokeBorder(MemoryColor.accent, lineWidth: 2)
            }
        }
    }
}
