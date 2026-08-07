import CoreModel
import DesignSystem
import Foundation
import ImageIO
import PhotosUI
import SwiftUI
import UniformTypeIdentifiers

/**
 사진 올리기 시트.

 순서가 화면 순서입니다 — **사진 고르기 → 어디 → 언제 → 올리기.**
 EXIF 에서 읽어낸 값은 미리 채워 두고 "자동" 딱지를 붙입니다. 맞으면 그냥 올리면 되고,
 틀리면 눌러서 고칩니다. 안드로이드 `UploadSheet` 와 같은 흐름입니다.

 **높이는 내용이 정합니다.** 사진 한 장을 올릴 때 시트가 화면 반을 먹을 까닭이
 없습니다. 사진이 많아지면 목록만 `uploadList` 에서 멈추고 그 안에서 구릅니다 —
 머리말과 아래 버튼은 그 밖이라 몇 장을 골랐든 늘 보입니다.
 */
public struct UploadView: View {
    @State private var store: UploadStore
    @State private var picked: [PhotosPickerItem] = []
    /// 날짜를 고치는 중인 사진.
    @State private var pickingDateOf: String?
    /// 날짜 시트 높이. **재서** 씁니다 — 달력이 필요한 만큼만 차지합니다.
    @State private var dateHeight: CGFloat = 400
    private let onClose: () -> Void

    public init(store: UploadStore, onClose: @escaping () -> Void) {
        self._store = State(initialValue: store)
        self.onClose = onClose
    }

    public var body: some View {
        Group {
            // 몸통 위에 화면을 끼우고 조작은 화면 밖으로 냅니다.
            if store.state.editingRegionOf != nil {
                PlasticRegionPicker(store: store)
            } else {
                PlasticUploadBody(
                    store: store,
                    onClose: onClose,
                    onPickDate: { pickingDateOf = $0 },
                    picked: $picked
                )
                .sheet(item: Binding(
                    get: { pickingDateOf.map(DateEdit.init(uri:)) },
                    set: { pickingDateOf = $0?.uri }
                )) { edit in
                    dateSheet(for: edit.uri)
                }
                .onChange(of: picked) { _, items in
                    Task { await load(items) }
                }
            }
        }
        // **세로를 채우지 않습니다.** 이 화면은 시트 안에 있고, 시트 높이는 이 내용을
        // 재서 정합니다(`SpaceDetailView` 의 uploadHeight). 여기서 `maxHeight: .infinity`
        // 를 주면 늘 화면 전체로 재져서 재는 의미가 없어집니다.
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .background(PlasticColor.body)
        .onChange(of: store.state.step) { _, step in
            // 다 올라가면 화면이 스스로 닫힙니다. "완료" 를 또 누르게 하지 않습니다.
            if step == .done { onClose() }
        }
    }

    // MARK: - 본 화면

    /**
     못 올렸을 때. **왜 안 됐는지와 사진이 어디 있는지**를 같이 말합니다 —
     실패만 알리면 사용자는 사진을 잃었다고 생각합니다.
    /**
     '어디'·'언제' 한 줄. **네모 상자입니다** — 눌러서 고치는 칸이라 눌릴 수 있게
     생겨야 합니다. 예전에는 밑줄만 있는 줄이었는데, 사진마다 두 줄씩 쌓이니
     어디까지가 한 사진인지 알 수 없었습니다.
    private func dateSheet(for uri: String) -> some View {
        let current = store.state.items.first { $0.uri == uri }?.takenOn
        return DatePicker(
            "",
            selection: Binding(
                get: { current?.asDate ?? Date() },
                set: { store.setDate(uri, CalendarDate(from: $0)) }
            ),
            displayedComponents: .date
        )
        .labelsHidden()
        .datePickerStyle(.graphical)
        .padding(MemorySpace.xl)
        // 높이는 **재서** 씁니다 — 달력이 필요한 만큼만. 다른 시트와 같은 규칙입니다.
        .background(
            GeometryReader { proxy in
                Color.clear
                    .onAppear { dateHeight = proxy.size.height }
                    .onChange(of: proxy.size.height) { _, value in dateHeight = value }
            }
        )
        .presentationDetents([.height(dateHeight)])
    }

    /// 고른 사진을 **임시 파일로 떨궈** 경로만 넘깁니다.
    /// Store 는 경로만 알면 되고, 사진 라이브러리를 몰라도 테스트할 수 있습니다.
    private func load(_ items: [PhotosPickerItem]) async {
        var paths: [PickedPhoto] = []
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent("upload", isDirectory: true)
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)

        for (index, item) in items.enumerated() {
            guard let data = try? await item.loadTransferable(type: Data.self) else { continue }
            let file = folder.appendingPathComponent("\(index)-\(UUID().uuidString).jpg")
            guard (try? data.write(to: file)) != nil else { continue }
            paths.append(PickedPhoto(uri: file.path))
        }
        await store.pick(paths)
    }

    // MARK: - 지역 고르기

}

/// `sheet(item:)` 에 넘기려면 Identifiable 이 필요합니다. 문자열 하나를 감싸는 껍데기입니다.
private struct DateEdit: Identifiable {
    let uri: String
    var id: String { uri }
}

/**
 사진 고르기 칸.

 `PhotosPicker` 의 라벨 자리는 **MainActor 가 아니라서** 거기서 `memoryMicro()` 같은
 디자인 시스템 함수를 바로 부르면 컴파일되지 않습니다. 별도 View 로 빼면 그 안쪽은
 다시 MainActor 라 평소처럼 쓸 수 있습니다.
 */
private struct PickerTile: View {
    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: "plus")
                .font(.system(size: 18, weight: .medium))
            Text(localized("upload_empty_pick")).memoryMicro()
        }
        .foregroundStyle(MemoryColor.ink2)
        .frame(width: 92, height: 92)
        .background(
            RoundedRectangle(cornerRadius: MemoryRadius.thumb, style: .continuous)
                .fill(MemoryColor.fill)
        )
    }
}

/// 아직 안 올라간 사진은 주소가 없어서 파일에서 바로 읽습니다.
///
/// **한 번만** 읽어 두고 씁니다. 그릴 때마다 읽으면 몇 MB짜리 원본을 화면이 움직일
/// 때마다 다시 여는 셈이 됩니다.
private struct LocalThumb: View {
    let path: String
    @State private var image: Image?

    var body: some View {
        RoundedRectangle(cornerRadius: MemoryRadius.thumb, style: .continuous)
            .fill(MemoryColor.fill)
            .overlay {
                image?
                    .resizable()
                    .scaledToFill()
                    .clipShape(RoundedRectangle(cornerRadius: MemoryRadius.thumb, style: .continuous))
            }
            .task(id: path) { image = await load() }
    }

    /// 썸네일로 쓸 만큼만 줄여서 읽습니다 (올릴 때 쓰는 것과 같은 길, 크기만 다름).
    private func load() async -> Image? {
        #if canImport(UIKit)
        let data = await Task.detached(priority: .userInitiated) {
            PhotoFileThumb.small(at: path)
        }.value
        return data.flatMap(UIImage.init(data:)).map { Image(uiImage: $0) }
        #else
        return nil
        #endif
    }
}

/// 화면용 작은 미리보기. 올리는 크기(760px)와 굳이 같을 필요가 없어 더 작게 만듭니다.
private enum PhotoFileThumb {
    static func small(at path: String) -> Data? {
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
}

extension CalendarDate {
    var asDate: Date {
        Calendar.current.date(from: DateComponents(year: year, month: month, day: day)) ?? Date()
    }

    init(from date: Date) {
        let parts = Calendar.current.dateComponents([.year, .month, .day], from: date)
        self.init(year: parts.year ?? 2026, month: parts.month ?? 1, day: parts.day ?? 1)
    }
}
