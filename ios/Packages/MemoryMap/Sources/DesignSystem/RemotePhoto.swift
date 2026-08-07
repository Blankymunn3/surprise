import SwiftUI

#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

/// 사진 한 장을 받아 그립니다.
///
/// **`AsyncImage` 를 안 쓰는 이유**: 헤더를 붙일 수가 없습니다. 규칙이 로그인을 요구하게
/// 되면 `AsyncImage` 로는 사진이 403 이 되어, 목록은 나오는데 사진만 안 뜹니다
/// (`docs/app/AUTH.md`). 안드로이드는 Coil 로더에 인터셉터를 다는 것으로 같은 일을 합니다.
///
/// 받아 둔 그림은 `URLSession` 의 기본 캐시(`URLCache`)에 남습니다 — 목록을 오르내릴 때마다
/// 새로 받지는 않습니다.
public struct RemotePhoto<Placeholder: View>: View {
    private let url: URL?
    private let placeholder: () -> Placeholder

    @Environment(\.photoToken) private var photoToken
    @State private var loaded: Image?

    public init(url: URL?, @ViewBuilder placeholder: @escaping () -> Placeholder) {
        self.url = url
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
        // 주소가 바뀌면 다시 받습니다. 안 그러면 목록에서 재사용될 때 옛 사진이 남습니다.
        .task(id: url) { await load() }
    }

    private func load() async {
        loaded = nil
        guard let url else { return }

        var request = URLRequest(url: url)
        // **우리 버킷으로 갈 때만** 토큰을 답니다. 남의 서버에 우리 토큰을 실어 보내면 안 됩니다.
        if url.host == "firebasestorage.googleapis.com", let token = await photoToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        // **HTTP 가 아닐 수도 있습니다.** 혼자 쓰는 짜국의 사진은 서버에 없고 기기 안에만
        // 있어서 주소가 `file://` 입니다. 그때는 응답에 상태 번호가 아예 없으므로,
        // 없으면 성공으로 봅니다 — 실패는 위에서 예외로 걸러집니다.
        // (`?? false` 로 두면 기기 안 사진이 전부 하얗게 나옵니다.)
        guard let (data, response) = try? await URLSession.shared.data(for: request),
              (response as? HTTPURLResponse).map({ (200..<300).contains($0.statusCode) }) ?? true,
              let image = Self.image(from: data)
        else { return }

        loaded = image
    }

    private static func image(from data: Data) -> Image? {
        #if canImport(UIKit)
        return UIImage(data: data).map(Image.init(uiImage:))
        #elseif canImport(AppKit)
        return NSImage(data: data).map(Image.init(nsImage:))
        #else
        return nil
        #endif
    }
}

/// 사진 요청에 얹을 Firebase ID 토큰. 앱 껍데기가 뿌리에서 한 번 꽂아 주면
/// 아래 모든 사진이 그것을 씁니다. 기본값은 `nil` — 로그인 전에는 헤더를 안 붙입니다.
public struct PhotoTokenKey: EnvironmentKey {
    public static let defaultValue: @Sendable () async -> String? = { nil }
}

public extension EnvironmentValues {
    var photoToken: @Sendable () async -> String? {
        get { self[PhotoTokenKey.self] }
        set { self[PhotoTokenKey.self] = newValue }
    }
}
