import Foundation
import SwiftUI

/// 색·글씨·모서리는 `untitled/project/짜국 디자인.dc.html` 이 원본입니다.
/// 안드로이드 `MemoryColors` 와 **같은 값**이어야 두 앱이 같아 보입니다.
///
/// **UI 는 이 여섯이 전부입니다** — 바탕 · 표면 · 잉크 · 레드 · 딥레드 · 회색 글.
/// 색은 사진이 냅니다. 사진과 경쟁하는 유채색 UI 를 두지 않습니다.
public enum MemoryColor {
    /// 주 동작 · 대표 · 오늘 · 에러. **이 넷 말고는 안 씁니다.**
    public static let accent = Color(hex: 0xEC3013)
    /// 작은 강조 글씨. 레드는 작게 쓰면 눈에 튀어서 한 단계 어둡게 갑니다.
    public static let accentDeep = Color(hex: 0xAE1800)

    public static let ink = Color(hex: 0x201E1D)      // 글 · 선 · 탭
    public static let ink2 = Color(hex: 0x7D7979)     // 메타
    public static let ink3 = Color(hex: 0x9B9797)     // '아직 없음' 처럼 더 흐린 것

    public static let paper = Color(hex: 0xF3F2F2)    // 화면 바탕 — 웜 그레이
    public static let surface = Color(hex: 0xFFFFFF)  // 떠 있는 면 — 흰 면
    public static let fill = Color(hex: 0xEAE9E9)     // 그룹 헤더 · 눌린 자리

    /// 테두리 1px 은 **잉크 그대로**입니다. 흐린 회색 선을 쓰지 않습니다.
    public static let line = Color(hex: 0x201E1D)
    /// 구획선 2px — 잉크 40%.
    public static let line2 = Color(hex: 0x201E1D).opacity(0.4)

    public static let onAccent = Color(hex: 0xFFFFFF)

    /// 시트 뒤를 어둡게. 잉크를 그대로 묽혀 씁니다.
    public static let scrim = Color(hex: 0x201E1D).opacity(0.32)

    /// 지도에서 아직 안 다녀온 지역. **사진이 있는 지역만 사진으로 칠해지고**
    /// 나머지는 이 회색입니다 — 지도 자체가 빈 화면의 그림 역할을 합니다.
    public static let mapLand = Color(hex: 0xEAE9E9)
    public static let mapSea = Color(hex: 0xF3F2F2)
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
/// 디자인 문서는 라틴·숫자에 Archivo 를 섞지만, 우리는 Pretendard 한 벌로 갑니다
/// (2026-08-04 결정). 서체 파일이 한 벌로 끝나고, 한글·라틴의 굵기가 한 줄 안에서
/// 어긋나지 않습니다.
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

    /// 여섯 단만 씁니다 — **한 화면에 세 단 이상 섞지 않습니다.**
    public static let display = font(25, .bold)     // 화면 제목
    public static let title = font(17, .bold)       // 상단바
    public static let headline = font(15, .bold)    // 버튼 · 본문 강조
    public static let body = font(13.5, .semibold)  // 보조 버튼 · 필드값
    public static let label = font(12.5, .regular)  // 설명
    public static let micro = font(11, .semibold)   // 딱지 · 캡션

    /// 안드로이드 `res/font` 에 있는 것과 **같은 네 벌**입니다.
    static func faceName(_ weight: Font.Weight) -> String {
        switch weight {
        case .bold, .heavy, .black: "\(family)-Bold"
        case .semibold: "\(family)-SemiBold"
        case .medium: "\(family)-Medium"
        default: "\(family)-Regular"
        }
    }

    /// 여섯 단 밖의 크기가 필요한 자리를 위해 열어 둡니다 — 초대 코드처럼
    /// **UI 글이 아니라 화면의 주인공**인 글자에만 씁니다. 보통 글에는 쓰지 마세요.
    public static func font(_ size: CGFloat, _ weight: Font.Weight) -> Font {
        .custom(faceName(weight), size: size)
    }
}

/// 큰 글자일수록 자간을 좁힙니다 — 한글은 그대로 두면 헐거워 보입니다.
/// 값은 안드로이드의 `em` 자간을 각 크기에 곱한 것입니다 (Display·Title −0.02em, 나머지 −0.01em).
public extension View {
    func memoryDisplay() -> some View { font(MemoryFont.display).tracking(-0.5) }
    func memoryTitle() -> some View { font(MemoryFont.title).tracking(-0.34) }
    func memoryHeadline() -> some View { font(MemoryFont.headline).tracking(-0.15) }
    func memoryBody() -> some View { font(MemoryFont.body).tracking(-0.135) }
    func memoryLabel() -> some View { font(MemoryFont.label) }
    func memoryMicro() -> some View { font(MemoryFont.micro) }
}

/// **모서리는 0 입니다.** 카드·버튼·칩·시트 전부 직각입니다.
///
/// 이름을 남겨 둔 이유는 자리마다 뜻이 다르기 때문입니다 — 나중에 한 자리만
/// 둥글게 하고 싶어지면 여기서 그 자리만 바꾸면 됩니다. 지금은 전부 같은 값입니다.
public enum MemoryRadius {
    public static let square: CGFloat = 0

    public static let dayCell = square
    public static let thumb = square
    public static let button = square
    public static let card = square
    public static let sheet = square
    /// 멤버 이니셜 칩도 네모입니다. 동그라미를 쓰지 않습니다.
    public static let pill = square
}

/// 테두리 1px 잉크 · 구획선 2px. 두께도 디자인이 정한 값입니다.
public enum MemoryStroke {
    public static let border: CGFloat = 1
    public static let divider: CGFloat = 2
}

/// 간격은 4·8·12·16·20 의 배수만 씁니다.
public enum MemorySpace {
    public static let xs: CGFloat = 4
    public static let s: CGFloat = 8
    public static let m: CGFloat = 12
    public static let l: CGFloat = 16
    public static let xl: CGFloat = 20
    public static let xxl: CGFloat = 24
    public static let xxxl: CGFloat = 32
}
