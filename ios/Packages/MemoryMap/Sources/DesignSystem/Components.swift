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

/// 주 동작. **글자는 왼끝에 맞추고 화살표가 오른끝에 섭니다** — 가운데 정렬이 아닙니다.
///
/// 왼끝 맞춤인 이유: 이 버튼은 화면 가로를 꽉 채웁니다. 가운데에 두면 글자가
/// 어디서 시작하는지 매번 달라져서, 위에 쌓인 글줄들과 왼쪽 선이 어긋납니다.
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
            HStack(spacing: MemorySpace.s) {
                Text(title).memoryHeadline()
                Spacer(minLength: 0)
                Text("→").memoryHeadline()
            }
            .foregroundStyle(enabled ? MemoryColor.onAccent : MemoryColor.ink3)
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity)
            .background(enabled ? MemoryColor.accent : MemoryColor.fill)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

/// 보조 동작. 흰 면에 1px 잉크 선. 여기도 왼끝 맞춤입니다.
public struct SoftButton: View {
    let title: String
    let action: () -> Void

    public init(_ title: String, action: @escaping () -> Void) {
        self.title = title
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Text(title)
                .memoryBody()
                .foregroundStyle(MemoryColor.ink)
                .padding(.horizontal, 16)
                .padding(.vertical, 13)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(MemoryColor.surface)
                .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
        }
        .buttonStyle(.plain)
    }
}

/// 카드 위 멤버 칩은 셋까지, 넘으면 잉크 칸에 +N.
public let avatarsShown = 3

/// 이름 첫 글자 칩. **네모에 흰 면, 1px 잉크 선입니다.**
///
/// 사람마다 색을 주지 않는 이유: 카드 대부분이 사진이라 여기에 색을 더하면
/// 사진과 색이 부딪힙니다. 사람을 구분하는 건 색이 아니라 글자입니다.
/// 안드로이드 `MemberAvatars` 와 같은 규칙입니다.
public struct MemberAvatars: View {
    let initials: [String]
    let max: Int

    public init(initials: [String], max: Int = avatarsShown) {
        self.initials = initials
        self.max = max
    }

    public var body: some View {
        let shown = Array(initials.prefix(max))
        let rest = initials.count - shown.count

        HStack(spacing: -6) {
            ForEach(Array(shown.enumerated()), id: \.offset) { _, text in
                chip(text, filled: false)
            }
            if rest > 0 { chip("+\(rest)", filled: true) }
        }
    }

    private func chip(_ text: String, filled: Bool) -> some View {
        Text(text)
            .memoryMicro()
            .foregroundStyle(filled ? MemoryColor.onAccent : MemoryColor.ink)
            .frame(width: 24, height: 24)
            .background(filled ? MemoryColor.ink : MemoryColor.surface)
            .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
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
                RemotePhoto(url: url) { MemoryColor.fill }
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
                RemotePhoto(url: parsed) { MemoryColor.fill }
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
