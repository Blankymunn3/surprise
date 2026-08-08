import SwiftUI

/// 구글 4색 G. **로고라 색을 우리 팔레트로 바꾸지 않습니다.**
/// 안드로이드 `GoogleMark` 와 같은 경로·같은 색입니다.
struct GoogleMark: View {
    var body: some View {
        Canvas { context, size in
            let scale = min(size.width, size.height) / 48
            for (path, color) in Self.paths {
                var shape = Path()
                shape.addPath(path, transform: .init(scaleX: scale, y: scale))
                context.fill(shape, with: .color(color))
            }
        }
    }

    private static let paths: [(Path, Color)] = [
        (blue, Color(red: 0x42 / 255, green: 0x85 / 255, blue: 0xF4 / 255)),
        (green, Color(red: 0x34 / 255, green: 0xA8 / 255, blue: 0x53 / 255)),
        (yellow, Color(red: 0xFB / 255, green: 0xBC / 255, blue: 0x05 / 255)),
        (red, Color(red: 0xEA / 255, green: 0x43 / 255, blue: 0x35 / 255)),
    ]

    private static var blue: Path {
        var p = Path()
        p.move(to: CGPoint(x: 45.12, y: 24.5))
        p.addCurve(to: CGPoint(x: 44.72, y: 20), control1: CGPoint(x: 45.12, y: 22.94), control2: CGPoint(x: 44.98, y: 21.44))
        p.addLine(to: CGPoint(x: 24, y: 20))
        p.addLine(to: CGPoint(x: 24, y: 28.51))
        p.addLine(to: CGPoint(x: 35.84, y: 28.51))
        p.addCurve(to: CGPoint(x: 31.45, y: 35.15), control1: CGPoint(x: 35.33, y: 31.26), control2: CGPoint(x: 33.78, y: 33.59))
        p.addLine(to: CGPoint(x: 31.45, y: 40.67))
        p.addLine(to: CGPoint(x: 38.56, y: 40.67))
        p.addCurve(to: CGPoint(x: 45.12, y: 24.5), control1: CGPoint(x: 42.72, y: 36.84), control2: CGPoint(x: 45.12, y: 31.2))
        p.closeSubpath()
        return p
    }

    private static var green: Path {
        var p = Path()
        p.move(to: CGPoint(x: 24, y: 46))
        p.addCurve(to: CGPoint(x: 38.56, y: 40.67), control1: CGPoint(x: 29.94, y: 46), control2: CGPoint(x: 34.92, y: 44.03))
        p.addLine(to: CGPoint(x: 31.45, y: 35.15))
        p.addCurve(to: CGPoint(x: 24, y: 37.25), control1: CGPoint(x: 29.48, y: 36.47), control2: CGPoint(x: 26.96, y: 37.25))
        p.addCurve(to: CGPoint(x: 11.69, y: 28.18), control1: CGPoint(x: 18.27, y: 37.25), control2: CGPoint(x: 13.42, y: 33.38))
        p.addLine(to: CGPoint(x: 4.34, y: 28.18))
        p.addLine(to: CGPoint(x: 4.34, y: 33.88))
        p.addCurve(to: CGPoint(x: 24, y: 46), control1: CGPoint(x: 7.96, y: 41.07), control2: CGPoint(x: 15.4, y: 46))
        p.closeSubpath()
        return p
    }

    private static var yellow: Path {
        var p = Path()
        p.move(to: CGPoint(x: 11.69, y: 28.18))
        p.addCurve(to: CGPoint(x: 11, y: 24), control1: CGPoint(x: 11.25, y: 26.86), control2: CGPoint(x: 11, y: 25.45))
        p.addCurve(to: CGPoint(x: 11.69, y: 19.82), control1: CGPoint(x: 11, y: 22.55), control2: CGPoint(x: 11.25, y: 21.14))
        p.addLine(to: CGPoint(x: 11.69, y: 14.12))
        p.addLine(to: CGPoint(x: 4.34, y: 14.12))
        p.addCurve(to: CGPoint(x: 2, y: 24), control1: CGPoint(x: 2.85, y: 17.09), control2: CGPoint(x: 2, y: 20.45))
        p.addCurve(to: CGPoint(x: 4.34, y: 33.88), control1: CGPoint(x: 2, y: 27.55), control2: CGPoint(x: 2.85, y: 30.91))
        p.closeSubpath()
        return p
    }

    private static var red: Path {
        var p = Path()
        p.move(to: CGPoint(x: 24, y: 10.75))
        p.addCurve(to: CGPoint(x: 32.41, y: 14.04), control1: CGPoint(x: 27.23, y: 10.75), control2: CGPoint(x: 30.13, y: 11.86))
        p.addLine(to: CGPoint(x: 38.72, y: 7.73))
        p.addCurve(to: CGPoint(x: 24, y: 2), control1: CGPoint(x: 34.91, y: 4.18), control2: CGPoint(x: 29.93, y: 2))
        p.addCurve(to: CGPoint(x: 4.34, y: 14.12), control1: CGPoint(x: 15.4, y: 2), control2: CGPoint(x: 7.96, y: 6.93))
        p.addLine(to: CGPoint(x: 11.69, y: 19.82))
        p.addCurve(to: CGPoint(x: 24, y: 10.75), control1: CGPoint(x: 13.42, y: 14.62), control2: CGPoint(x: 18.27, y: 10.75))
        p.closeSubpath()
        return p
    }
}
