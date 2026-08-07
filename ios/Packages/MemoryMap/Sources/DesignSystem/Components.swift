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
/**
 지도 | 달력 탭. **가로를 꽉 채운 네모 두 칸**입니다.

 알약이 아닌 이유: 이 탭은 지도 위에 떠 있지 않고 지도 **위쪽에 자리를 차지하고**
 있습니다. 떠 있지 않으니 알약으로 만들어 배경과 떼어 놓을 까닭이 없고,
 가로를 꽉 채우면 두 칸이 정확히 반씩이라 어느 쪽이 켜졌는지 한눈에 보입니다.

 고른 칸은 **잉크로 꽉 채웁니다** — 선만으로는 두 칸 중 어느 쪽인지 헷갈립니다.
/**
 사진 올리기 버튼. **네모, 54, 단색 레드.**

 동그라미가 아닌 이유: 이 디자인에는 둥근 것이 하나도 없습니다. 지도 위에서
 형태로 먼저 읽히게 하는 일은 모서리가 아니라 **레드 한 색**이 맡습니다 —
 화면에서 유일하게 꽉 찬 레드라 다른 것과 헷갈릴 수가 없습니다.
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
        // 패미컴 스타일에서는 하우징에 앉힌 **빨간 A 버튼**입니다. 시트 넷(만들기·참여·
        // 초대 코드·올리기)이 모두 이 부품으로 만들어져 있어서, 여기 한 곳만 바꾸면
        // 넷이 같이 따라옵니다. 화살표는 뺍니다 — 알약 안에서는 글자만으로 충분하고,
        // 넣으면 A 버튼이 아니라 목록의 한 줄처럼 보입니다.
        Button(action: action) {
            Text(title)
                .font(MemoryFont.font(15, .bold))
                .foregroundStyle(enabled ? PlasticColor.onRed : PlasticColor.onButtonOff)
                .frame(maxWidth: .infinity)
                .frame(height: PlasticSize.button)
                .background(Capsule().fill(enabled ? PlasticColor.red : PlasticColor.buttonOff))
        }
        .buttonStyle(.plasticPress)
        .disabled(!enabled)
        .padding(PlasticSize.buttonInset)
        .raisedPlastic()
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
        // 패미컴 스타일에서는 하우징에 앉힌 **검은 고무 알약**입니다.
        Button(action: action) {
            Text(title)
                .font(MemoryFont.font(15, .bold))
                .foregroundStyle(PlasticColor.onRubber)
                .frame(maxWidth: .infinity)
                .frame(height: PlasticSize.button)
                .background(Capsule().fill(PlasticColor.rubber))
        }
        .buttonStyle(.plasticPress)
        .padding(PlasticSize.buttonInset)
        .raisedPlastic()
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
                    .accessibilityLabel(Text("component_cover_photo", bundle: .module))
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
