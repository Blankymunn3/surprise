import CoreModel
import DesignSystem
import Foundation
import PhotosUI
import SwiftUI

/**
 **사진 올리기 — 패미컴 컨트롤러 스타일.**

 안드로이드 `UploadPlastic.kt` 와 같은 짜임새입니다. 몸통 위에 화면을 끼우고,
 조작(취소·올리기)은 화면 밖입니다.

 **'어디'·'언제' 칸은 지도의 카트리지 슬롯과 같은 모양입니다.** 둘 다 "값을 꽂아 넣는
 자리" 라서 같게 뒀습니다 — 이 스타일에서 파인 홈은 무언가를 넣는 곳이라는 뜻입니다.

 ⚠️ 목업은 사진 줄 하나에 '어디·언제' 한 벌이었지만, 실제 상태는 **사진 한 장마다**
 제 지역·날짜를 듭니다. 목업이 보여 준 것은 생김새지 짜임새가 아니라서,
 칸 모양만 가져오고 줄은 사진마다 그립니다.
 */
struct PlasticUploadBody: View {
    let store: UploadStore
    let onClose: () -> Void
    let onPickDate: (String) -> Void

    @Binding var picked: [PhotosPickerItem]

    var body: some View {
        // **높이를 채우지 않습니다.** 내용만큼만 자라고, 시트가 그 높이를 재서 씁니다
        // (`SpaceDetailView` 의 uploadHeight). 사진 한 장을 올릴 때 시트가 화면 반을
        // 먹을 까닭이 없습니다.
        VStack(spacing: 0) {
            PlasticGrip()
            header

            screen
                .frame(maxWidth: .infinity)
                .sunken(PlasticRadius.screen)

            controls
        }
        .padding(.horizontal, MemorySpace.s)
        .background(PlasticColor.body)
    }

    private var screen: some View {
        VStack(alignment: .leading, spacing: 0) {
            if case .failed(let savedLocally) = store.state.step {
                failurePlate(savedLocally: savedLocally)
                Spacer().frame(height: MemorySpace.s)
            }

            if store.state.items.isEmpty {
                emptyPlate
            } else {
                // **여기가 시트 높이를 정합니다.** 목록은 사진 수만큼 자라다가
                // 이 한도에서 멈추고 그 뒤로는 구릅니다 — 시트가 화면을 삼키지 않으면서도
                // 사진이 많을 때 훑어 내릴 수 있습니다.
                //
                // 머리말과 아래 버튼은 이 밖에 있어 늘 보입니다.
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        ForEach(store.state.items) { item in
                            itemRow(item)
                        }
                    }
                    .padding(.bottom, MemorySpace.xs)
                }
                .frame(maxHeight: uploadList)

                if let split = store.state.splitCounts {
                    // 나눠 올린다고 알려 주기만 합니다. 막지는 않습니다 — 일부러 그럴 수도 있어서요.
                    Text(localized("upload_split_notice", split.places, split.days))
                        .font(MemoryFont.font(11, .semibold))
                        .foregroundStyle(PlasticColor.onPlateDim)
                        .padding(.top, MemorySpace.xs)
                }
            }
        }
        .padding(MemorySpace.s)
        .frame(maxWidth: .infinity, alignment: .topLeading)
    }

    private var header: some View {
        HStack {
            Text(localized("upload_title"))
                .font(MemoryFont.font(17, .bold))
                .foregroundStyle(PlasticColor.ink)
            Spacer(minLength: 0)
            if !store.state.items.isEmpty {
                Text(localized("upload_count", store.state.items.count))
                    .font(MemoryFont.font(11, .bold))
                    .tracking(0.8)
                    .foregroundStyle(PlasticColor.trimLo)
            }
        }
        .padding(.horizontal, MemorySpace.xs)
        .padding(.vertical, MemorySpace.s)
    }

    /**
     사진 한 장 — 왼쪽에 그림, 오른쪽에 '어디'·'언제' 슬롯 둘.

     사진은 화면 안에 **움푹 끼웁니다.** 목록의 카드는 볼록한 하우징에 끼웠는데,
     거기는 카드가 기기 위에 놓인 물건이고 여기는 이미 화면 안이라 또 볼록하게 하면
     화면에서 플라스틱이 튀어나온 꼴이 됩니다.
     */
    private func itemRow(_ item: UploadItem) -> some View {
        HStack(alignment: .top, spacing: MemorySpace.s) {
            PhotoThumb(url: item.uri)
                .frame(width: PlasticSize.uploadThumb, height: PlasticSize.uploadThumb)
                .clipShape(RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous))

            VStack(alignment: .leading, spacing: MemorySpace.xs) {
                slotField(
                    label: localized("upload_field_where"),
                    value: item.region?.displayName ?? localized("upload_field_pick"),
                    dimmed: item.region == nil,
                    auto: item.regionAuto
                ) { store.startPickingRegion(item.uri) }

                slotField(
                    label: localized("upload_field_when"),
                    value: localized("upload_date", item.takenOn.month, item.takenOn.day),
                    dimmed: false,
                    auto: item.dateAuto
                ) { onPickDate(item.uri) }
            }
        }
        .padding(.vertical, MemorySpace.xs)
    }

    /**
     값을 꽂아 넣는 칸. 지도 검색칸과 같은 슬롯 모양입니다.

     왼쪽의 작은 검은 홈이 라벨 대신 "여기에 넣는다" 를 말합니다. 라벨 글자는 그 옆에
     작게 남겨 뒀습니다 — 홈만으로는 어디인지 언제인지 가릴 수 없습니다.
     */
    private func slotField(
        label: String,
        value: String,
        dimmed: Bool,
        auto: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: MemorySpace.s) {
                RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                    .fill(PlasticColor.ink)
                    .frame(width: 3, height: 12)

                Text(label)
                    .font(MemoryFont.font(11, .semibold))
                    .foregroundStyle(PlasticColor.onPlateDim)
                    .frame(width: 24, alignment: .leading)

                Text(value)
                    .font(MemoryFont.font(13.5, .semibold))
                    .foregroundStyle(dimmed ? PlasticColor.onPlateDim : PlasticColor.onPlate)
                    .lineLimit(1)

                Spacer(minLength: 0)

                if auto {
                    Text(localized("upload_auto_badge"))
                        .font(MemoryFont.font(11, .semibold))
                        .foregroundStyle(PlasticColor.onPlateDim)
                }
            }
            .padding(.horizontal, MemorySpace.s)
            .padding(.vertical, 7)
            .background(PlasticColor.plate)
            .clipShape(RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous))
        }
        .buttonStyle(.plasticPress)
    }

    /**
     못 올렸을 때. **왜 안 됐는지와 사진이 어디 있는지**를 같이 말합니다 —
     실패만 알리면 사용자는 사진을 잃었다고 생각합니다.

     화면 안이라 빨간 **면**을 쓰지 않고 빨간 글자와 왼쪽 선만 씁니다.
     검정 판 위에 빨간 상자를 놓으면 아래 조작부의 A 버튼보다 세게 튑니다.
     */
    private func failurePlate(savedLocally: Bool) -> some View {
        HStack(alignment: .top, spacing: MemorySpace.s) {
            Rectangle().fill(PlasticColor.red).frame(width: 3)

            VStack(alignment: .leading, spacing: 0) {
                Text(localized("upload_failed_title"))
                    .font(MemoryFont.font(13.5, .bold))
                    .foregroundStyle(PlasticColor.redHi)

                Text(localized(savedLocally ? "upload_failed_kept" : "upload_failed_plain"))
                    .font(MemoryFont.font(11, .semibold))
                    .foregroundStyle(PlasticColor.onPlateDim)
                    .padding(.top, 2)

                Button { store.retry() } label: {
                    Text(localized("upload_retry"))
                        .font(MemoryFont.font(11, .bold))
                        .foregroundStyle(PlasticColor.onRubber)
                        .padding(.horizontal, MemorySpace.m)
                        .padding(.vertical, 6)
                        .background(Capsule().fill(PlasticColor.rubber))
                }
                .buttonStyle(.plasticPress)
                .padding(.top, MemorySpace.xs)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    private var emptyPlate: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(localized("upload_empty_title"))
                .font(MemoryFont.font(17, .bold))
                .foregroundStyle(PlasticColor.onPlate)

            Text(localized("upload_empty_hint"))
                .font(MemoryFont.font(11, .semibold))
                .foregroundStyle(PlasticColor.onPlateDim)
                .padding(.top, MemorySpace.xs)

            PhotosPicker(selection: $picked, matching: .images) {
                Text(localized("upload_empty_pick"))
                    .font(MemoryFont.font(13.5, .bold))
                    .foregroundStyle(PlasticColor.onRubber)
                    .padding(.horizontal, MemorySpace.l)
                    .padding(.vertical, MemorySpace.s)
                    .background(Capsule().fill(PlasticColor.rubber))
            }
            .padding(.top, MemorySpace.m)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, MemorySpace.s)
        .padding(.vertical, MemorySpace.l)
    }

    /**
     아래 조작부 — 왼쪽 취소(고무 알약), 오른쪽 올리기(빨간 A 버튼).

     올리기 버튼의 글자는 **↑ 하나**입니다. 지도·달력의 ＋ 와 같은 자리·같은 크기라
     손이 이미 아는 버튼이고, 여기서는 "올린다" 는 뜻만 바꿔 답니다.
     올리는 중에는 왼쪽 알약이 그 사실을 말합니다.
     */
    private var controls: some View {
        HStack(spacing: MemorySpace.m) {
            Button(action: onClose) {
                Text(workingLabel ?? localized("upload_cancel"))
                    .font(MemoryFont.font(15, .bold))
                    .foregroundStyle(PlasticColor.onRubber)
                    .frame(maxWidth: .infinity)
                    .frame(height: PlasticSize.button)
                    .background(Capsule().fill(PlasticColor.rubber))
            }
            .buttonStyle(.plasticPress)
            .padding(PlasticSize.buttonInset)
            .raisedPlastic()

            Button { Task { await store.confirm() } } label: {
                Text("↑")
                    .font(MemoryFont.font(22, .bold))
                    .foregroundStyle(store.state.canUpload ? PlasticColor.onRed : PlasticColor.onButtonOff)
                    .frame(width: PlasticSize.button, height: PlasticSize.button)
                    .background(Circle().fill(store.state.canUpload ? PlasticColor.red : PlasticColor.buttonOff))
            }
            .buttonStyle(.plasticPress)
            .disabled(!store.state.canUpload)
            .accessibilityLabel(localized("upload_confirm_short"))
            .padding(PlasticSize.buttonInset)
            .raisedPlastic()
        }
        .padding(.horizontal, MemorySpace.xs)
        .padding(.vertical, MemorySpace.m)
    }

    private var workingLabel: String? {
        switch store.state.step {
        case .uploading: return localized("upload_uploading")
        case .reading: return localized("upload_reading_short")
        default: return nil
        }
    }
}

/**
 올리기 시트 안 목록의 **최대** 높이.

 시트는 내용만큼만 자랍니다 — 사진 한 장을 올릴 때 화면 반을 먹을 까닭이 없습니다.
 이 값은 그 자람이 멈추는 자리이고, 넘으면 목록이 대신 구릅니다.
 안드로이드 `PlasticSize.UploadList` 와 같은 값입니다.

 기준 디자인 쪽(`UploadView.main`)도 같은 값을 씁니다 — 스위프트의 `private` 은
 **파일 안까지**라 여기서는 열어 둡니다.
 */
let uploadList: CGFloat = 300

/// 지역 고르기. 카트리지 슬롯에 이름을 넣고, 나온 것을 화면 안에서 고릅니다.
struct PlasticRegionPicker: View {
    let store: UploadStore

    var body: some View {
        VStack(spacing: 0) {
            PlasticGrip()

            HStack(spacing: MemorySpace.s) {
                Button { store.cancelPickingRegion() } label: {
                    Text("‹")
                        .font(MemoryFont.font(17, .bold))
                        .foregroundStyle(PlasticColor.onRubber)
                        .frame(width: PlasticSize.monthNav, height: PlasticSize.monthNav)
                        .background(
                            RoundedRectangle(cornerRadius: PlasticRadius.knob, style: .continuous)
                                .fill(PlasticColor.rubber)
                        )
                }
                .buttonStyle(.plasticPress)
                .accessibilityLabel(localized("upload_region_back"))

                Text(localized("upload_region_title"))
                    .font(MemoryFont.font(17, .bold))
                    .foregroundStyle(PlasticColor.ink)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, MemorySpace.xs)
            .padding(.vertical, MemorySpace.s)

            HStack(spacing: MemorySpace.s) {
                RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                    .fill(PlasticColor.ink)
                    .frame(width: 3, height: 14)

                TextField(
                    "",
                    text: Binding(
                        get: { store.state.regionQuery },
                        set: { value in Task { await store.search(value) } }
                    ),
                    prompt: Text(localized("upload_region_placeholder"))
                        .foregroundStyle(PlasticColor.onPlateDim)
                )
                .textFieldStyle(.plain)
                .font(MemoryFont.font(15, .semibold))
                .foregroundStyle(PlasticColor.onPlate)
                .tint(PlasticColor.red)
            }
            .padding(.horizontal, MemorySpace.m)
            .padding(.vertical, 11)
            .sunken(PlasticRadius.chip, face: PlasticColor.plateLo)
            .padding(.bottom, MemorySpace.s)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    ForEach(store.state.regionResults) { region in
                        Button { store.choose(region) } label: {
                            HStack(alignment: .lastTextBaseline, spacing: MemorySpace.s) {
                                Text(region.name)
                                    .font(MemoryFont.font(15, .semibold))
                                    .foregroundStyle(PlasticColor.onPlate)
                                if let parent = region.parentName {
                                    Text(parent)
                                        .font(MemoryFont.font(11, .semibold))
                                        .foregroundStyle(PlasticColor.onPlateDim)
                                }
                                Spacer(minLength: 0)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, MemorySpace.m)
                            .padding(.vertical, 13)
                        }
                        .buttonStyle(.plasticPress)

                        PlasticColor.plateLo.frame(height: 1)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: uploadList)
            .sunken(PlasticRadius.screen)

            Spacer().frame(height: MemorySpace.m)
        }
        .padding(.horizontal, MemorySpace.s)
        .background(PlasticColor.body)
    }
}
