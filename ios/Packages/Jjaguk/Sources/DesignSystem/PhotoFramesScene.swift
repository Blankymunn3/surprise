import SwiftUI

/// 빈 목록에 놓는 그림 — **겹쳐 놓은 사진틀 셋**입니다.
///
/// 앞의 한 장만 흰 면이고 뒤의 둘은 비었거나 회색인 이유: 아직 사진이 없다는 것을
/// 말이 아니라 그림으로 먼저 알리려는 것입니다. 레드 사각 하나가 구성을 잡아 줍니다.
///
/// 지도·달력 화면에는 지도 자체가 그림 역할을 하므로 이 그림을 쓰지 않습니다.
/// 안드로이드 `PhotoFramesScene` 과 **같은 좌표**를 씁니다 — 다르면 두 앱의 그림이 달라집니다.
///
/// **패미컴 스타일에서는 검정 판 위에 놓입니다.** 그래서 색을 뒤집습니다 — 선이 잉크면
/// 검정 위의 검정이라 아예 안 보이고, 면이 흰색이면 판 위에서 혼자 번쩍입니다.
/// 모양(좌표·굵기)은 그대로라 두 스타일이 같은 그림입니다.
public struct PhotoFramesScene: View {
    /// 원본 그림의 좌표계.
    private static let w: CGFloat = 150
    private static let h: CGFloat = 110

    public static let ratio = w / h

    public init() {}

    private var stroke: Color { PlasticColor.onPlateDim }
    private var back: Color { PlasticColor.plate }
    private var front: Color { PlasticColor.plateHi }
    private var mark: Color { PlasticColor.red }

    public var body: some View {
        Canvas { context, size in
            let k = size.width / Self.w
            func p(_ v: CGFloat) -> CGFloat { v * k }

            let line = StrokeStyle(lineWidth: 2 * k)

            func frame(_ left: CGFloat, _ top: CGFloat, fill: Color?) {
                let rect = CGRect(x: p(left), y: p(top), width: p(76), height: p(58))
                if let fill { context.fill(Path(rect), with: .color(fill)) }
                context.stroke(Path(rect), with: .color(stroke), style: line)
            }

            // 뒤에서 앞으로. 앞의 것이 뒤의 것을 가려야 겹쳐 보입니다.
            frame(6, 18, fill: nil)
            frame(26, 34, fill: back)
            frame(46, 50, fill: front)

            context.fill(
                Path(CGRect(x: p(98), y: p(10), width: p(22), height: p(22))),
                with: .color(mark)
            )

            // 앞장 안의 산등성이
            var ridge = Path()
            ridge.move(to: CGPoint(x: p(60), y: p(79)))
            ridge.addLine(to: CGPoint(x: p(74), y: p(64)))
            ridge.addLine(to: CGPoint(x: p(86), y: p(74)))
            ridge.addLine(to: CGPoint(x: p(98), y: p(60)))
            context.stroke(ridge, with: .color(stroke), style: line)
        }
    }
}
