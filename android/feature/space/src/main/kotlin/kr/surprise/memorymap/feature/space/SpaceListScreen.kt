package kr.surprise.memorymap.feature.space

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.component.HillScene
import kr.surprise.memorymap.core.designsystem.component.MemberAvatars
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PrimaryButton
import kr.surprise.memorymap.core.designsystem.component.SpaceCard
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap
import kr.surprise.memorymap.core.model.Space

/**
 * 앱의 메인. **공간이 하나뿐이어도 여기서 시작합니다** —
 * 들어오는 자리가 늘 같아야 두 번째 공간이 생겨도 앱이 달라진 것처럼 느껴지지 않습니다.
 *
 * Composable 은 State 를 받고 Intent 를 올려보내기만 합니다. ViewModel 을 직접 받지 않습니다.
 */
@Composable
fun SpaceListScreen(
    state: SpaceListState,
    myInitial: String,
    onIntent: (SpaceListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MemoryColors.Paper)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Gap.m),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = Gap.xl, end = Gap.xl, top = Gap.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("공간", style = MemoryType.Display, modifier = Modifier.weight(1f))
                    MemberAvatars(initials = listOf(myInitial))
                }
            }

            when (val ui = state.spaces) {
                is SpacesUi.Loading -> item { Hint("불러오는 중이에요") }

                is SpacesUi.Failed -> item {
                    Hint("목록을 불러오지 못했어요. 아래로 당겨 다시 시도해 주세요.")
                }

                is SpacesUi.Ready -> {
                    if (ui.items.isEmpty()) {
                        item { EmptyScene("아직 공간이 없어요. 하나 만들어 볼까요?") }
                    }
                    items(ui.items, key = { it.id.value }) { space ->
                        SpaceCard(
                            name = space.name,
                            meta = space.metaLine(),
                            coverUrl = space.coverPhotoUrl,
                            memberInitials = space.members.map { it.initial },
                            onClick = { onIntent(SpaceListIntent.SpaceTapped(space.id)) },
                            modifier = Modifier.padding(horizontal = Gap.xl),
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(Gap.s)) }

            item {
                ActionRow(
                    label = "새 공간 만들기",
                    tinted = true,
                    onClick = { onIntent(SpaceListIntent.CreateTapped) },
                )
            }
            item {
                ActionRow(
                    label = "초대 코드로 참여",
                    tinted = false,
                    icon = false,
                    onClick = { onIntent(SpaceListIntent.JoinTapped) },
                )
            }
        }
    }

    if (state.sheet != SpaceListSheet.None) {
        SpaceSheet(state = state, onIntent = onIntent)
    }
}

private fun Space.metaLine(): String = buildString {
    append("사진 ").append(photoCount).append("장")
    append(" · 지역 ").append(regionCount).append("곳")
    lastPhotoOn?.let { append(" · ").append(it.monthValue).append("월 ").append(it.dayOfMonth).append("일") }
}

@Composable
private fun EmptyScene(text: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Gap.xl, vertical = Gap.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HillScene(
            Modifier
                .fillMaxWidth(0.62f)
                .clip(MemoryShapes.Card),
        )
        Spacer(Modifier.height(Gap.l))
        Text(text, style = MemoryType.Body, color = MemoryColors.Ink3, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MemoryType.Body,
        color = MemoryColors.Ink3,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Gap.xl, vertical = Gap.xxxl),
    )
}

/**
 * 만들기·참여는 목록 아래 **평범한 줄**입니다. 점선 상자로 강조하면 매번 눈이
 * 거기로 끌리는데, 자주 하는 일이 아닙니다.
 */
@Composable
private fun ActionRow(label: String, tinted: Boolean, onClick: () -> Unit, icon: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Gap.xl, vertical = Gap.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap.l),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(MemoryShapes.Thumb)
                .background(if (tinted) MemoryColors.AccentTint else MemoryColors.Fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (icon) MemoryIcons.Plus else MemoryIcons.Members,
                contentDescription = null,
                tint = if (tinted) MemoryColors.Accent else MemoryColors.Ink2,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(label, style = MemoryType.Body, modifier = Modifier.weight(1f))
        Icon(MemoryIcons.ChevronRight, contentDescription = null, tint = MemoryColors.Ink3, modifier = Modifier.size(18.dp))
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SpaceSheet(state: SpaceListState, onIntent: (SpaceListIntent) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { onIntent(SpaceListIntent.SheetDismissed) },
        sheetState = rememberModalBottomSheetState(),
        containerColor = MemoryColors.Surface,
        shape = MemoryShapes.Sheet,
    ) {
        Column(Modifier.padding(start = Gap.xl, end = Gap.xl, bottom = Gap.xxxl)) {
            when (val sheet = state.sheet) {
                SpaceListSheet.None -> Unit

                SpaceListSheet.Create -> {
                    Text("새 공간 만들기", style = MemoryType.Title)
                    Text(
                        "이름을 정하면 초대 코드가 함께 나와요",
                        style = MemoryType.Label,
                        color = MemoryColors.Ink3,
                        modifier = Modifier.padding(top = Gap.xs, bottom = Gap.xl),
                    )
                    Field(
                        value = state.pendingName,
                        placeholder = "우리 추억 지도",
                        onValueChange = { onIntent(SpaceListIntent.NameTyped(it)) },
                    )
                    Spacer(Modifier.height(Gap.xl))
                    PrimaryButton(
                        text = if (state.working) "만드는 중…" else "만들기",
                        enabled = state.canCreate(),
                        onClick = { onIntent(SpaceListIntent.CreateConfirmed) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SpaceListSheet.Join -> {
                    Text("초대 코드로 참여", style = MemoryType.Title)
                    Text(
                        "받은 여섯 글자를 넣어 주세요",
                        style = MemoryType.Label,
                        color = MemoryColors.Ink3,
                        modifier = Modifier.padding(top = Gap.xs, bottom = Gap.xl),
                    )
                    Field(
                        value = state.pendingCode,
                        placeholder = "K7QF2M",
                        onValueChange = { onIntent(SpaceListIntent.CodeTyped(it)) },
                    )
                    Spacer(Modifier.height(Gap.xl))
                    PrimaryButton(
                        text = if (state.working) "확인 중…" else "참여하기",
                        enabled = state.canJoin(),
                        onClick = { onIntent(SpaceListIntent.JoinConfirmed) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is SpaceListSheet.Invited -> {
                    Text("${sheet.spaceName} 만들었어요", style = MemoryType.Title)
                    Text(
                        "이 코드를 보내면 같이 채울 수 있어요",
                        style = MemoryType.Label,
                        color = MemoryColors.Ink3,
                        modifier = Modifier.padding(top = Gap.xs, bottom = Gap.xl),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(MemoryShapes.Button)
                            .background(MemoryColors.AccentTint)
                            .padding(vertical = Gap.xl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(sheet.code, style = MemoryType.Display, color = MemoryColors.Accent)
                    }
                    Spacer(Modifier.height(Gap.xl))
                    PrimaryButton(
                        text = "코드 보내기",
                        onClick = { onIntent(SpaceListIntent.InviteCopied(sheet.code)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Field(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MemoryShapes.Button)
            .background(MemoryColors.Fill)
            .padding(horizontal = Gap.l, vertical = Gap.l),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = MemoryType.Body, color = MemoryColors.Ink3)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MemoryType.Body.copy(color = MemoryColors.Ink),
            cursorBrush = SolidColor(MemoryColors.Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
