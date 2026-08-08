import CoreModel
import DesignSystem
import Foundation
import SwiftUI

/**
 **⋯ 관리 메뉴 — 패미컴 컨트롤러 스타일.**

 시트 넷과 같은 규칙입니다 — **몸통이 통째로 올라오고** 내용은 그 안에 끼운
 검정 화면에 놓입니다. 화면(검정 판)만 올라오면 기기에서 화면이 떨어져 나온
 것처럼 보입니다. 안드로이드 `SpaceMenuPlastic.kt` 와 같습니다.

 목업에는 '짜국 나가기' 도 있었지만 **넣지 않았습니다.** 지금 어느 쪽에도 없는
 기능이라, 눌러도 아무 일 없는 줄을 두는 것보다 없는 편이 낫습니다.
 */
struct PlasticSpaceMenu: View {
    let store: SpaceMenuStore
    let onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            grip

            if store.state.renaming {
                renameBody
            } else {
                menuBody
            }
        }
        .padding(.horizontal, MemorySpace.s)
        .padding(.bottom, MemorySpace.xxl)
        .frame(maxWidth: .infinity)
        .background(PlasticColor.body)
        .clipShape(
            UnevenRoundedRectangle(
                topLeadingRadius: PlasticRadius.device,
                topTrailingRadius: PlasticRadius.device,
                style: .continuous
            )
        )
    }

    /// 몸통에 새긴 홈. 목록 화면 위쪽의 줄무늬와 같은 것입니다.
    private var grip: some View {
        Capsule()
            .fill(PlasticColor.trim)
            .frame(width: PlasticSize.grip, height: PlasticSize.stripe)
            .padding(.vertical, MemorySpace.s)
    }

    @ViewBuilder
    private var menuBody: some View {
        // 제목줄은 몸통 위입니다 — 짜국 이름과 닫기 버튼.
        HStack(spacing: MemorySpace.s) {
            Text(store.state.space?.name ?? "")
                .font(MemoryFont.font(17, .bold))
                .foregroundStyle(PlasticColor.ink)
                .lineLimit(1)
            Spacer(minLength: 0)
            Button(action: onClose) {
                Text("×")
                    .font(MemoryFont.font(17, .bold))
                    .foregroundStyle(PlasticColor.onRubber)
                    .frame(width: PlasticSize.sheetClose, height: PlasticSize.sheetClose)
                    .background(Circle().fill(PlasticColor.rubber))
            }
            .buttonStyle(.plasticPress)
            .accessibilityLabel(localized("menu_close"))
        }
        .padding(.horizontal, MemorySpace.xs)
        .padding(.bottom, MemorySpace.s)

        VStack(alignment: .leading, spacing: 0) {
            // 혼자 쓰는 짜국에는 멤버도 초대 코드도 없습니다 — 있는 척하지 않습니다.
            if let space = store.state.space, space.kind == .shared {
                plateLabel(localized("menu_members"))
                ForEach(space.members) { member in
                    HStack(spacing: MemorySpace.s) {
                        Text(member.initial)
                            .font(MemoryFont.font(11, .bold))
                            .foregroundStyle(PlasticColor.plate)
                            .frame(width: PlasticSize.chip, height: PlasticSize.chip)
                            .background(
                                RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                                    .fill(PlasticColor.body)
                            )
                        Text(member.displayName)
                            .font(MemoryFont.font(13.5, .semibold))
                            .foregroundStyle(PlasticColor.onPlate)
                            .lineLimit(1)
                        Spacer(minLength: 0)
                        if member.role == .owner {
                            Text(localized("menu_owner"))
                                .font(MemoryFont.font(11, .bold))
                                .foregroundStyle(PlasticColor.onPlateDim)
                        }
                    }
                    .padding(.vertical, 7)
                }

                Spacer().frame(height: MemorySpace.s)
                plateLabel(localized("menu_invite_code"))
                HStack(spacing: MemorySpace.s) {
                    Text(store.state.code ?? localized("menu_invite_code_making"))
                        .font(MemoryFont.font(20, .bold))
                        .tracking(store.state.code != nil ? 4 : 0)
                        .foregroundStyle(
                            store.state.code != nil ? PlasticColor.onPlate : PlasticColor.onPlateDim
                        )
                    Spacer(minLength: 0)
                    if let code = store.state.code {
                        Button {
                            #if os(iOS)
                            UIPasteboard.general.string = code
                            #endif
                        } label: {
                            Text(localized("menu_copy"))
                                .font(MemoryFont.font(11, .bold))
                                .foregroundStyle(PlasticColor.onRubber)
                                .padding(.horizontal, MemorySpace.m)
                                .padding(.vertical, 6)
                                .background(Capsule().fill(PlasticColor.rubber))
                        }
                        .buttonStyle(.plasticPress)
                    }
                }

                // 줄 사이의 홈. 판을 파낸 자국이라 두 덩어리가 갈립니다.
                RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                    .fill(PlasticColor.plateLo)
                    .frame(height: 2)
                    .padding(.vertical, MemorySpace.s)
            }

            Button { store.startRenaming() } label: {
                HStack {
                    Text(localized("menu_rename"))
                        .font(MemoryFont.font(13.5, .semibold))
                        .foregroundStyle(PlasticColor.onPlate)
                    Spacer(minLength: 0)
                    Text("›")
                        .font(MemoryFont.font(15, .bold))
                        .foregroundStyle(PlasticColor.onPlateDim)
                }
                .contentShape(Rectangle())
                .padding(.vertical, MemorySpace.s)
            }
            .buttonStyle(.plasticPress)
        }
        .padding(MemorySpace.s)
        .frame(maxWidth: .infinity, alignment: .leading)
        .sunken(PlasticRadius.screen)
    }

    @ViewBuilder
    private var renameBody: some View {
        Text(localized("menu_rename"))
            .font(MemoryFont.font(17, .bold))
            .foregroundStyle(PlasticColor.ink)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, MemorySpace.xs)
            .padding(.bottom, MemorySpace.s)

        // 값을 꽂아 넣는 자리 — 지도 검색칸·올리기의 어디·언제와 같은 슬롯입니다.
        HStack(spacing: MemorySpace.s) {
            RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                .fill(PlasticColor.ink)
                .frame(width: 3, height: 14)

            TextField(
                "",
                text: Binding(
                    get: { store.state.pendingName },
                    set: { store.typeName($0) }
                ),
                prompt: Text(localized("menu_rename_placeholder")).foregroundStyle(PlasticColor.onPlateDim)
            )
            .textFieldStyle(.plain)
            .memoryBody()
            .foregroundStyle(PlasticColor.onPlate)
            .tint(PlasticColor.red)
        }
        .padding(.horizontal, MemorySpace.m)
        .padding(.vertical, 12)
        .sunken(PlasticRadius.chip, face: PlasticColor.plateLo)

        Spacer().frame(height: MemorySpace.s)

        PrimaryButton(
            localized(store.state.working ? "menu_rename_working" : "menu_rename_confirm"),
            enabled: !store.state.pendingName.isEmpty && !store.state.working
        ) {
            // 결과(Bool)는 여기서 쓰지 않습니다 — 닫는 일은 부르는 쪽이 합니다.
            Task { _ = await store.confirmRename() }
        }
    }

    /// 검정 판 위의 구역 이름표.
    private func plateLabel(_ text: String) -> some View {
        Text(text)
            .memoryMicro()
            .foregroundStyle(PlasticColor.onPlateDim)
            .tracking(0.7)
            .padding(.bottom, MemorySpace.xs)
    }
}
