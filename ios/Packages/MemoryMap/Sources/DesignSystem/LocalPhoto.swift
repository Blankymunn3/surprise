import SwiftUI

#if canImport(UIKit)
import ImageIO
import UniformTypeIdentifiers
import UIKit
#endif

/**
 아직 **안 올린** 사진 한 장을 기기 파일에서 읽어 그립니다. `RemotePhoto` 의 짝입니다.

 왜 따로 있어야 하는가: 올리기 화면의 사진에는 아직 주소가 없습니다. 있는 것은
 기기 안의 **파일 경로**(`/var/.../upload/0-….jpg`)뿐입니다. 이걸 내려받는 로더에
 그대로 넘기면 `URL(string:)` 이 스킴 없는 주소를 만들고, 요청이 통째로 실패해서
 **자리만 있고 그림은 영영 안 나옵니다.**

 안드로이드는 Coil 이 `content://` 를 알아서 읽어 주므로 이런 짝이 필요 없습니다.
 */
public struct LocalPhoto<Placeholder: View>: View {
    private let path: String?
    private let placeholder: () -> Placeholder

    @State private var loaded: Image?

    public init(path: String?, @ViewBuilder placeholder: @escaping () -> Placeholder) {
        self.path = path
        self.placeholder = placeholder
    }

    public var body: some View {
        Group {
            if let loaded {
                loaded.resizable().scaledToFill()
            } else {
                placeholder()
            }
        }
        // 경로가 바뀌면 다시 읽습니다. 안 그러면 목록에서 재사용될 때 옛 사진이 남습니다.
        .task(id: path) { await load() }
    }

    private func load() async {
        loaded = nil
        guard let path else { return }
        #if canImport(UIKit)
        // 원본은 수천 픽셀입니다. 62점짜리 자리에 그대로 올리면 화면이 버벅입니다.
        let data = await Task.detached(priority: .userInitiated) { smallJpeg(at: path) }.value
        loaded = data.flatMap(UIImage.init(data:)).map { Image(uiImage: $0) }
        #endif
    }
}

#if canImport(UIKit)
/// 화면용 작은 미리보기. 올리는 크기(760px)와 굳이 같을 필요가 없어 더 작게 만듭니다.
private func smallJpeg(at path: String) -> Data? {
    guard let source = CGImageSourceCreateWithURL(URL(fileURLWithPath: path) as CFURL, nil),
          let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
              kCGImageSourceCreateThumbnailFromImageAlways: true,
              kCGImageSourceCreateThumbnailWithTransform: true,
              kCGImageSourceThumbnailMaxPixelSize: 240,
          ] as CFDictionary)
    else { return nil }

    let out = NSMutableData()
    guard let destination = CGImageDestinationCreateWithData(
        out, UTType.jpeg.identifier as CFString, 1, nil
    ) else { return nil }
    CGImageDestinationAddImage(destination, image, nil)
    guard CGImageDestinationFinalize(destination) else { return nil }
    return out as Data
}
#endif

/**
 올리기 화면의 사진 한 칸. **검은 화면 안에 움푹 끼운 그림**입니다.

 아직 못 읽었을 때 회색 판을 깔아 두는 이유: 자리가 비어 있으면 사진이 몇 장인지
 세다가 헷갈립니다. 다 읽고 나면 그 위에 그림이 덮습니다.
 */
public struct LocalPhotoThumb: View {
    private let path: String

    public init(path: String) { self.path = path }

    public var body: some View {
        LocalPhoto(path: path) { PlasticColor.plateHi }
            .clipShape(RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous))
    }
}
