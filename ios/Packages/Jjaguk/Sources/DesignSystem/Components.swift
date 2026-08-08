import CoreModel
import Foundation
import SwiftUI

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
                    // 날짜 배지는 각인 — PS2P 8 (2026-08-09 검수 시안). 내용이
                    // "7.27" 꼴 숫자뿐이라 라틴 전용 서체로 충분합니다.
                    .font(MemoryFont.pressStart(8))
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
