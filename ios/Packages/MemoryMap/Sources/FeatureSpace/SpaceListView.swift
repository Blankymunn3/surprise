import CoreModel
import DesignSystem
import Foundation
import SwiftUI

/// 앱의 메인. **공간이 하나뿐이어도 여기서 시작합니다.**
/// 카드는 사진이 전부입니다 — 공간을 알아보는 건 이름이 아니라 사진이라서요.
public struct SpaceListView: View {
    @State private var store: SpaceListStore
    /// 시트 높이는 내용에 맞춰 재서 씁니다.
    @State private var sheetHeight: CGFloat = 260
    /// ID 만이 아니라 짜국을 통째로 넘깁니다 — 들어간 화면이 **종류**를 알아야
    /// 기기 안 사진을 볼지 서버 사진을 볼지 정할 수 있습니다.
    private let onOpen: (Space) -> Void

    public init(store: SpaceListStore, onOpen: @escaping (Space) -> Void) {
        self._store = State(initialValue: store)
        self.onOpen = onOpen
    }

    public var body: some View {
        // 만들기·참여·로그인·초대 코드는 **아래에서 올라오는 시트**입니다.
        // 목록을 잠깐 가리고 끝내는 일이라, 화면을 통째로 갈아 끼우면
        // 어디에서 하던 일인지 놓칩니다. 시트 안의 모양은 새 디자인 그대로입니다.
        list
            .task { await store.send(.appeared) }
            .sheet(isPresented: sheetShown) {
                SpaceSheet(store: store, onOpen: onOpen)
                    // 시트 높이는 **내용에 맞춥니다.** 만들기·참여·로그인·초대 코드가
                    // 각각 길이가 달라서 하나로 고정하면 어떤 것은 비고 어떤 것은 잘립니다.
                    .background(
                        GeometryReader { proxy in
                            Color.clear
                                .onAppear { sheetHeight = proxy.size.height }
                                .onChange(of: proxy.size.height) { _, value in sheetHeight = value }
                        }
                    )
                    .presentationDetents([.height(sheetHeight)])
                    .presentationDragIndicator(.hidden)
            }
    }

    /// 시트는 상태가 정하고 화면은 따라가기만 합니다. 닫으면 상태도 같이 닫힙니다 —
    /// 안 그러면 아래로 쓸어 내린 뒤 버튼을 눌러도 두 번째부터 안 열립니다.
    private var sheetShown: Binding<Bool> {
        Binding(
            get: { store.state.sheet != .none },
            set: { shown in
                if !shown { Task { await store.send(.sheetDismissed) } }
            }
        )
    }

    private var list: some View {
        VStack(spacing: 0) {
            header
            divider(inset: true)

            // 목록만 늘어납니다. 만들기·참여는 아래에 **붙박이로** 둡니다 —
            // 짜국이 늘어나도 그 둘을 찾으러 스크롤하지 않게요.
            body(for: store.state.spaces)
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            divider(inset: false)
            VStack(spacing: MemorySpace.s) {
                PrimaryButton(localized("list_create")) {
                    Task { await store.send(.createTapped) }
                }
                SoftButton(localized("list_join")) {
                    Task { await store.send(.joinTapped) }
                }
            }
            .padding(.horizontal, MemorySpace.xl)
            .padding(.top, MemorySpace.m)
            .padding(.bottom, MemorySpace.xxl)
        }
        .background(MemoryColor.paper)
    }

    @ViewBuilder
    private func body(for spaces: SpacesUi) -> some View {
        switch spaces {
        case .loading:
            hint(localized("list_loading"))
        case .failed:
            hint(localized("list_failed"))
        case .ready(let items):
            if items.isEmpty {
                emptyScene
            } else {
                ScrollView {
                    LazyVStack(spacing: 14) {
                        ForEach(items) { space in
                            SpaceCardView(space: space) { onOpen(space) }
                        }
                    }
                    .padding(.horizontal, MemorySpace.xl)
                    .padding(.top, MemorySpace.l)
                    .padding(.bottom, MemorySpace.s)
                }
            }
        }
    }

    /// 레드 사각 하나가 앱 이름 앞에 섭니다. 아이콘이 아니라 표식이라 뜻을 붙이지 않습니다.
    private var header: some View {
        HStack(alignment: .lastTextBaseline, spacing: 9) {
            Rectangle()
                .fill(MemoryColor.accent)
                .frame(width: 13, height: 13)
                .alignmentGuide(.lastTextBaseline) { $0[.bottom] }
            Text(localized("list_title")).memoryDisplay()
            Spacer()
            Text(localized("list_tagline"))
                .memoryMicro()
                .foregroundStyle(MemoryColor.ink2)
        }
        .padding(.horizontal, MemorySpace.xl)
        .padding(.top, MemorySpace.s)
        .padding(.bottom, MemorySpace.m)
    }

    /// 구획선은 2px 입니다. 테두리(1px)보다 굵어야 '나누는 선' 으로 읽힙니다.
    private func divider(inset: Bool) -> some View {
        MemoryColor.line2
            .frame(height: MemoryStroke.divider)
            .padding(.horizontal, inset ? MemorySpace.xl : 0)
    }

    /// 첫 실행. **가운데 정렬하지 않습니다** — 글이 왼끝에 맞아야 다음에 올 목록과
    /// 같은 자리에서 시작하고, 짜국이 생겼을 때 화면이 통째로 움직인 것처럼 안 보입니다.
    private var emptyScene: some View {
        VStack(alignment: .leading, spacing: 14) {
            PhotoFramesScene()
                .aspectRatio(PhotoFramesScene.ratio, contentMode: .fit)
                .frame(maxWidth: 150)
            Text(localized("list_empty_title")).memoryTitle()
            Text(emptyBlurb)
                .memoryLabel()
                .foregroundStyle(MemoryColor.ink2)
                .lineSpacing(4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 28)
    }

    /// **지도**와 **달력** 만 굵게. 이 앱이 무엇인지가 그 두 낱말에 들어 있습니다.
    private var emptyBlurb: AttributedString {
        var text = AttributedString(localized("list_empty_blurb"))
        for word in [localized("list_empty_bold_map"), localized("list_empty_bold_calendar")]
        where text.range(of: word) != nil {
            let range = text.range(of: word)!
            text[range].font = MemoryFont.headline
            text[range].foregroundColor = MemoryColor.ink
        }
        return text
    }

    private func hint(_ text: String) -> some View {
        Text(text)
            .memoryBody()
            .foregroundStyle(MemoryColor.ink3)
            .frame(maxWidth: .infinity)
            .padding(.vertical, MemorySpace.xxxl)
    }

}

/// 카드 높이는 고정입니다. 사진 비율이 제각각이어도 목록의 리듬은 일정해야 합니다.
private let cardHeight: CGFloat = 150

struct SpaceCardView: View {
    let space: Space
    let onTap: () -> Void

    private var hasCover: Bool { space.coverPhotoURL.flatMap(URL.init(string:)) != nil }

    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .bottomLeading) {
                if let cover = space.coverPhotoURL, let url = URL(string: cover) {
                    RemotePhoto(url: url) { MemoryColor.fill }

                    // 흰 글자가 밝은 사진 위에서도 읽히도록 아래쪽만 어둡게.
                    // 사진에 색을 입히는 틴트가 아니라, 글자 있는 쪽에만 두는 그늘입니다.
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0.38),
                            .init(color: MemoryColor.ink.opacity(0.78), location: 1),
                        ],
                        startPoint: .top, endPoint: .bottom
                    )
                } else {
                    MemoryColor.fill
                }

                HStack(alignment: .bottom, spacing: 10) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(space.name)
                            .memoryTitle()
                            .foregroundStyle(hasCover ? Color.white : MemoryColor.ink)
                        Text(meta)
                            .memoryMicro()
                            .foregroundStyle(hasCover ? Color.white.opacity(0.82) : MemoryColor.ink2)
                    }
                    Spacer(minLength: 0)
                    MemberAvatars(initials: space.members.map(\.initial))
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 12)
            }
            .frame(height: cardHeight)
            .clipped()
            .overlay(alignment: .topLeading) {
                if space.kind == .personal { onlyOnThisPhone }
            }
            .shadow(color: MemoryColor.ink.opacity(0.16), radius: 6, y: 2)
        }
        .buttonStyle(.plain)
    }

    /// 사진이 이 기기 안에만 있다는 표시.
    ///
    /// **다는 쪽이 예외입니다** — 같이 쓰는 짜국에는 아무것도 달지 않습니다. 둘 다 달면
    /// 목록이 딱지투성이가 되고 어느 쪽이 특별한지도 알 수 없습니다.
    ///
    /// 레드를 쓰지 않는 이유: 누르는 것이 아니라 그냥 알려 주는 것이라서요.
    private var onlyOnThisPhone: some View {
        Text(SharedText.onlyOnThisPhone)
            .memoryMicro()
            .foregroundStyle(MemoryColor.ink)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(MemoryColor.surface)
            .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
            .padding(12)
    }

    /// 사진이 없으면 수를 세지 않고 그 사실만 말합니다.
    /// "사진 0 · 지역 0" 은 셈이 아니라 잡음입니다.
    private var meta: String {
        guard space.photoCount > 0 else { return localized("card_meta_empty") }
        var parts = ["사진 \(space.photoCount)", "지역 \(space.regionCount)"]
        if let last = space.lastPhotoOn { parts.append("\(last.month)월 \(last.day)일") }
        return parts.joined(separator: " · ")
    }
}
