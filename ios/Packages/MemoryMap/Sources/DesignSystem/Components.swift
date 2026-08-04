import CoreModel
import Foundation
import SwiftUI

/**
 콘텐츠 **위에 떠 있는** 조작 층 — 지도 위 검색칸·버튼 같은 것.

 **유리가 아닙니다.** 예전에는 반투명 흰색으로 뒤가 비치게 했는데, 새 디자인은
 유리를 아예 쓰지 않습니다 — 꽉 찬 흰 면과 1px 잉크 선으로만 떠 있음을 나타냅니다.
 사진 위에 반투명을 얹으면 사진 색이 그대로 올라와, 글자가 읽히는 정도가
 사진마다 달라지기 때문입니다.

 이름에 Glass 를 다시 붙이지 마세요. 반투명을 되살리는 첫걸음이 됩니다.
 */
public struct FloatingBackground: ViewModifier {
    public func body(content: Content) -> some View {
        content
            .background(MemoryColor.surface)
            .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
            .shadow(color: MemoryColor.ink.opacity(0.16), radius: 6, y: 2)
    }
}

public extension View {
    func floatingSurface() -> some View { modifier(FloatingBackground()) }
}

/// 탭 높이. 시안이 정한 값입니다.
private let tabHeight: CGFloat = 40

/**
 지도 | 달력 탭. **가로를 꽉 채운 네모 두 칸**입니다.

 알약이 아닌 이유: 이 탭은 지도 위에 떠 있지 않고 지도 **위쪽에 자리를 차지하고**
 있습니다. 떠 있지 않으니 알약으로 만들어 배경과 떼어 놓을 까닭이 없고,
 가로를 꽉 채우면 두 칸이 정확히 반씩이라 어느 쪽이 켜졌는지 한눈에 보입니다.

 고른 칸은 **잉크로 꽉 채웁니다** — 선만으로는 두 칸 중 어느 쪽인지 헷갈립니다.
 */
public struct Segmented: View {
    let options: [String]
    @Binding var selection: Int

    public init(options: [String], selection: Binding<Int>) {
        self.options = options
        self._selection = selection
    }

    public var body: some View {
        HStack(spacing: 0) {
            ForEach(options.indices, id: \.self) { index in
                let on = index == selection

                // 칸 사이 선은 **한 줄만** 긋습니다. 칸마다 테두리를 두르면 가운데가
                // 두 겹이 되어 그 선만 굵어 보입니다.
                if index > 0 {
                    MemoryColor.line.frame(width: MemoryStroke.border)
                }

                Text(options[index])
                    .memoryBody()
                    .foregroundStyle(on ? MemoryColor.paper : MemoryColor.ink)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(on ? MemoryColor.ink : MemoryColor.surface)
                    .contentShape(Rectangle())
                    .onTapGesture { selection = index }
                    .accessibilityAddTraits(on ? [.isSelected, .isButton] : .isButton)
            }
        }
        .frame(height: tabHeight)
        .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
    }
}

/**
 사진 올리기 버튼. **네모, 54, 단색 레드.**

 동그라미가 아닌 이유: 이 디자인에는 둥근 것이 하나도 없습니다. 지도 위에서
 형태로 먼저 읽히게 하는 일은 모서리가 아니라 **레드 한 색**이 맡습니다 —
 화면에서 유일하게 꽉 찬 레드라 다른 것과 헷갈릴 수가 없습니다.
 */
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
                .font(.system(size: 21, weight: .semibold))
                .foregroundStyle(MemoryColor.onAccent)
                .frame(width: 54, height: 54)
                .background(MemoryColor.accent)
                .shadow(color: MemoryColor.ink.opacity(0.28), radius: 8, y: 4)
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

/// 숫자 위에 까는 그늘의 높이. 숫자 한 줄만 덮으면 됩니다.
private let dayShadeHeight: CGFloat = 22

public struct DayCell: View {
    let day: Int
    let photoURL: String?
    let isToday: Bool
    let isSunday: Bool
    let isSelected: Bool

    public init(
        day: Int, photoURL: String?, isToday: Bool, isSunday: Bool, isSelected: Bool = false
    ) {
        self.day = day
        self.photoURL = photoURL
        self.isToday = isToday
        self.isSunday = isSunday
        self.isSelected = isSelected
    }

    /**
     숫자는 **늘 왼쪽 위**, 배경은 **사진이 있을 때만**. 안드로이드 `DayCell` 과 같습니다.
     빈 날까지 회색을 깔면 달력이 격자무늬가 되어 정작 사진이 묻힙니다.

     테두리는 두 가지입니다 — **고른 날은 잉크, 오늘은 레드.** 둘 다면 고른 쪽이
     이깁니다. 지금 무엇을 보고 있는지가, 오늘이 언제인지보다 급합니다.
     */
    public var body: some View {
        ZStack(alignment: .topLeading) {
            Color.clear

            if let photoURL, let url = URL(string: photoURL) {
                RemotePhoto(url: url) { MemoryColor.fill }
                // 밝은 사진 위에서도 흰 숫자가 읽히도록 **위쪽 한 줄만** 어둡게.
                // 칸 전체를 덮으면 사진이 어두워지고, 사진을 보려고 만든 칸이 아니게 됩니다.
                .overlay(alignment: .top) {
                    LinearGradient(
                        colors: [MemoryColor.ink.opacity(0.45), .clear],
                        startPoint: .top, endPoint: .bottom
                    )
                    .frame(height: dayShadeHeight)
                }
                .clipped()
            }

            Text("\(day)")
                .memoryMicro()
                .foregroundStyle(textColor)
                .padding(.leading, 5)
                .padding(.top, 3)
        }
        .aspectRatio(1, contentMode: .fit)
        .overlay {
            if let edge = edgeColor {
                Rectangle().strokeBorder(edge, lineWidth: 2)
            }
        }
    }

    private var edgeColor: Color? {
        if isSelected { return MemoryColor.ink }
        if isToday { return MemoryColor.accent }
        return nil
    }

    private var textColor: Color {
        if photoURL != nil { return .white }
        // 일요일만 딥레드입니다. 레드는 주 동작에 쓰는 색이라, 눌러야 할 것이
        // 아닌 자리에는 한 단계 어두운 쪽을 씁니다.
        return isSunday ? MemoryColor.accentDeep : MemoryColor.ink
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
