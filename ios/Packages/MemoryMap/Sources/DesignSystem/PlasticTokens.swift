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
    /// 십자키 가운데 원 안의 작은 점
    public static let rubberHi = Color(hex: 0x4C4C4C)
    public static let onRubber = Color(hex: 0xC8C5C0)

    /// 검정 판 위의 글자
    public static let onPlate = Color(hex: 0xDCD9D3)
    public static let onPlateDim = Color(hex: 0x9E9B96)
    public static let onRed = Color(hex: 0xFFFFFF)

    /**
     **못 누르는 버튼.** 이 스타일에는 흐리게 하는 장치가 없어서(고무는 원래 검정)
     색으로 가릅니다 — 빨강을 어두운 쪽으로 내리고 글자는 몸통 색으로 둡니다.

     글자를 `onPlateDim` 으로 두지 않는 이유: 어두운 빨강 위의 회색은 대비가 너무
     낮아 **글자가 안 읽힙니다.** 못 누르는 것과 안 보이는 것은 다릅니다.

     ⚠️ 여기서는 `.disabled()` 가 **위에 한 번 더 흐리게** 합니다. 안드로이드에는
     그런 것이 없어서, 같은 색을 써도 iOS 쪽이 더 옅게 보였습니다. 그래서 애초에
     넉넉히 밝은 값으로 잡았습니다. 안드로이드 `RedOff`/`OnRedOff` 와 같은 값입니다.
     */
    public static let redOff = redLo
    public static let onRedOff = body
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

    /**
     십자키 한 변. 팔 하나는 이것의 1/3 이므로 **3의 배수**여야 팔이 딱 나뉩니다.

     지도 조작부는 십자키 · 알약 둘 · 빨간 버튼 둘이 **한 줄에** 서므로 폭이 빠듯합니다.
     이 값을 키우면 좁은 폰에서 오른쪽 버튼이 밀려납니다.
     */
    public static let cross: CGFloat = 90

    /// 가운데 고무 알약 (실물의 SELECT · START 자리, 여기서는 확대·축소)
    public static let pillWidth: CGFloat = 42
    public static let pillHeight: CGFloat = 20
    /// 빨간 A · B 버튼
    public static let redButton: CGFloat = 44
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

/**
 시트 손잡이 — **몸통에 새긴 회색 홈**입니다.

 아래에서 올라오는 판마다 하나씩 답니다. 기본 손잡이(`presentationDragIndicator`)를
 끄고 이것을 쓰는 이유: 시스템 손잡이는 둥근 진회색 막대라, 회색 플라스틱 위에서
 남의 앱 조각처럼 보입니다. 목록 화면 위쪽의 줄무늬와 같은 것입니다.

 손잡이가 하는 말이 하나 더 있습니다 — **끌어 내려 닫을 수 있다**는 것. 그래서
 닫기 버튼을 따로 두지 않는 시트에는 반드시 있어야 합니다.
 */
public struct PlasticGrip: View {
    public init() {}

    public var body: some View {
        Capsule()
            .fill(PlasticColor.trim)
            .frame(width: PlasticSize.grip, height: PlasticSize.stripe)
            .padding(.top, MemorySpace.s)
            .padding(.bottom, MemorySpace.xs)
    }
}

/**
 **누르면 내려앉습니다.**

 플라스틱 버튼에는 물결(ripple)이 없습니다 — 실제 버튼은 빛이 번지는 것이 아니라
 그냥 **내려갑니다.** `.buttonStyle(.plain)` 대신 이것을 씁니다.

 색을 어둡게 하지 않는 이유: 빨간 A 버튼은 이미 진한 빨강이고 고무는 검정이라
 더 어둡게 해도 거의 티가 안 납니다. **움직임이 훨씬 잘 읽힙니다.**

 기준 디자인에서는 아무 일도 하지 않습니다 — 그쪽은 종이와 잉크의 세계라
 눌러서 내려갈 두께가 없습니다. 안드로이드 `Modifier.pressable` 과 같습니다.
 */
public struct PlasticPressStyle: ButtonStyle {
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .offset(y: plasticTrial && configuration.isPressed ? pressDrop : 0)
            // 뗄 때가 누를 때보다 조금 느립니다. 손가락이 떨어진 뒤 버튼이 올라오는 것이
            // 눈에 보여야 "눌렀다" 가 완결됩니다.
            .animation(
                .easeOut(duration: configuration.isPressed ? 0.04 : 0.09),
                value: configuration.isPressed
            )
    }
}

public extension ButtonStyle where Self == PlasticPressStyle {
    /// 누르면 내려앉는 플라스틱 버튼. `.plain` 자리에 씁니다.
    static var plasticPress: PlasticPressStyle { PlasticPressStyle() }
}

/**
 눌렸을 때 내려가는 깊이.

 2 면 베벨(위 2 · 아래 3)만큼이라 버튼이 제 그림자 속으로 들어가는 것처럼 보입니다.
 더 깊게 하면 눌린 것이 아니라 화면이 흔들린 것으로 읽힙니다.
 */
private let pressDrop: CGFloat = 2

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
