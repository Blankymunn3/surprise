package kr.surprise.memorymap.feature.space

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.component.FRAMES_RATIO
import kr.surprise.memorymap.core.designsystem.component.PhotoFramesScene
import kr.surprise.memorymap.core.designsystem.component.PrimaryButton
import kr.surprise.memorymap.core.designsystem.component.SoftButton
import kr.surprise.memorymap.core.designsystem.component.SpaceCard
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceKind

/**
 * 앱의 메인. **공간이 하나뿐이어도 여기서 시작합니다** —
 * 들어오는 자리가 늘 같아야 두 번째 공간이 생겨도 앱이 달라진 것처럼 느껴지지 않습니다.
 *
 * Composable 은 State 를 받고 Intent 를 올려보내기만 합니다. ViewModel 을 직접 받지 않습니다.
 */
@Composable
fun SpaceListScreen(
    state: SpaceListState,
    onIntent: (SpaceListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MemoryColors.Paper)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Header()
        Divider()

        // 목록만 늘어납니다. 만들기·참여는 아래에 **붙박이로** 둡니다 —
        // 짜국이 늘어나도 그 둘을 찾으러 스크롤하지 않게요.
        Box(Modifier.weight(1f)) {
            when (val ui = state.spaces) {
                is SpacesUi.Loading -> Hint("불러오는 중이에요")

                is SpacesUi.Failed -> Hint("목록을 불러오지 못했어요. 아래로 당겨 다시 시도해 주세요.")

                is SpacesUi.Ready ->
                    if (ui.items.isEmpty()) {
                        EmptyScene()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = Gap.xl, end = Gap.xl, top = Gap.l, bottom = Gap.s,
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(ui.items, key = { it.id.value }) { space ->
                                SpaceCard(
                                    name = space.name,
                                    meta = space.metaLine(),
                                    coverUrl = space.coverPhotoUrl,
                                    memberInitials = space.members.map { it.initial },
                                    onClick = { onIntent(SpaceListIntent.SpaceTapped(space.id)) },
                                    onlyOnThisPhone = space.kind == SpaceKind.Personal,
                                )
                            }
                        }
                    }
            }
        }

        Divider(inset = false)
        Column(
            Modifier.padding(start = Gap.xl, end = Gap.xl, top = Gap.m, bottom = Gap.xxl),
            verticalArrangement = Arrangement.spacedBy(Gap.s),
        ) {
            PrimaryButton(
                text = "새 짜국 만들기",
                onClick = { onIntent(SpaceListIntent.CreateTapped) },
                modifier = Modifier.fillMaxWidth(),
            )
            SoftButton(
                text = "초대 코드로 참여",
                onClick = { onIntent(SpaceListIntent.JoinTapped) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (state.sheet != SpaceListSheet.None) {
        SpaceSheet(state = state, onIntent = onIntent)
    }
}

/** 사진이 없으면 수를 세지 않고 그 사실만 말합니다. "사진 0 · 지역 0" 은 셈이 아니라 잡음입니다. */
private fun Space.metaLine(): String {
    if (photoCount == 0) return "아직 사진이 없어요"
    return buildString {
        append("사진 ").append(photoCount)
        append(" · 지역 ").append(regionCount)
        lastPhotoOn?.let { append(" · ").append(it.monthValue).append("월 ").append(it.dayOfMonth).append("일") }
    }
}

/** 레드 사각 하나가 앱 이름 앞에 섭니다. 아이콘이 아니라 표식이라 뜻을 붙이지 않습니다. */
@Composable
private fun Header() {
    Row(
        Modifier.fillMaxWidth().padding(start = Gap.xl, end = Gap.xl, top = Gap.s, bottom = Gap.m),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            Modifier
                .padding(bottom = 5.dp)
                .size(13.dp)
                .background(MemoryColors.Accent)
        )
        Spacer(Modifier.width(9.dp))
        Text("짜국", style = MemoryType.Display)
        Spacer(Modifier.weight(1f))
        Text(
            text = "어디와 언제로 보는 사진첩",
            style = MemoryType.Micro,
            color = MemoryColors.Ink2,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

/** 구획선은 2px 입니다. 테두리(1px)보다 굵어야 '나누는 선' 으로 읽힙니다. */
@Composable
private fun Divider(inset: Boolean = true) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (inset) Gap.xl else 0.dp)
            .height(MemoryStroke.Divider)
            .background(MemoryColors.Line2)
    )
}

/**
 * 첫 실행. **가운데 정렬하지 않습니다** — 글이 왼끝에 맞아야 다음에 올 목록과
 * 같은 자리에서 시작하고, 짜국이 생겼을 때 화면이 통째로 움직인 것처럼 안 보입니다.
 */
@Composable
private fun EmptyScene() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        PhotoFramesScene(Modifier.fillMaxWidth(0.42f).aspectRatio(FRAMES_RATIO))
        Spacer(Modifier.height(14.dp))
        Text("아직 짜국이 없어요", style = MemoryType.Title)
        Spacer(Modifier.height(14.dp))
        Text(
            text = buildAnnotatedString {
                append("짜국은 사진을 ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MemoryColors.Ink)) { append("지도") }
                append("와 ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MemoryColors.Ink)) { append("달력") }
                append(", 두 가지로 보는 사진첩이에요. 혼자 써도 되고, 가까운 사람들과 같이 채워도 돼요.")
            },
            style = MemoryType.Label,
            color = MemoryColors.Ink2,
        )
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
                    Text("새 짜국 만들기", style = MemoryType.Title)
                    Text(
                        "사진을 어디에 둘지 먼저 고릅니다",
                        style = MemoryType.Label,
                        color = MemoryColors.Ink3,
                        modifier = Modifier.padding(top = Gap.xs, bottom = Gap.l),
                    )
                    KindPicker(
                        selected = state.pendingKind,
                        onSelect = { onIntent(SpaceListIntent.KindSelected(it)) },
                    )
                    Spacer(Modifier.height(Gap.m))
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

                is SpaceListSheet.SignIn -> {
                    Text("구글로 로그인", style = MemoryType.Title)
                    Text(
                        "같이 보려면 누가 이 짜국의 멤버인지 서버가 알아야 해요. " +
                            "남이 사진을 못 보게 막는 것도 이걸로 합니다.",
                        style = MemoryType.Label,
                        color = MemoryColors.Ink2,
                        modifier = Modifier.padding(top = Gap.xs, bottom = Gap.l),
                    )
                    GoogleButton(
                        text = if (state.working) "로그인 중…" else "구글로 계속하기",
                        enabled = !state.working,
                        onClick = { onIntent(SpaceListIntent.SignInTapped) },
                    )
                    // 만들기에서 왔을 때만 빠져나갈 길을 둡니다. 참여로 왔으면 혼자로 갈
                    // 곳이 없습니다 — 남의 짜국에 혼자 들어갈 수는 없으니까요.
                    if (sheet.next == SpaceListSheet.Next.Create) {
                        Text(
                            "그냥 혼자 쓸래요",
                            style = MemoryType.Label,
                            color = MemoryColors.Ink2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onIntent(SpaceListIntent.SignInGaveUp) }
                                .padding(top = Gap.m, bottom = Gap.xs),
                        )
                    }
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
                            .background(MemoryColors.Fill)
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

/**
 * 구글 버튼만 **감빛이 아닙니다.** 우리 것이 아니라 남의 서비스로 넘어가는 문이라
 * 앱의 강조색을 입히면 우리가 하는 일처럼 보입니다. 흰 바탕에 가는 테두리 —
 * 구글이 권하는 모양이기도 합니다 (`docs/app/design.html` 의 '로그인').
 */
@Composable
private fun GoogleButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MemoryShapes.Button)
            .background(MemoryColors.Surface)
            .border(1.5.dp, MemoryColors.Line, MemoryShapes.Button)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = Gap.l),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoogleMark(Modifier.size(18.dp))
        Spacer(Modifier.width(Gap.s))
        Text(text, style = MemoryType.Headline, color = MemoryColors.Ink)
    }
}

/** 구글 4색 G. 로고라 색을 우리 팔레트로 바꾸지 않습니다. */
@Composable
private fun GoogleMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        fun p(path: String, color: Color) {
            drawPath(
                path = PathParser().parsePathString(path).toPath().also {
                    it.transform(Matrix().apply { scale(s / 48f, s / 48f) })
                },
                color = color,
            )
        }
        p(
            "M45.12 24.5c0-1.56-.14-3.06-.4-4.5H24v8.51h11.84c-.51 2.75-2.06 5.08-4.39 " +
                "6.64v5.52h7.11c4.16-3.83 6.56-9.47 6.56-16.17z",
            Color(0xFF4285F4),
        )
        p(
            "M24 46c5.94 0 10.92-1.97 14.56-5.33l-7.11-5.52c-1.97 1.32-4.49 2.1-7.45 " +
                "2.1-5.73 0-10.58-3.87-12.31-9.07H4.34v5.7C7.96 41.07 15.4 46 24 46z",
            Color(0xFF34A853),
        )
        p(
            "M11.69 28.18C11.25 26.86 11 25.45 11 24s.25-2.86.69-4.18v-5.7H4.34C2.85 17.09 " +
                "2 20.45 2 24s.85 6.91 2.34 9.88l7.35-5.7z",
            Color(0xFFFBBC05),
        )
        p(
            "M24 10.75c3.23 0 6.13 1.11 8.41 3.29l6.31-6.31C34.91 4.18 29.93 2 24 2 15.4 2 " +
                "7.96 6.93 4.34 14.12l7.35 5.7c1.73-5.2 6.58-9.07 12.31-9.07z",
            Color(0xFFEA4335),
        )
    }
}

/**
 * 혼자 / 같이 고르기 (`docs/app/design.html` 의 '짜국 만들기').
 *
 * **세로로 쌓는 이유**: 줄마다 설명이 한 줄씩 붙습니다. `지도|달력` 같은 알약에는
 * 설명이 안 들어가고, 설명 없이 두면 사진이 폰 밖으로 나가는지 모르고 고르게 됩니다.
 */
@Composable
private fun KindPicker(selected: SpaceKind, onSelect: (SpaceKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Gap.s)) {
        KindOption(
            title = "혼자 쓸래요",
            detail = "사진이 이 폰에만 있어요 · 로그인 없이 바로",
            checked = selected == SpaceKind.Personal,
            onClick = { onSelect(SpaceKind.Personal) },
        )
        KindOption(
            title = "같이 볼래요",
            detail = "초대한 사람들과 같이 봐요 · 로그인이 필요해요",
            checked = selected == SpaceKind.Shared,
            onClick = { onSelect(SpaceKind.Shared) },
        )
    }
}

@Composable
private fun KindOption(title: String, detail: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MemoryShapes.Button)
            .background(if (checked) MemoryColors.Fill else MemoryColors.Fill)
            // 테두리를 **안쪽에** 그립니다. 바깥에 두면 고를 때마다 칸이 커졌다 작아져
            // 두 줄이 흔들립니다.
            .then(
                if (checked) Modifier.border(1.5.dp, MemoryColors.Accent, MemoryShapes.Button)
                else Modifier
            )
            .selectable(selected = checked, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Gap.l, vertical = Gap.m),
        horizontalArrangement = Arrangement.spacedBy(Gap.m),
    ) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(16.dp)
                .clip(MemoryShapes.Pill)
                .background(MemoryColors.Surface)
                .border(1.5.dp, if (checked) MemoryColors.Accent else MemoryColors.Line, MemoryShapes.Pill),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Box(Modifier.size(7.dp).clip(MemoryShapes.Pill).background(MemoryColors.Accent))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MemoryType.Headline)
            Text(
                detail,
                style = MemoryType.Label,
                color = MemoryColors.Ink2,
                modifier = Modifier.padding(top = 1.dp),
            )
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
