import SwiftUI

/**
 **시험용 토큰 — 패미컴 컨트롤러 스타일.**

 지금 앱의 기준은 `MemoryColor` (웜 그레이 + 잉크 + 레드, 모서리 0) 입니다.
 이 파일은 그것과 **나란히** 두는 다른 한 벌이고, 짜국 목록 한 화면에서만 씁니다
 (`SpaceListView` 의 `plasticTrial`). 채택되지 않으면 이 파일과 그 화면만 지우면 됩니다.

 색은 NES 컨트롤러 실물에서 땄습니다 — 회색 플라스틱 몸통, 검정 페이스플레이트,
 빨간 A·B 버튼, 검은 십자키. 안드로이드 `PlasticColors` 와 **같은 값**입니다.
 */
public enum PlasticColor {
    /// 몸통 플라스틱. 화면 바탕입니다.
    public static let body = Color(hex: 0xDCD9D3)
    /// 위·왼쪽 베벨 — 빛 받는 쪽
    public static let bodyHi = Color(hex: 0xF0EEEA)
    /// 아래·오른쪽 베벨 — 그늘 지는 쪽
    public static let bodyLo = Color(hex: 0xBEBBB4)

    /// 검정 페이스플레이트. 사진이 놓이는 판입니다.
    public static let plate = Color(hex: 0x3B3B3B)
    public static let plateLo = Color(hex: 0x262626)

    /// 몸통에 새긴 회색 줄무늬
    public static let trim = Color(hex: 0x9C9C9C)
    public static let trimLo = Color(hex: 0x7E7E7E)

    /// A·B 버튼의 빨강. 주 동작에만 씁니다.
    public static let red = Color(hex: 0xD8342A)

    /// 고무 버튼
    public static let rubber = Color(hex: 0x3A3A3A)
    public static let onRubber = Color(hex: 0xC8C5C0)

    /// 검정 판 위의 글자
    public static let onPlate = Color(hex: 0xDCD9D3)
    public static let onPlateDim = Color(hex: 0x9E9B96)
    public static let onRed = Color(hex: 0xFFFFFF)
}

/// 이 스타일에서는 모서리가 **둥급니다** — 지금 기준(모서리 0)과 정반대입니다.
public enum PlasticRadius {
    public static let device: CGFloat = 10
    /// 버튼을 감싼 사각 하우징. 사진 액자도 이것입니다.
    public static let housing: CGFloat = 5
    /// 몸통에 끼운 화면
    public static let screen: CGFloat = 4
    public static let chip: CGFloat = 3
}

/**
 **이 스타일에만 있는 치수.**

 나머지 여백은 앱의 `MemorySpace` 단(4·8·12·16·20·24·32)을 그대로 씁니다 —
 화면 하나 때문에 여백 체계를 새로 만들지 않습니다. 여기 있는 것은 그 단으로는
 표현되지 않는, **형태가 뜻을 갖는** 값들뿐입니다.
 안드로이드 `PlasticSize` 와 같은 값입니다.
 */
public enum PlasticSize {
    /// 하우징이 사진을 감싸는 두께. 이게 곧 액자 테의 굵기입니다.
    public static let housingInset: CGFloat = 5
    /// 하우징이 버튼을 감싸는 두께
    public static let buttonInset: CGFloat = 7
    /// 고무 알약과 A 버튼의 높이. **둘이 같아야** 나란히 섰을 때 어긋나지 않습니다.
    public static let button: CGFloat = 46
    /// 카드 사진 높이
    public static let photo: CGFloat = 108
    /// 멤버 이니셜 칩
    public static let chip: CGFloat = 22
    /// 칩끼리 겹치는 폭
    public static let chipOverlap: CGFloat = 2
    /// 몸통에 새긴 줄무늬 한 줄의 두께
    public static let stripe: CGFloat = 5
}

/// 베벨 두께. 위·왼쪽보다 아래·오른쪽을 한 겹 두껍게 해야 두께가 느껴집니다.
private let bevelLight: CGFloat = 2
private let bevelDark: CGFloat = 3

public extension View {
    /**
     **볼록한 플라스틱** — 몸통·하우징처럼 튀어나온 것.

     색을 세 겹 깔아 만듭니다. 바깥이 밝은 색, 그 안이 어두운 색, 맨 안이 본체 —
     여백을 한쪽씩만 줘서 밝은 테는 위·왼쪽에, 어두운 테는 아래·오른쪽에 남습니다.

     **베벨은 기기와 콘텐츠의 경계에만 줍니다.** 콘텐츠 안(카드 이름줄, 달력 칸)에는
     주지 않습니다 — 줄마다 두르면 자글자글해져서 정작 사진이 안 보입니다.
     */
    func raisedPlastic(_ radius: CGFloat = PlasticRadius.housing) -> some View {
        self
            .background(PlasticColor.body)
            .padding(.trailing, bevelLight)
            .padding(.bottom, bevelDark)
            .background(PlasticColor.bodyLo)
            .padding(.leading, bevelLight)
            .padding(.top, bevelLight)
            .background(PlasticColor.bodyHi)
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
    }

    /**
     **움푹 팬 자리** — 끼워 넣은 화면, 파 놓은 홈.

     볼록한 것과 **빛 방향이 반대**입니다. 위·왼쪽이 어둡고 아래·오른쪽이 밝습니다.
     */
    func sunken(
        _ radius: CGFloat = PlasticRadius.screen,
        face: Color = PlasticColor.plate,
        rim: CGFloat = 2
    ) -> some View {
        self
            .background(face)
            .padding(.trailing, 1)
            .padding(.bottom, 1)
            .background(PlasticColor.bodyHi)
            .padding(.leading, rim)
            .padding(.top, rim)
            .background(PlasticColor.plateLo)
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
    }
}
