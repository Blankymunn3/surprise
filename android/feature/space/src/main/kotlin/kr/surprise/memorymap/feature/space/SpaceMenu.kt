package kr.surprise.memorymap.feature.space

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PrimaryButton
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.Pretendard
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap
import kr.surprise.memorymap.core.model.MemberRole
import kr.surprise.memorymap.core.model.SpaceKind
import androidx.compose.ui.text.font.FontWeight

/**
 * ⋯ 하나에 **멤버 · 초대 코드 · 이름**을 다 넣습니다.
 *
 * 화면을 셋으로 나누지 않는 이유: 셋 다 어쩌다 한 번 하는 일입니다. 각각 화면을
 * 만들면 그 화면으로 가는 길을 또 만들어야 하고, 정작 자주 쓰는 지도·달력이 밀립니다.
 *
 * 아래에서 올라오는 판입니다 — 짜국을 벗어나는 것이 아니라 그 위에 잠깐 얹는 것이라
 * 뒤가 보여야 어디에 있는지 알 수 있습니다.
 */
@Composable
fun SpaceMenu(
    state: SpaceMenuState,
    onIntent: (SpaceMenuIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MemoryColors.Scrim)
            .clickable { onIntent(SpaceMenuIntent.Dismissed) }
    ) {
        // 몸통이 통째로 올라오고 내용은 끼운 검정 화면에 놓입니다.
        PlasticSpaceMenu(state, onIntent, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MenuBody(state: SpaceMenuState, onIntent: (SpaceMenuIntent) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = state.space?.name.orEmpty(),
            style = MemoryType.Headline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(30.dp)
                .border(MemoryStroke.Border, MemoryColors.Line)
                .clickable { onIntent(SpaceMenuIntent.Dismissed) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(MemoryIcons.Close, contentDescription = stringResource(R.string.menu_close), tint = MemoryColors.Ink, modifier = Modifier.size(13.dp))
        }
    }

    // 혼자 쓰는 짜국에는 멤버도 초대 코드도 없습니다 — 있는 척하지 않습니다.
    if (state.space?.kind == SpaceKind.Shared) {
        Spacer(Modifier.height(Gap.m))
        SectionLabel(stringResource(R.string.menu_members))
        state.space.members.forEach { member ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = Gap.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(28.dp).background(MemoryColors.Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(member.initial, style = MemoryType.Micro, color = MemoryColors.OnAccent)
                }
                Text(
                    text = member.displayName,
                    style = MemoryType.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (member.role == MemberRole.Owner) {
                    Text(
                        stringResource(R.string.menu_owner),
                        style = MemoryType.Micro,
                        color = MemoryColors.Ink,
                        modifier = Modifier
                            .border(MemoryStroke.Border, MemoryColors.Line)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(MemoryStroke.Border).background(MemoryColors.Fill))
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel(stringResource(R.string.menu_invite_code))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = state.code ?: stringResource(R.string.menu_invite_code_making),
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = if (state.code != null) 4.sp else 0.sp,
                color = if (state.code != null) MemoryColors.Ink else MemoryColors.Ink3,
                modifier = Modifier.weight(1f),
            )
            state.code?.let { code ->
                Text(
                    stringResource(R.string.menu_copy),
                    style = MemoryType.Micro,
                    color = MemoryColors.Ink,
                    modifier = Modifier
                        .background(MemoryColors.Surface)
                        .border(MemoryStroke.Border, MemoryColors.Line)
                        .clickable { onIntent(SpaceMenuIntent.CodeCopied(code)) }
                        .padding(horizontal = Gap.m, vertical = 7.dp),
                )
            }
        }
    }

    Spacer(Modifier.height(Gap.m))
    Box(Modifier.fillMaxWidth().height(MemoryStroke.Border).background(MemoryColors.Fill))
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onIntent(SpaceMenuIntent.RenameTapped) }
            .padding(top = Gap.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.menu_rename), style = MemoryType.Body, color = MemoryColors.Ink2, modifier = Modifier.weight(1f))
        Icon(MemoryIcons.ChevronRight, contentDescription = null, tint = MemoryColors.Ink3, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun RenameBody(state: SpaceMenuState, onIntent: (SpaceMenuIntent) -> Unit) {
    Text(stringResource(R.string.menu_rename), style = MemoryType.Headline)
    Spacer(Modifier.height(Gap.m))

    Box(
        Modifier
            .fillMaxWidth()
            .background(MemoryColors.Surface)
            .border(MemoryStroke.Border, MemoryColors.Line)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        if (state.pendingName.isEmpty()) {
            Text(stringResource(R.string.menu_rename_placeholder), style = MemoryType.Body, color = MemoryColors.Ink3)
        }
        BasicTextField(
            value = state.pendingName,
            onValueChange = { onIntent(SpaceMenuIntent.NameTyped(it)) },
            singleLine = true,
            textStyle = MemoryType.Body.copy(color = MemoryColors.Ink),
            cursorBrush = SolidColor(MemoryColors.Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(Gap.m))
    PrimaryButton(
        text = stringResource(
            if (state.working) R.string.menu_rename_working else R.string.menu_rename_confirm
        ),
        enabled = state.pendingName.isNotBlank() && !state.working,
        onClick = { onIntent(SpaceMenuIntent.RenameConfirmed) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MemoryType.Micro,
        color = MemoryColors.Ink2,
        letterSpacing = 0.7.sp,
        modifier = Modifier.padding(bottom = Gap.xs),
    )
}
