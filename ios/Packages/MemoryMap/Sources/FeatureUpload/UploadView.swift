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
 */
public struct UploadView: View {
    @State private var store: UploadStore
    @State private var items: [PhotosPickerItem] = []
    private let onClose: () -> Void

    public init(store: UploadStore, onClose: @escaping () -> Void) {
        self._store = State(initialValue: store)
        self.onClose = onClose
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: MemorySpace.l) {
            handle

            if store.state.pickingRegion {
                regionPicker
            } else {
                main
            }
        }
        .padding(.horizontal, MemorySpace.xl)
        .padding(.bottom, MemorySpace.xxxl)
        .background(MemoryColor.surface)
        .onChange(of: store.state.step) { _, step in
            // 다 올라가면 시트가 스스로 닫힙니다. "완료" 를 또 누르게 하지 않습니다.
            if step == .done { onClose() }
        }
    }

    private var handle: some View {
        Capsule()
            .fill(MemoryColor.line2)
            .frame(width: 36, height: 4)
            .frame(maxWidth: .infinity)
            .padding(.top, 10)
    }

    // MARK: - 본 화면

    @ViewBuilder
    private var main: some View {
        Text("사진 올리기").memoryTitle()

        photoRow

        if let notice = store.state.mismatchNotice {
            // 나눠 올리라고 알려 주기만 합니다. 막지는 않습니다 — 일부러 그럴 수도 있어서요.
            Text(notice)
                .memoryLabel()
                .foregroundStyle(MemoryColor.accent)
        }

        field(title: "어디", auto: store.state.regionFromExif) {
            Button { store.startPickingRegion() } label: {
                rowLabel(store.state.region?.displayName ?? "지역 고르기",
                         placeholder: store.state.region == nil)
            }
        }

        field(title: "언제", auto: store.state.dateFromExif) {
            DatePicker(
                "",
                selection: Binding(
                    get: { store.state.takenOn?.asDate ?? Date() },
                    set: { store.setDate(CalendarDate(from: $0)) }
                ),
                displayedComponents: .date
            )
            .labelsHidden()
            .datePickerStyle(.compact)
        }

        if case .failed = store.state.step {
            Text("올리지 못했어요. 사진은 그대로 있으니 잠시 뒤 다시 해 주세요.")
                .memoryLabel()
                .foregroundStyle(MemoryColor.accent)
        }

        PrimaryButton(buttonTitle, enabled: store.state.canUpload) {
            Task { await store.confirm() }
        }
    }

    private var buttonTitle: String {
        switch store.state.step {
        case .reading: "읽는 중…"
        case .uploading: "올리는 중…"
        default: store.state.picked.isEmpty ? "사진을 골라 주세요" : "\(store.state.picked.count)장 올리기"
        }
    }

    private var photoRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: MemorySpace.s) {
                PhotosPicker(selection: $items, matching: .images, photoLibrary: .shared()) {
                    VStack(spacing: 4) {
                        Image(systemName: "plus")
                            .font(.system(size: 18, weight: .medium))
                        Text("고르기").memoryMicro()
                    }
                    .foregroundStyle(MemoryColor.ink2)
                    .frame(width: 92, height: 92)
                    .background(
                        RoundedRectangle(cornerRadius: MemoryRadius.thumb, style: .continuous)
                            .fill(MemoryColor.fill)
                    )
                }

                ForEach(store.state.picked) { picked in
                    LocalThumb(path: picked.uri)
                        .frame(width: 92, height: 92)
                }
            }
        }
        .onChange(of: items) { _, picked in
            Task { await load(picked) }
        }
    }

    /// 고른 사진을 **임시 파일로 떨궈** 경로만 넘깁니다.
    /// Store 는 경로만 알면 되고, 사진 라이브러리를 몰라도 테스트할 수 있습니다.
    private func load(_ picked: [PhotosPickerItem]) async {
        var paths: [PickedPhoto] = []
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent("upload", isDirectory: true)
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)

        for (index, item) in picked.enumerated() {
            guard let data = try? await item.loadTransferable(type: Data.self) else { continue }
            let file = folder.appendingPathComponent("\(index)-\(UUID().uuidString).jpg")
            guard (try? data.write(to: file)) != nil else { continue }
            paths.append(PickedPhoto(uri: file.path))
        }
        await store.pick(paths)
    }

    // MARK: - 지역 고르기

    @ViewBuilder
    private var regionPicker: some View {
        HStack {
            Text("어디였나요?").memoryTitle()
            Spacer()
            Button("닫기") { store.cancelPickingRegion() }
                .memoryLabel()
                .foregroundStyle(MemoryColor.ink2)
        }

        TextField("지역 검색", text: Binding(
            get: { store.state.regionQuery },
            set: { value in Task { await store.search(value) } }
        ))
        .textFieldStyle(.plain)
        .memoryBody()
        .padding(.horizontal, MemorySpace.l)
        .padding(.vertical, MemorySpace.m)
        .background(
            RoundedRectangle(cornerRadius: MemoryRadius.button, style: .continuous)
                .fill(MemoryColor.fill)
        )

        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ForEach(store.state.regionResults) { region in
                    Button { store.choose(region) } label: {
                        HStack {
                            Text(region.name).memoryBody()
                            Spacer()
                            if let parent = region.parentName {
                                Text(parent).memoryLabel().foregroundStyle(MemoryColor.ink3)
                            }
                        }
                        .padding(.vertical, MemorySpace.m)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .frame(maxHeight: 280)
    }

    // MARK: - 작은 것들

    @ViewBuilder
    private func field(
        title: String, auto: Bool, @ViewBuilder content: () -> some View
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Text(title).memoryLabel().foregroundStyle(MemoryColor.ink2)
                if auto {
                    // 왜 이미 채워져 있는지 알려 줍니다. 안 그러면 "내가 넣었나?" 가 됩니다.
                    Text("자동")
                        .memoryMicro()
                        .foregroundStyle(MemoryColor.accent)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 2)
                        .background(Capsule().fill(MemoryColor.accentTint))
                }
            }
            content()
        }
    }

    private func rowLabel(_ text: String, placeholder: Bool) -> some View {
        HStack {
            Text(text)
                .memoryBody()
                .foregroundStyle(placeholder ? MemoryColor.ink3 : MemoryColor.ink)
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(MemoryColor.ink3)
        }
        .padding(.horizontal, MemorySpace.l)
        .padding(.vertical, MemorySpace.m)
        .background(
            RoundedRectangle(cornerRadius: MemoryRadius.button, style: .continuous)
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
