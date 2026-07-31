import Foundation
import SwiftUI

/**
 언덕과 나무, 그 사이로 난 길. `docs/app/design.html` 표지의 그림입니다.

 **선이 아니라 면으로** 그립니다 — 테두리도 그림자도 없습니다. 그림은 사진이 없는
 자리를 채우는 것이지 눈길을 끌려는 것이 아니라서요.

 좌표는 0~1 로 적고 그릴 때 크기를 곱합니다. 어떤 크기로 놓아도 같은 그림이 됩니다.
 안드로이드 `HillScene` 과 **같은 좌표**를 씁니다 — 다르면 두 앱의 그림이 달라집니다.
 */
public struct HillScene: View {
    /// 그림의 가로:세로. 두 앱이 같아야 같은 자리에서 잘립니다.
    public static let ratio: CGFloat = 300.0 / 160.0

    private let showsTrail: Bool
    private let showsSky: Bool

    public init(showsTrail: Bool = true, showsSky: Bool = true) {
        self.showsTrail = showsTrail
        self.showsSky = showsSky
    }

    public var body: some View {
        Canvas { context, size in
            let w = size.width, h = size.height
            func x(_ v: CGFloat) -> CGFloat { v * w }
            func y(_ v: CGFloat) -> CGFloat { v * h }

            if showsSky {
                context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(MemoryColor.mossSoft))
            }

            // 먼 언덕
            var far = Path()
            far.move(to: CGPoint(x: 0, y: y(0.58)))
            far.addCurve(to: CGPoint(x: x(0.52), y: y(0.47)),
                         control1: CGPoint(x: x(0.16), y: y(0.40)),
                         control2: CGPoint(x: x(0.34), y: y(0.52)))
            far.addCurve(to: CGPoint(x: w, y: y(0.48)),
                         control1: CGPoint(x: x(0.72), y: y(0.41)),
                         control2: CGPoint(x: x(0.86), y: y(0.54)))
            far.addLine(to: CGPoint(x: w, y: h))
            far.addLine(to: CGPoint(x: 0, y: h))
            far.closeSubpath()
            context.fill(far, with: .color(Self.farHill))

            // 가까운 언덕
            var near = Path()
            near.move(to: CGPoint(x: 0, y: y(0.74)))
            near.addCurve(to: CGPoint(x: x(0.58), y: y(0.70)),
                          control1: CGPoint(x: x(0.20), y: y(0.62)),
                          control2: CGPoint(x: x(0.38), y: y(0.76)))
            near.addCurve(to: CGPoint(x: w, y: y(0.72)),
                          control1: CGPoint(x: x(0.78), y: y(0.64)),
                          control2: CGPoint(x: x(0.90), y: y(0.76)))
            near.addLine(to: CGPoint(x: w, y: h))
            near.addLine(to: CGPoint(x: 0, y: h))
            near.closeSubpath()
            context.fill(near, with: .color(Self.nearHill))

            if showsTrail {
                // 길 — 점선을 겹쳐 밟고 간 자국처럼 보이게 합니다
                var trail = Path()
                trail.move(to: CGPoint(x: x(0.10), y: y(0.97)))
                trail.addCurve(to: CGPoint(x: x(0.42), y: y(0.72)),
                               control1: CGPoint(x: x(0.26), y: y(0.86)),
                               control2: CGPoint(x: x(0.30), y: y(0.76)))
                trail.addCurve(to: CGPoint(x: x(0.80), y: y(0.74)),
                               control1: CGPoint(x: x(0.56), y: y(0.68)),
                               control2: CGPoint(x: x(0.66), y: y(0.76)))
                context.stroke(trail, with: .color(Self.trail),
                               style: StrokeStyle(lineWidth: h * 0.055, lineCap: .round))
                context.stroke(trail, with: .color(Self.trailDots.opacity(0.7)),
                               style: StrokeStyle(lineWidth: h * 0.055, lineCap: .round,
                                                  dash: [h * 0.012, h * 0.09]))
            }

            tree(&context, cx: x(0.16), cy: y(0.56), r: h * 0.20)
            tree(&context, cx: x(0.62), cy: y(0.44), r: h * 0.26)
            tree(&context, cx: x(0.89), cy: y(0.62), r: h * 0.15)

            // 작은 것들 — 열매·꽃. 아주 좁게만 씁니다.
            dot(&context, x(0.33), y(0.88), h * 0.022, MemoryColor.honey)
            dot(&context, x(0.36), y(0.92), h * 0.015, Self.honeyLight)
            dot(&context, x(0.70), y(0.89), h * 0.018, MemoryColor.accent.opacity(0.8))
        }
        // **비율을 스스로 정하지 않습니다.** `.fill` 을 걸면 부모보다 커져서 카드가
        // 화면 밖으로 밀려납니다. 그림은 0~1 좌표라 어떤 크기를 받아도 알아서 맞습니다.
        // 홀로 놓을 때는 부르는 쪽에서 `HillScene.ratio` 를 걸어 주세요.
        .accessibilityHidden(true)
    }

    /// 나무 하나 — 줄기 하나에 잎 덩어리 셋.
    private func tree(_ context: inout GraphicsContext, cx: CGFloat, cy: CGFloat, r: CGFloat) {
        context.fill(
            Path(CGRect(x: cx - r * 0.12, y: cy, width: r * 0.24, height: r * 1.2)),
            with: .color(Self.bark)
        )
        dot(&context, cx, cy - r * 0.30, r, Self.leaf1)
        dot(&context, cx - r * 0.68, cy + r * 0.10, r * 0.66, Self.leaf2)
        dot(&context, cx + r * 0.66, cy + r * 0.14, r * 0.58, Self.leaf3)
    }

    private func dot(_ context: inout GraphicsContext, _ cx: CGFloat, _ cy: CGFloat,
                     _ r: CGFloat, _ color: Color) {
        context.fill(
            Path(ellipseIn: CGRect(x: cx - r, y: cy - r, width: r * 2, height: r * 2)),
            with: .color(color)
        )
    }

    private static let farHill = Color(hex: 0xC7DEC9)
    private static let nearHill = Color(hex: 0xA9CDAF)
    private static let trail = Color(hex: 0xEFE3CB)
    private static let trailDots = Color(hex: 0xDFCEAE)
    private static let bark = Color(hex: 0xB98C63)
    private static let leaf1 = Color(hex: 0x8FBE94)
    private static let leaf2 = Color(hex: 0x7FB489)
    private static let leaf3 = Color(hex: 0x9CC79F)
    private static let honeyLight = Color(hex: 0xF5D68F)
}
