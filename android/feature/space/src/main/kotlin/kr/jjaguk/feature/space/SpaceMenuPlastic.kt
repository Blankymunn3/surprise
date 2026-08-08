package kr.jjaguk.feature.space

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.jjaguk.core.designsystem.component.PrimaryButton
import kr.jjaguk.core.designsystem.theme.MemoryType
import kr.jjaguk.core.designsystem.theme.PlasticColors
import kr.jjaguk.core.designsystem.theme.PlasticShapes
import kr.jjaguk.core.designsystem.theme.PlasticSize
import kr.jjaguk.core.designsystem.theme.Pretendard
import kr.jjaguk.core.designsystem.theme.Space as Gap
import kr.jjaguk.core.designsystem.theme.pressable
import kr.jjaguk.core.designsystem.theme.sunken
import kr.jjaguk.core.model.MemberRole
import kr.jjaguk.core.model.SpaceKind

/**
 * **⋯ 관리 메뉴 — 패미컴 컨트롤러 스타일.**
 *
 * 시트 넷과 같은 규칙입니다 — **몸통이 통째로 올라오고** 내용은 그 안에 끼운
 * 검정 화면에 놓입니다. 화면(검정 판)만 올라오면 기기에서 화면이 떨어져 나온
 * 것처럼 보입니다.
 *
 * 목업에는 '짜국 나가기' 도 있었지만 **넣지 않았습니다.** 지금 어느 쪽에도 없는
 * 기능이라, 눌러도 아무 일 없는 줄을 두는 것보다 없는 편이 낫습니다
 * (지도의 '내 위치' 를 iOS 에서 뺀 것과 같은 판단입니다).
 */
@Composable
internal fun PlasticSpaceMenu(
    state: SpaceMenuState,
    onIntent: (SpaceMenuIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(PlasticShapes.Device)
            .background(PlasticColors.Body)
            // 판을 눌러도 닫히지 않게 여기서 터치를 먹습니다.
            .clickable(enabled = false) { }
    ) {
        Column(Modifier.padding(horizontal = Gap.s).padding(bottom = Gap.xxl)) {
            Grip()

            if (state.renaming) {
                PlasticRenameBody(state, onIntent)
            } else {
                PlasticMenuBody(state, onIntent)
            }
        }
    }
}

/** 몸통에 새긴 홈. 목록 화면 위쪽의 줄무늬와 같은 것입니다. */
@Composable
private fun Grip() {
    Box(Modifier.fillMaxWidth().padding(vertical = Gap.s), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(PlasticSize.Grip)
                .height(PlasticSize.Stripe)
                .clip(PlasticShapes.Pill)
                .background(PlasticColors.Trim)
        )
    }
}

@Composable
private fun PlasticMenuBody(state: SpaceMenuState, onIntent: (SpaceMenuIntent) -> Unit) {
    // 제목줄은 몸통 위입니다 — 짜국 이름과 닫기 버튼.
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gap.xs).padding(bottom = Gap.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.space?.name.orEmpty(),
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = PlasticColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(PlasticSize.SheetClose)
                .clip(PlasticShapes.Pill)
                .background(PlasticColors.Rubber)
                .pressable { onIntent(SpaceMenuIntent.Dismissed) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "×",
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = PlasticColors.OnRubber,
            )
        }
    }

    Column(Modifier.fillMaxWidth().sunken(PlasticShapes.Screen).padding(Gap.s)) {
        // 혼자 쓰는 짜국에는 멤버도 초대 코드도 없습니다 — 있는 척하지 않습니다.
        if (state.space?.kind == SpaceKind.Shared) {
            PlateLabel(stringResource(R.string.menu_members))
            state.space.members.forEach { member ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Gap.s),
                ) {
                    Box(
                        Modifier
                            .size(PlasticSize.Chip)
                            .clip(PlasticShapes.Chip)
                            .background(PlasticColors.Body),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = member.initial,
                            fontFamily = Pretendard,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = PlasticColors.Plate,
                        )
                    }
                    Text(
                        text = member.displayName,
                        fontFamily = Pretendard,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = PlasticColors.OnPlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (member.role == MemberRole.Owner) {
                        Text(
                            text = stringResource(R.string.menu_owner),
                            fontFamily = Pretendard,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = PlasticColors.OnPlateDim,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Gap.s))
            PlateLabel(stringResource(R.string.menu_invite_code))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Gap.s),
            ) {
                Text(
                    text = state.code ?: stringResource(R.string.menu_invite_code_making),
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = if (state.code != null) 4.sp else 0.sp,
                    color = if (state.code != null) PlasticColors.OnPlate else PlasticColors.OnPlateDim,
                    modifier = Modifier.weight(1f),
                )
                state.code?.let { code ->
                    Text(
                        text = stringResource(R.string.menu_copy),
                        fontFamily = Pretendard,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = PlasticColors.OnRubber,
                        modifier = Modifier
                            .clip(PlasticShapes.Pill)
                            .background(PlasticColors.Rubber)
                            .pressable { onIntent(SpaceMenuIntent.CodeCopied(code)) }
                            .padding(horizontal = Gap.m, vertical = 6.dp),
                    )
                }
            }

            // 줄 사이의 홈. 판을 파낸 자국이라 두 덩어리가 갈립니다.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Gap.s)
                    .height(2.dp)
                    .clip(PlasticShapes.Chip)
                    .background(PlasticColors.PlateLo)
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onIntent(SpaceMenuIntent.RenameTapped) }
                .padding(vertical = Gap.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.menu_rename),
                fontFamily = Pretendard,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = PlasticColors.OnPlate,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "›",
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = PlasticColors.OnPlateDim,
            )
        }
    }
}

@Composable
private fun PlasticRenameBody(state: SpaceMenuState, onIntent: (SpaceMenuIntent) -> Unit) {
    Text(
        text = stringResource(R.string.menu_rename),
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        color = PlasticColors.Ink,
        modifier = Modifier.padding(horizontal = Gap.xs).padding(bottom = Gap.s),
    )

    // 값을 꽂아 넣는 자리 — 지도 검색칸·올리기의 어디·언제와 같은 슬롯입니다.
    Row(
        Modifier
            .fillMaxWidth()
            .sunken(PlasticShapes.Chip, face = PlasticColors.PlateLo)
            .padding(horizontal = Gap.m, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap.s),
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 14.dp)
                .clip(PlasticShapes.Chip)
                .background(PlasticColors.Ink)
        )
        Box(Modifier.weight(1f)) {
            if (state.pendingName.isEmpty()) {
                Text(stringResource(R.string.menu_rename_placeholder), style = MemoryType.Body, color = PlasticColors.OnPlateDim)
            }
            BasicTextField(
                value = state.pendingName,
                onValueChange = { onIntent(SpaceMenuIntent.NameTyped(it)) },
                singleLine = true,
                textStyle = MemoryType.Body.copy(color = PlasticColors.OnPlate),
                cursorBrush = SolidColor(PlasticColors.Red),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    Spacer(Modifier.height(Gap.s))
    PrimaryButton(
        text = stringResource(if (state.working) R.string.menu_rename_working else R.string.menu_rename_confirm),
        enabled = state.pendingName.isNotBlank() && !state.working,
        onClick = { onIntent(SpaceMenuIntent.RenameConfirmed) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 검정 판 위의 구역 이름표. */
@Composable
private fun PlateLabel(text: String) {
    Text(
        text = text,
        style = MemoryType.Micro,
        color = PlasticColors.OnPlateDim,
        letterSpacing = 0.7.sp,
        modifier = Modifier.padding(bottom = Gap.xs),
    )
}
