import Foundation
import SwiftUI

/// 색·글씨·모서리는 `docs/app/design.html` 이 원본입니다.
/// 안드로이드 `MemoryColors` 와 **같은 값**이어야 두 앱이 같아 보입니다.
public enum MemoryColor {
    public static let accent = Color(hex: 0xE11D5B)
    public static let accentTint = Color(hex: 0xFDEDF2)

    public static let ink = Color(hex: 0x1A1416)      // 검정이 아니라 따뜻한 먹색
    public static let ink2 = Color(hex: 0x6E6266)
    public static let ink3 = Color(hex: 0xA2969A)

    public static let paper = Color(hex: 0xFBF8F9)
    public static let surface = Color.white
    public static let fill = Color(hex: 0xF4EFF1)
    public static let line = Color(hex: 0xEDE5E8)
    public static let line2 = Color(hex: 0xE0D5DA)

    public static let mapSea = Color(hex: 0xDEEAEF)
    public static let mapLand = Color(hex: 0xEFEAE3)
}

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: 1
        )
    }
}

/// **Pretendard 한 벌.** 굵기와 자간만으로 위계를 만듭니다.
///
/// 굵기를 **PostScript 이름으로 직접** 고릅니다. `.custom("Pretendard").weight(.semibold)`
/// 로 하지 않는 이유: 이 폰트 파일들은 Medium·SemiBold 가 각자 다른 패밀리로 들어 있어
/// (`Pretendard Medium`, `Pretendard SemiBold`) `Pretendard` 패밀리 안에서는 찾지 못하고
/// 시스템이 굵기를 **흉내 내 그립니다**. 이름을 직접 대면 그 얼굴이 그대로 나옵니다.
///
/// 앱 번들에 폰트가 없으면 시스템 서체로 떨어집니다 — 등록은 앱 껍데기가 합니다
/// (`ios/App/Info.plist` 의 `UIAppFonts`).
public enum MemoryFont {
    static let family = "Pretendard"

    public static let display = font(32, .bold)
    public static let title = font(20, .bold)
    public static let headline = font(17, .semibold)
    public static let body = font(15, .regular)
    public static let label = font(13, .medium)
    public static let micro = font(11, .semibold)

    /// 안드로이드 `res/font` 에 있는 것과 **같은 네 벌**입니다.
    static func faceName(_ weight: Font.Weight) -> String {
        switch weight {
        case .bold, .heavy, .black: "\(family)-Bold"
        case .semibold: "\(family)-SemiBold"
        case .medium: "\(family)-Medium"
        default: "\(family)-Regular"
        }
    }

    static func font(_ size: CGFloat, _ weight: Font.Weight) -> Font {
        .custom(faceName(weight), size: size)
    }
}

/// 큰 글자일수록 자간을 좁힙니다 — 한글은 그대로 두면 헐거워 보입니다.
public extension View {
    func memoryDisplay() -> some View { font(MemoryFont.display).tracking(-0.96) }
    func memoryTitle() -> some View { font(MemoryFont.title).tracking(-0.4) }
    func memoryHeadline() -> some View { font(MemoryFont.headline).tracking(-0.17) }
    func memoryBody() -> some View { font(MemoryFont.body) }
    func memoryLabel() -> some View { font(MemoryFont.label) }
    func memoryMicro() -> some View { font(MemoryFont.micro).tracking(0.44) }
}

public enum MemoryRadius {
    public static let dayCell: CGFloat = 10
    public static let thumb: CGFloat = 12
    public static let button: CGFloat = 16
    public static let card: CGFloat = 20
    public static let sheet: CGFloat = 24
}

public enum MemorySpace {
    public static let xs: CGFloat = 4
    public static let s: CGFloat = 8
    public static let m: CGFloat = 12
    public static let l: CGFloat = 16
    public static let xl: CGFloat = 20
    public static let xxl: CGFloat = 24
    public static let xxxl: CGFloat = 32
}
