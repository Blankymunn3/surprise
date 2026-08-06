import CoreModel
import DesignSystem
import Foundation
import SwiftUI

/**
 **시험용 화면 — 패미컴 컨트롤러 스타일의 짜국 목록.**

 지금 앱의 목록은 `SpaceListView` 의 `list` 이고, 이 파일은 같은 상태를 다른 옷으로
 그린 것뿐입니다. 상태·Store·시트는 하나도 건드리지 않습니다 —
 켜고 끄는 것은 `SpaceListView` 의 `plasticTrial` 하나입니다.

 옮긴 규칙 (안드로이드 `SpaceListPlastic.kt` 와 같습니다):
 - 화면 바탕 = 회색 플라스틱 몸통
 - 사진이 놓이는 판 = 검정 페이스플레이트
 - 짜국 카드 = 버튼 하우징에 **움푹 끼운** 사진. 사진은 손대지 않습니다
 - 주 동작 = 빨간 A 버튼, 보조 = 검은 고무 알약

 글자 크기는 `MemoryFont` 의 단(25/17/15/13.5/12.5/11)을 그대로 씁니다.
 시안의 px 값을 pt 로 옮기면 안 됩니다 — 시안은 300px 폭이라 실제 폰보다 좁아서,
 그대로 옮기면 글씨가 작아집니다.
 */
struct PlasticListBody: View {
    let store: SpaceListStore
    let onOpen: (Space) -> Void

    var body: some View {
        VStack(spacing: 0) {
            brand
            stripes

            // 검정 페이스플레이트. 남는 세로를 다 먹고, 그 안에서만 목록이 구릅니다.
            plate
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .sunken(PlasticRadius.screen)

            controls
        }
        .padding(.horizontal, MemorySpace.s)
        .background(PlasticColor.body)
    }

    @ViewBuilder
    private var plate: some View {
        switch store.state.spaces {
        case .loading:
            plateHint("불러오는 중이에요")
        case .failed:
            plateHint("목록을 불러오지 못했어요")
        case .ready(let items):
            if items.isEmpty {
                plateEmpty
            } else {
                ScrollView {
                    LazyVStack(spacing: MemorySpace.m) {
                        ForEach(items) { space in
                            PlasticCard(space: space) { onOpen(space) }
                        }
                    }
                    .padding(MemorySpace.m)
                }
            }
        }
    }

    /// 로고 자리. 기울임은 쓰지 않습니다 — Pretendard 에 진짜 이탤릭이 없어 흉내만 나옵니다.
    private var brand: some View {
        HStack(alignment: .lastTextBaseline, spacing: 0) {
            Text("짜국")
                .font(MemoryFont.font(25, .bold))
                .tracking(-0.5)
                .foregroundStyle(PlasticColor.red)
            Spacer(minLength: 0)
            Text("MAP & CALENDAR")
                .font(MemoryFont.font(11, .bold))
                .tracking(1.2)
                .foregroundStyle(PlasticColor.trimLo)
        }
        .padding(.horizontal, MemorySpace.xs)
        .padding(.top, MemorySpace.xs)
        .padding(.bottom, MemorySpace.s)
    }

    /// 몸통에 새긴 회색 줄무늬 셋. 컨트롤러 얼굴의 그 줄입니다.
    private var stripes: some View {
        VStack(spacing: MemorySpace.xs) {
            ForEach(0..<3, id: \.self) { _ in
                Capsule().fill(PlasticColor.trim).frame(height: PlasticSize.stripe)
            }
        }
        .padding(.horizontal, MemorySpace.xs)
        .padding(.bottom, MemorySpace.s)
    }

    /**
     아래 조작부 — 고무 알약과 빨간 A 버튼.

     **SELECT · START 라벨은 뺐습니다.** 컨트롤러에는 그 글자가 찍혀 있지만, 사진첩
     앱에서는 무엇을 하는 버튼인지 알려 주지 않는 장식일 뿐이라 뜬금없어 보입니다.
     형태(고무 알약 · 빨간 원 · 플라스틱 하우징)만으로 이미 컨트롤러로 읽힙니다.
     */
    private var controls: some View {
        HStack(spacing: MemorySpace.m) {
            Button { Task { await store.send(.joinTapped) } } label: {
                Text("초대 코드로 참여")
                    .font(MemoryFont.font(15, .bold))
                    .foregroundStyle(PlasticColor.onRubber)
                    .frame(maxWidth: .infinity)
                    .frame(height: PlasticSize.button)
                    .background(Capsule().fill(PlasticColor.rubber))
            }
            .buttonStyle(.plain)
            .padding(PlasticSize.buttonInset)
            .raisedPlastic()

            Button { Task { await store.send(.createTapped) } } label: {
                Text("＋")
                    .font(MemoryFont.font(24, .bold))
                    .foregroundStyle(PlasticColor.onRed)
                    .frame(width: PlasticSize.button, height: PlasticSize.button)
                    .background(Circle().fill(PlasticColor.red))
            }
            .buttonStyle(.plain)
            .padding(PlasticSize.buttonInset)
            .raisedPlastic()
        }
        .padding(.horizontal, MemorySpace.xs)
        .padding(.vertical, MemorySpace.m)
    }

    private func plateHint(_ text: String) -> some View {
        Text(text)
            .font(MemoryFont.font(13.5, .semibold))
            .foregroundStyle(PlasticColor.onPlateDim)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var plateEmpty: some View {
        VStack(alignment: .leading, spacing: 0) {
            PhotoFramesScene()
                .aspectRatio(PhotoFramesScene.ratio, contentMode: .fit)
                .frame(maxWidth: 130)
            Text("아직 짜국이 없어요")
                .font(MemoryFont.font(17, .bold))
                .foregroundStyle(PlasticColor.onPlate)
                .padding(.top, MemorySpace.l)
            Text("아래 빨간 버튼으로 하나 만들어 보세요.")
                .font(MemoryFont.font(12.5, .regular))
                .foregroundStyle(PlasticColor.onPlateDim)
                .padding(.top, MemorySpace.s)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(.horizontal, MemorySpace.xxl)
    }
}

private struct PlasticCard: View {
    let space: Space
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: MemorySpace.s) {
                // 하우징 — 볼록한 플라스틱. 그 안에 사진을 움푹 끼웁니다.
                photo
                    .frame(maxWidth: .infinity)
                    .frame(height: PlasticSize.photo)
                    .sunken(PlasticRadius.chip, face: PlasticColor.plateLo)
                    .overlay(alignment: .topLeading) {
                        if space.kind == .personal { onlyHere }
                    }
                    .padding(PlasticSize.housingInset)
                    .raisedPlastic()

                // 이름줄은 검정 판 위에 그대로 놓입니다 — 여기에도 베벨을 주면 자글자글해집니다.
                HStack(alignment: .bottom, spacing: MemorySpace.s) {
                    VStack(alignment: .leading, spacing: 0) {
                        Text(space.name)
                            .font(MemoryFont.font(17, .bold))
                            .foregroundStyle(PlasticColor.onPlate)
                            .lineLimit(1)
                        Text(metaShort)
                            .font(MemoryFont.font(12.5, .semibold))
                            .foregroundStyle(PlasticColor.onPlateDim)
                    }
                    Spacer(minLength: 0)
                    crew
                }
                .padding(.horizontal, MemorySpace.xs)
            }
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var photo: some View {
        if let cover = space.coverPhotoURL, let url = URL(string: cover) {
            RemotePhoto(url: url) { PlasticColor.plateLo }
        } else {
            PlasticColor.plateLo
        }
    }

    private var onlyHere: some View {
        Text("이 폰에만")
            .font(MemoryFont.font(11, .bold))
            .tracking(0.4)
            .foregroundStyle(PlasticColor.plate)
            .padding(.horizontal, MemorySpace.s)
            .padding(.vertical, MemorySpace.xs)
            .background(Capsule().fill(PlasticColor.body))
            .padding(MemorySpace.s)
    }

    /// 멤버는 작은 플라스틱 칩. 셋까지 보이고 넘으면 어두운 칩에 +N.
    private var crew: some View {
        let initials = space.members.map(\.initial)
        let shown = Array(initials.prefix(3))
        let rest = initials.count - shown.count

        return HStack(spacing: -PlasticSize.chipOverlap) {
            ForEach(Array(shown.enumerated()), id: \.offset) { _, text in
                chip(text, filled: false)
            }
            if rest > 0 { chip("+\(rest)", filled: true) }
        }
    }

    private func chip(_ text: String, filled: Bool) -> some View {
        Text(text)
            .font(MemoryFont.font(11, .bold))
            .foregroundStyle(filled ? PlasticColor.body : PlasticColor.plate)
            .frame(width: PlasticSize.chip, height: PlasticSize.chip)
            .background(
                RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                    .fill(filled ? PlasticColor.trimLo : PlasticColor.body)
            )
    }

    /// "13 · 8곳 · 7.27" — 몰드된 라벨처럼 짧게 끊습니다.
    private var metaShort: String {
        guard space.photoCount > 0 else { return "아직 비어 있어요" }
        var parts = ["\(space.photoCount)", "\(space.regionCount)곳"]
        if let last = space.lastPhotoOn { parts.append("\(last.month).\(last.day)") }
        return parts.joined(separator: " · ")
    }
}
