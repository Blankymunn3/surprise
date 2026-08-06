import SwiftUI

/**
 **시험용 토큰 — 패미컴 컨트롤러 스타일.**

 지금 앱의 기준은 `MemoryColor` (웜 그레이 + 잉크 + 레드, 모서리 0) 입니다.
 이 파일은 그것과 **나란히** 두는 다른 한 벌입니다. 채택되지 않으면 이 파일과
 `*Plastic.swift` 들을 지우면 됩니다.

 색은 NES 컨트롤러 실물에서 땄습니다 — 회색 플라스틱 몸통, 검정 페이스플레이트,
 빨간 A·B 버튼, 검은 십자키. 안드로이드 `PlasticColors` 와 **같은 값**입니다.
 */
/**
 **시험 스위치 하나로 모든 화면을 켜고 끕니다.**

 처음엔 화면마다 `plasticTrial` 을 뒀는데, 화면이 늘자 켜 보려면 여러 파일을
 고쳐야 했습니다. 검수는 앱 전체를 한 벌로 보는 일이라 스위치도 하나여야 합니다.
 안드로이드 `PLASTIC_TRIAL` 과 늘 같은 값이어야 합니다.

 `false` 로 두면 앱은 기준 디자인(`MemoryColor`) 그대로 돌아갑니다.
 */
public let plasticTrial = true

public enum PlasticColor {
    /// 몸통 플라스틱. 화면 바탕입니다.
    public static let body = Color(hex: 0xDCD9D3)
    /// 위·왼쪽 베벨 — 빛 받는 쪽
    public static let bodyHi = Color(hex: 0xF0EEEA)
    /// 아래·오른쪽 베벨 — 그늘 지는 쪽
    public static let bodyLo = Color(hex: 0xBEBBB4)

    /// 검정 페이스플레이트. 사진이 놓이는 판입니다.
    public static let plate = Color(hex: 0x3B3B3B)
    public static let plateHi = Color(hex: 0x4A4A4A)
    public static let plateLo = Color(hex: 0x262626)

    /// 몸통에 새긴 회색 줄무늬
    public static let trim = Color(hex: 0x9C9C9C)
    public static let trimLo = Color(hex: 0x7E7E7E)

    /// A·B 버튼의 빨강. 주 동작에만 씁니다.
    public static let red = Color(hex: 0xD8342A)
    public static let redHi = Color(hex: 0xE85A4C)
    public static let redLo = Color(hex: 0x9E1F17)

    /// 십자키의 검정
    public static let ink = Color(hex: 0x1B1B1B)

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
    /// 십자키의 팔·달 넘김 버튼처럼 살짝만 둥근 것
    public static let knob: CGFloat = 4
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

    /// 십자키 한 변. 팔 하나는 이것의 1/3 이므로 **3의 배수**여야 팔이 딱 나뉩니다.
    public static let cross: CGFloat = 96
    /// 십자키 가운데의 작은 점
    public static let dotCore: CGFloat = 8

    /// 지역 시트의 닫기 버튼
    public static let sheetClose: CGFloat = 32
    /// 지역 시트 안의 사진. 화면에 끼워 넣느라 좁아진 만큼 `photo` 보다 작습니다.
    public static let sheetPhoto: CGFloat = 84

    /// 달력 한 칸의 **최소** 높이. 실제 높이는 폭을 7로 나눠 정해집니다.
    public static let dayCell: CGFloat = 38
    /// 달 넘김 버튼 (‹ ›)
    public static let monthNav: CGFloat = 34
    /// 달력 아래 목록의 사진
    public static let calendarPhoto: CGFloat = 68

    /// 올리기 목록의 사진. 옆에 슬롯 두 줄이 서므로 그 두 줄 높이와 맞춥니다.
    public static let uploadThumb: CGFloat = 58

    /**
     시트 손잡이 홈의 길이. 몸통이 통째로 올라오는 시트에서 잉크 선 대신 씁니다 —
     두께는 `stripe` 와 같아서 목록 화면 위쪽 줄무늬와 한 벌로 읽힙니다.
     */
    public static let grip: CGFloat = 44
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
