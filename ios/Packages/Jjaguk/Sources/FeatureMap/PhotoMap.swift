import CoreModel
import DesignSystem
import MapKit
import SwiftUI

#if canImport(UIKit)
import UIKit

/**
 MKMapView 로 그리는 지도.

 SwiftUI `Map` 을 버린 이유: `MapPolygon` 은 **색으로만** 칠할 수 있습니다.
 `ImagePaint` 를 줘도 조용히 무시되어, 다녀온 지역을 대표사진으로 채우는 이 앱의
 핵심 그림이 iOS 에서만 안 나왔습니다. MKMapView 의 오버레이 렌더러는 CGContext 를
 그대로 주므로 사진을 지역 모양으로 오려 그릴 수 있습니다 — 안드로이드
 (`ImageSource` + `RasterLayer`)와 같은 그림이 됩니다.

 카메라 계약은 SwiftUI 지도와 같게 유지합니다 — `position` 은 **명령**(부모가 넣으면
 지도가 따라가고), `visibleRegion` 은 **보고**(지도가 실제로 보여 주는 범위)입니다.
 십자키·확대축소·내 위치가 전부 이 두 값으로 만들어져 있어서, 계약을 지키면
 그쪽 코드는 한 줄도 안 바뀝니다.
 */
struct PhotoMap: UIViewRepresentable {
    @Binding var position: MapCameraPosition
    @Binding var visibleRegion: MKCoordinateRegion?
    let fills: [RegionFill]
    let covers: [String: CGImage]
    let outline: [[GeoPoint]]
    let me: CLLocationCoordinate2D?
    let onTap: (Double, Double) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        let setup = MKStandardMapConfiguration(elevationStyle: .flat)
        setup.pointOfInterestFilter = .excludingAll
        map.preferredConfiguration = setup
        // 어두운 지도. 검정 판에 끼운 화면 안에서 하얀 지도가 혼자 빛나면
        // 화면이 아니라 구멍처럼 보입니다. 앱 전체가 아니라 지도만 어둡습니다.
        map.overrideUserInterfaceStyle = .dark
        map.showsCompass = false
        map.delegate = context.coordinator

        let tap = UITapGestureRecognizer(target: context.coordinator, action: #selector(Coordinator.tapped))
        map.addGestureRecognizer(tap)
        return map
    }

    func updateUIView(_ map: MKMapView, context: Context) {
        let coordinator = context.coordinator
        coordinator.parent = self

        // 카메라 명령. 마지막으로 따른 명령과 같으면 무시합니다 — `updateUIView` 는
        // 다시 그릴 때마다 불려서, 매번 따르면 손으로 민 지도가 도로 튕겨 갑니다.
        if let region = position.region, coordinator.lastApplied != Camera(region) {
            coordinator.lastApplied = Camera(region)
            coordinator.applying = true
            map.setRegion(region, animated: true)
        }

        // 지역 채우기. 서명이 같으면 손대지 않습니다 — 경계 점이 수천 개라
        // 안의 값을 견주는 것도, 다시 만드는 것도 비쌉니다.
        let drawing = fills.map { "\($0.code)=\($0.coverURL):\(covers[$0.coverURL] != nil)" }
            .joined(separator: "|")
        if coordinator.drawn != drawing {
            coordinator.drawn = drawing
            map.removeOverlays(coordinator.fillOverlays)
            coordinator.fillOverlays = fills.compactMap { fill in
                covers[fill.coverURL].map { PhotoFill(fill: fill, image: $0) }
            }
            coordinator.fillOverlays.forEach { map.addOverlay($0, level: .aboveRoads) }
        }

        // 고른 지역의 테두리. 채우기와 **따로** 관리합니다 — 지역을 고를 때마다
        // 채우기의 좌표 수천 개를 다시 만들 이유가 없습니다.
        //
        // ⚠️ 서명에 **고리 수만 쓰면 안 됩니다.** 시군구는 대부분 고리가 하나라,
        // 다른 지역을 골라도 수가 같아 테두리가 옛 지역에 그대로 남습니다.
        // 첫 점의 좌표까지 봐야 "다른 지역" 이 구분됩니다.
        let outlined = "\(outline.reduce(0) { $0 + $1.count }):"
            + (outline.first?.first.map { "\($0.latitude),\($0.longitude)" } ?? "-")
        if coordinator.outlined != outlined {
            coordinator.outlined = outlined
            map.removeOverlays(coordinator.outlineOverlays)
            coordinator.outlineOverlays = outline.map { ring in
                var points = ring.map { CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude) }
                return MKPolyline(coordinates: &points, count: points.count)
            }
            // 채우기보다 위층입니다 — 사진으로 채워진 지역을 골라도 테두리가 보여야
            // "지금 이 지역을 보고 있다" 가 읽힙니다. 안드로이드와 같은 그림입니다.
            coordinator.outlineOverlays.forEach { map.addOverlay($0, level: .aboveLabels) }
        }

        // 내 자리. **사진 수 딱지는 없습니다** — 지역이 이미 그 사진으로 칠해져 있고,
        // 몇 장인지는 지역 시트가 말합니다. 딱지까지 찍으면 같은 말을 두 번 하는 셈이고
        // 정작 사진을 가립니다. 웹과 같은 그림입니다.
        let marks = "me:\(me.map { "\($0.latitude),\($0.longitude)" } ?? "-")"
        if coordinator.marked != marks {
            coordinator.marked = marks
            map.removeAnnotations(map.annotations)
            if let me {
                map.addAnnotation(MeMark(coordinate: me))
            }
        }
    }

    /// 좌표 여섯 자리(cm 단위)면 "같은 명령" 을 가리기에 충분합니다.
    /// `MKCoordinateRegion` 이 Equatable 이 아니라 직접 만듭니다.
    struct Camera: Equatable {
        let a: Int, b: Int, c: Int, d: Int
        init(_ region: MKCoordinateRegion) {
            a = Int(region.center.latitude * 1e6)
            b = Int(region.center.longitude * 1e6)
            c = Int(region.span.latitudeDelta * 1e6)
            d = Int(region.span.longitudeDelta * 1e6)
        }
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        var parent: PhotoMap
        var lastApplied: Camera?
        /// 지금 움직임이 **우리 명령**인지. 아니면 사용자가 민 것이고,
        /// 그때는 명령 기억을 지웁니다 — 지워야 같은 자리로 "다시" 보내는 명령이 통합니다.
        var applying = false
        var drawn = ""
        var outlined = ""
        var marked = ""
        /// 우리가 얹은 오버레이들. 채우기와 테두리를 따로 걷어내려면 따로 들고 있어야
        /// 합니다 — `map.overlays` 를 통째로 지우면 서로가 서로를 지웁니다.
        var fillOverlays: [MKOverlay] = []
        var outlineOverlays: [MKOverlay] = []

        init(_ parent: PhotoMap) { self.parent = parent }

        func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
            parent.visibleRegion = mapView.region
            if applying {
                applying = false
            } else {
                lastApplied = nil
            }
        }

        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let fill = overlay as? PhotoFill {
                return PhotoFillRenderer(fill: fill)
            }
            if let line = overlay as? MKPolyline {
                let renderer = MKPolylineRenderer(polyline: line)
                // 이 스타일에서 테두리는 컨트롤러의 빨강입니다.
                renderer.strokeColor = UIColor(PlasticColor.red)
                renderer.lineWidth = 3
                return renderer
            }
            return MKOverlayRenderer(overlay: overlay)
        }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            if annotation is MeMark {
                let view = mapView.dequeueReusableAnnotationView(withIdentifier: "me")
                    ?? MKAnnotationView(annotation: annotation, reuseIdentifier: "me")
                view.annotation = annotation
                // 눌러도 밑의 지도가 받게 꺼 둡니다 — 표시일 뿐 버튼이 아닙니다.
                view.isEnabled = false
                meDot(into: view)
                return view
            }
            return nil
        }

        @objc func tapped(_ gesture: UITapGestureRecognizer) {
            guard let map = gesture.view as? MKMapView else { return }
            let coordinate = map.convert(gesture.location(in: map), toCoordinateFrom: map)
            parent.onTap(coordinate.latitude, coordinate.longitude)
        }

        /// 내가 지금 있는 자리. 지역 딱지(네모)와 다르게 생겨야 해서 원이고,
        /// 유일하게 테두리가 흰색입니다.
        private func meDot(into view: MKAnnotationView) {
            view.subviews.forEach { $0.removeFromSuperview() }
            view.frame = CGRect(x: 0, y: 0, width: 16, height: 16)
            let dot = UIView(frame: view.bounds)
            dot.backgroundColor = UIColor(PlasticColor.red)
            dot.layer.cornerRadius = 8
            dot.layer.borderColor = UIColor.white.cgColor
            dot.layer.borderWidth = 2
            view.addSubview(dot)
        }
    }
}

/**
 사진으로 칠할 지역 하나 — 오버레이 데이터.

 경계 상자와 고리들을 **지도 점(MKMapPoint) 좌표로 미리** 바꿔 둡니다.
 렌더러의 `draw` 는 타일마다 불려서, 그때마다 수천 개 좌표를 바꾸면 지도가 버벅입니다.
 */
final class PhotoFill: NSObject, MKOverlay {
    let image: CGImage
    /// 고리 하나 = 지도 점 목록 하나. 첫 고리가 몸이고 그다음부터는 구멍입니다.
    let rings: [[MKMapPoint]]
    let boundingMapRect: MKMapRect

    var coordinate: CLLocationCoordinate2D {
        MKMapPoint(x: boundingMapRect.midX, y: boundingMapRect.midY).coordinate
    }

    init(fill: RegionFill, image: CGImage) {
        self.image = image
        var rings: [[MKMapPoint]] = []
        var box = MKMapRect.null
        for polygon in fill.polygons {
            for ring in polygon {
                let points = ring.map {
                    MKMapPoint(CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude))
                }
                rings.append(points)
                for point in points {
                    box = box.union(MKMapRect(x: point.x, y: point.y, width: 0, height: 0))
                }
            }
        }
        self.rings = rings
        self.boundingMapRect = box
    }
}

/**
 대표사진 **한 장**을 지역 모양으로 오려 그립니다.

 안드로이드와 같은 규칙입니다 — 사진을 늘이지 않고 **가운데를 잘라** 경계 상자를
 꽉 채운 뒤, 지역 밖을 지웁니다. 구멍은 짝홀 규칙(`evenOdd`)이 알아서 뚫습니다.
 살짝 비치게(85%) 두는 것도 같습니다 — 완전히 덮으면 길·지명이 사라집니다.
 */
final class PhotoFillRenderer: MKOverlayRenderer {
    private let fill: PhotoFill

    init(fill: PhotoFill) {
        self.fill = fill
        super.init(overlay: fill)
    }

    override func draw(_ mapRect: MKMapRect, zoomScale: MKZoomScale, in context: CGContext) {
        let box = rect(for: fill.boundingMapRect)
        guard box.width > 0, box.height > 0 else { return }

        let path = CGMutablePath()
        for ring in fill.rings {
            guard let first = ring.first else { continue }
            path.move(to: point(for: first))
            for mapPoint in ring.dropFirst() {
                path.addLine(to: point(for: mapPoint))
            }
            path.closeSubpath()
        }

        context.saveGState()
        context.addPath(path)
        context.clip(using: .evenOdd)
        context.setAlpha(0.85)

        // 가운데 자르기 — 짧은 쪽에 맞춰 키우고 넘치는 만큼은 지역 밖이라 잘려 나갑니다.
        let scale = max(box.width / CGFloat(fill.image.width), box.height / CGFloat(fill.image.height))
        let drawSize = CGSize(width: CGFloat(fill.image.width) * scale, height: CGFloat(fill.image.height) * scale)
        let drawRect = CGRect(
            x: box.midX - drawSize.width / 2,
            y: box.midY - drawSize.height / 2,
            width: drawSize.width,
            height: drawSize.height
        )

        // CGContext 는 그림을 뒤집어 그립니다. 그 자리에서 위아래만 도로 뒤집습니다.
        context.translateBy(x: 0, y: drawRect.midY * 2)
        context.scaleBy(x: 1, y: -1)
        context.draw(fill.image, in: drawRect)
        context.restoreGState()
    }
}

/// 내가 지금 있는 자리.
final class MeMark: NSObject, MKAnnotation {
    let coordinate: CLLocationCoordinate2D
    init(coordinate: CLLocationCoordinate2D) { self.coordinate = coordinate }
}
#endif
