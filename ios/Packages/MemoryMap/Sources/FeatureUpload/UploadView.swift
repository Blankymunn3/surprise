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
/**
 사진 올리기.

 **전체 화면입니다.** 사진마다 어디·언제를 보고 고치는 일이라, 지도를 가린 채
 시트 안에서 할 일이 아닙니다 — 여러 장을 훑어 내려야 합니다.
 EXIF 에서 읽어낸 값은 미리 채워 두고 "자동" 딱지를 붙입니다. 맞으면 그냥 올리면 되고,
 틀리면 눌러서 고칩니다. 안드로이드 `UploadSheet` 와 같은 흐름입니다.
 */
public struct UploadView: View {
    @State private var store: UploadStore
    @State private var picked: [PhotosPickerItem] = []
    /// 날짜를 고치는 중인 사진.
    @State private var pickingDateOf: String?
    private let onClose: () -> Void

    public init(store: UploadStore, onClose: @escaping () -> Void) {
        self._store = State(initialValue: store)
        self.onClose = onClose
    }

    public var body: some View {
        Group {
            if store.state.editingRegionOf != nil {
                regionPicker
            } else {
                main
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(MemoryColor.paper)
        .onChange(of: store.state.step) { _, step in
            // 다 올라가면 화면이 스스로 닫힙니다. "완료" 를 또 누르게 하지 않습니다.
            if step == .done { onClose() }
        }
    }

    // MARK: - 본 화면

    private var main: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            divider

            if case .failed(let savedLocally) = store.state.step {
                failureCard(savedLocally: savedLocally)
            }

            if store.state.items.isEmpty {
                emptyPick
            } else {
                autoNotice

                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        ForEach(store.state.items) { item in
                            itemRow(item)
                        }
                    }
                    .padding(.horizontal, MemorySpace.xl)
                    .padding(.top, MemorySpace.xs)
                    .padding(.bottom, MemorySpace.s)
                }

                MemoryColor.line2.frame(height: MemoryStroke.divider)
                VStack(alignment: .leading, spacing: MemorySpace.s) {
                    if let notice = store.state.splitNotice {
                        // 나눠 올린다고 알려 주기만 합니다. 막지는 않습니다 — 일부러 그럴 수도 있어서요.
                        Text(notice).memoryMicro().foregroundStyle(MemoryColor.ink2)
                    }
                    PrimaryButton(buttonTitle, enabled: store.state.canUpload) {
                        Task { await store.confirm() }
                    }
                }
                .padding(.horizontal, MemorySpace.xl)
                .padding(.top, MemorySpace.m)
                .padding(.bottom, MemorySpace.xxl)
            }
        }
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

    private var header: some View {
        HStack(spacing: 0) {
            Button(action: onClose) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(MemoryColor.ink)
                    .frame(width: 40, height: 40)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("닫기")

            Text("사진 올리기").memoryTitle()
            Spacer(minLength: 0)

            if !store.state.items.isEmpty {
                Text("\(store.state.items.count)장")
                    .memoryMicro()
                    .foregroundStyle(MemoryColor.ink)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 3)
                    .background(MemoryColor.surface)
                    .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
            }
        }
        .padding(.horizontal, 2)
        .padding(.vertical, 2)
    }

    private var divider: some View {
        MemoryColor.line2
            .frame(height: MemoryStroke.divider)
            .padding(.horizontal, MemorySpace.xl)
    }

    /// 자동으로 채웠다는 것과 **고칠 수 있다는 것**을 같이 알립니다.
    private var autoNotice: some View {
        HStack(alignment: .top, spacing: 6) {
            Text("ⓘ").memoryMicro().foregroundStyle(MemoryColor.ink2)
            Text("지역·날짜는 사진에서 자동으로 읽었어요. 눌러서 고치면 '자동' 표시가 사라져요.")
                .memoryMicro()
                .foregroundStyle(MemoryColor.ink2)
        }
        .padding(.horizontal, MemorySpace.xl)
        .padding(.top, MemorySpace.m)
        .padding(.bottom, MemorySpace.xs)
    }

    /**
     못 올렸을 때. **왜 안 됐는지와 사진이 어디 있는지**를 같이 말합니다 —
     실패만 알리면 사용자는 사진을 잃었다고 생각합니다.
     */
    private func failureCard(savedLocally: Bool) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("지금은 올릴 수 없어요").memoryBody().foregroundStyle(MemoryColor.accentDeep)
            Text(savedLocally
                ? "사진은 폰에 저장해 뒀어요. 연결되면 여기서 다시 시도해 주세요."
                : "잠시 뒤에 다시 시도해 주세요.")
                .memoryLabel()
                .foregroundStyle(MemoryColor.accentDeep)
                .padding(.top, 3)

            Button { store.retry() } label: {
                Text("다시 시도")
                    .memoryMicro()
                    .foregroundStyle(MemoryColor.accentDeep)
                    .padding(.horizontal, 11)
                    .padding(.vertical, 6)
                    .background(MemoryColor.surface)
                    .overlay(
                        Rectangle().strokeBorder(MemoryColor.accentDeep, lineWidth: MemoryStroke.border)
                    )
            }
            .buttonStyle(.plain)
            .padding(.top, MemorySpace.s)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, MemorySpace.m)
        .background(MemoryColor.surface)
        .overlay(Rectangle().strokeBorder(MemoryColor.accent, lineWidth: MemoryStroke.border))
        .padding(.horizontal, MemorySpace.xl)
        .padding(.top, MemorySpace.m)
    }

    private var emptyPick: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("올릴 사진을 골라 주세요").memoryTitle()
            PhotosPicker(selection: $picked, matching: .images, photoLibrary: .shared()) {
                PickerTile()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(.horizontal, 28)
    }

    private var buttonTitle: String {
        switch store.state.step {
        case .reading: "사진 읽는 중…"
        case .uploading: "올리는 중…"
        default: "\(store.state.items.count)장 올리기"
        }
    }

    /// 사진 한 장 — 왼쪽에 그림, 오른쪽에 '어디' 와 '언제' 두 줄.
    private func itemRow(_ item: UploadItem) -> some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: MemorySpace.m) {
                LocalThumb(path: item.uri).frame(width: 62, height: 62)

                VStack(spacing: 6) {
                    fieldRow(
                        label: "어디",
                        value: item.region?.displayName ?? "고르기",
                        dimmed: item.region == nil,
                        auto: item.regionAuto
                    ) { store.startPickingRegion(item.uri) }

                    fieldRow(
                        label: "언제",
                        value: "\(item.takenOn.month)월 \(item.takenOn.day)일",
                        dimmed: false,
                        auto: item.dateAuto
                    ) { pickingDateOf = item.uri }
                }
            }
            .padding(.vertical, 11)

            MemoryColor.fill.frame(height: MemoryStroke.border)
        }
    }

    /**
     '어디'·'언제' 한 줄. **네모 상자입니다** — 눌러서 고치는 칸이라 눌릴 수 있게
     생겨야 합니다. 예전에는 밑줄만 있는 줄이었는데, 사진마다 두 줄씩 쌓이니
     어디까지가 한 사진인지 알 수 없었습니다.
     */
    private func fieldRow(
        label: String, value: String, dimmed: Bool, auto: Bool, action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: MemorySpace.s) {
                Text(label)
                    .memoryMicro()
                    .foregroundStyle(MemoryColor.ink2)
                    .frame(width: 26, alignment: .leading)
                Text(value)
                    .memoryBody()
                    .foregroundStyle(dimmed ? MemoryColor.ink3 : MemoryColor.ink)
                    .lineLimit(1)
                Spacer(minLength: 0)
                if auto {
                    Text("자동")
                        .memoryMicro()
                        .foregroundStyle(MemoryColor.ink2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(MemoryColor.fill)
                }
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(MemoryColor.ink3)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(MemoryColor.surface)
            .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
        }
        .buttonStyle(.plain)
    }

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
        .presentationDetents([.medium])
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

    private var regionPicker: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 0) {
                Button { store.cancelPickingRegion() } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(MemoryColor.ink)
                        .frame(width: 40, height: 40)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("뒤로")

                Text("어디에서 찍었나요").memoryTitle()
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 2)
            .padding(.vertical, 2)

            divider

            TextField("지역 검색 — 강릉, 제주…", text: Binding(
                get: { store.state.regionQuery },
                set: { value in Task { await store.search(value) } }
            ))
            .textFieldStyle(.plain)
            .memoryBody()
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .background(MemoryColor.surface)
            .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
            .padding(.horizontal, MemorySpace.xl)
            .padding(.vertical, MemorySpace.m)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    ForEach(store.state.regionResults) { region in
                        Button { store.choose(region) } label: {
                            HStack(alignment: .lastTextBaseline, spacing: MemorySpace.s) {
                                Text(region.name).memoryBody()
                                if let parent = region.parentName {
                                    Text(parent).memoryMicro().foregroundStyle(MemoryColor.ink2)
                                }
                                Spacer(minLength: 0)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.vertical, 13)
                        }
                        .buttonStyle(.plain)

                        MemoryColor.fill.frame(height: MemoryStroke.border)
                    }
                }
                .padding(.horizontal, MemorySpace.xl)
            }
        }
    }
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
            Text("고르기").memoryMicro()
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
