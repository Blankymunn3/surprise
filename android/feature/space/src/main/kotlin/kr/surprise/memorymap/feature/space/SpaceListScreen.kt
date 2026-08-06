package kr.surprise.memorymap.feature.space

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.surprise.memorymap.core.designsystem.component.FRAMES_RATIO
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PhotoFramesScene
import kr.surprise.memorymap.core.designsystem.component.PrimaryButton
import kr.surprise.memorymap.core.designsystem.component.SoftButton
import kr.surprise.memorymap.core.designsystem.component.SpaceCard
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.PLASTIC_TRIAL
import kr.surprise.memorymap.core.designsystem.theme.PlasticColors
import kr.surprise.memorymap.core.designsystem.theme.PlasticShapes
import kr.surprise.memorymap.core.designsystem.theme.PlasticSize
import kr.surprise.memorymap.core.designsystem.theme.Pretendard
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap
import kr.surprise.memorymap.core.designsystem.theme.raisedPlastic
import kr.surprise.memorymap.core.designsystem.theme.sunken
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceKind

/**
 * 앱의 메인. **공간이 하나뿐이어도 여기서 시작합니다** —
 * 들어오는 자리가 늘 같아야 두 번째 공간이 생겨도 앱이 달라진 것처럼 느껴지지 않습니다.
 *
 * 만들기·참여·로그인·초대 코드는 **아래에서 올라오는 시트**입니다. 목록을 잠깐 가리고
 * 끝내는 일이라, 화면을 통째로 갈아 끼우면 어디에서 하던 일인지 놓칩니다.
 * 시트 안의 모양은 새 디자인 그대로입니다 — 담는 그릇만 시트입니다.
 *
 * Composable 은 State 를 받고 Intent 를 올려보내기만 합니다. ViewModel 을 직접 받지 않습니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceListScreen(
    state: SpaceListState,
    onIntent: (SpaceListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MemoryColors.Paper)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        if (PLASTIC_TRIAL) PlasticListBody(state, onIntent) else ListBody(state, onIntent)
    }

    if (state.sheet != SpaceListSheet.None) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(SpaceListIntent.SheetDismissed) },
            sheetState = rememberModalBottomSheetState(),
            containerColor = if (PLASTIC_TRIAL) PlasticColors.Body else MemoryColors.Surface,
            // 시트 위쪽에도 2px 잉크 선을 긋습니다 — 지역 시트와 같은 규칙입니다.
            dragHandle = { SheetGrip() },
            shape = if (PLASTIC_TRIAL) PlasticShapes.Device else MemoryShapes.Sheet,
        ) {
            when (val sheet = state.sheet) {
                SpaceListSheet.None -> Unit
                SpaceListSheet.Create -> CreateSheet(state, onIntent)
                SpaceListSheet.Join -> JoinSheet(state, onIntent)
                is SpaceListSheet.SignIn -> SignInSheet(sheet, state.working, onIntent)
                is SpaceListSheet.Invited -> InvitedSheet(sheet, onIntent)
            }
        }
    }
}

/**
 * 시트 손잡이. 막대 하나가 아니라 **위쪽 2px 잉크 선**입니다 —
 * 이 디자인에는 둥근 막대가 들어갈 자리가 없고, 지역 시트도 같은 선으로 시작합니다.
 * 끌어 내려 닫는 것은 그대로 됩니다.
 */
@Composable
private fun SheetGrip() {
    // 패미컴 스타일에서는 **몸통이 통째로 올라옵니다.** 화면(검정 판)만 올라오면
    // 기기에서 화면이 떨어져 나온 것처럼 보입니다. 그래서 손잡이도 잉크 선이 아니라
    // 몸통에 새긴 회색 홈 — 목록 화면 위쪽의 줄무늬와 같은 것입니다.
    if (PLASTIC_TRIAL) {
        Box(Modifier.fillMaxWidth().padding(vertical = Gap.s), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(PlasticSize.Grip)
                    .height(PlasticSize.Stripe)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.Trim)
            )
        }
        return
    }

    Box(Modifier.fillMaxWidth().height(MemoryStroke.Divider).background(MemoryColors.Ink))
}

// ---------------------------------------------------------------------------
// 목록

@Composable
private fun ListBody(state: SpaceListState, onIntent: (SpaceListIntent) -> Unit) {
    Column(Modifier.fillMaxSize()) {
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

// ---------------------------------------------------------------------------
// 공통 부품 — 전체 화면들

/**
 * 시트 **몸통 위** 글자색.
 *
 * 시트는 패미컴 스타일에서 회색 플라스틱이 통째로 올라오는 것이라, 그 위의 글자는
 * 잉크가 아니라 플라스틱에 새긴 검정입니다. 자리마다 조건문을 쓰면 시트 넷이
 * 지저분해져서 여기 두 개로 모읍니다.
 */
private val bodyInk: Color
    @Composable get() = if (PLASTIC_TRIAL) PlasticColors.Ink else MemoryColors.Ink

/** 몸통 위의 흐린 글자 (설명·각주) */
private val bodyDim: Color
    @Composable get() = if (PLASTIC_TRIAL) PlasticColors.TrimLo else MemoryColors.Ink2

/**
 * 시트 제목. 뒤로 버튼을 두지 않습니다 — 시트는 끌어 내리거나 뒤를 눌러 닫습니다.
 * 버튼을 또 두면 닫는 길이 셋이 됩니다.
 */
@Composable
private fun SheetTitle(text: String) {
    // 몸통 위의 글자는 잉크가 아니라 플라스틱에 새긴 검정입니다.
    Text(
        text = text,
        style = MemoryType.Title,
        color = if (PLASTIC_TRIAL) PlasticColors.Ink else MemoryColors.Ink,
        modifier = Modifier.padding(bottom = Gap.m),
    )
}

/** "어떻게 쓸까요" 같은 구역 이름표. */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MemoryType.Micro,
        color = if (PLASTIC_TRIAL) PlasticColors.TrimLo else MemoryColors.Ink2,
        letterSpacing = 0.7.sp,
        modifier = modifier,
    )
}

/**
 * 글자칸. 흰 면에 1px 잉크 선 — 회색 면을 쓰지 않습니다.
 *
 * 패미컴 스타일에서는 **카트리지 슬롯**입니다. 지도 검색칸·올리기의 어디·언제와
 * 같은 모양인데, 셋 다 "값을 꽂아 넣는 자리" 라서 같아야 합니다.
 */
@Composable
private fun Field(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    if (PLASTIC_TRIAL) {
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
                if (value.isEmpty()) {
                    Text(placeholder, style = MemoryType.Body, color = PlasticColors.OnPlateDim)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MemoryType.Body.copy(color = PlasticColors.OnPlate),
                    cursorBrush = SolidColor(PlasticColors.Red),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }

    Box(
        Modifier
            .fillMaxWidth()
            .background(MemoryColors.Surface)
            .border(MemoryStroke.Border, MemoryColors.Line)
            .padding(horizontal = 14.dp, vertical = 13.dp),
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

// ---------------------------------------------------------------------------
// 새 짜국 (시안 2a)

@Composable
private fun CreateSheet(state: SpaceListState, onIntent: (SpaceListIntent) -> Unit) {
    SheetBody {
        SheetTitle("새 짜국")

        Column {
            SectionLabel("어떻게 쓸까요", Modifier.padding(bottom = Gap.s))
            KindOption(
                title = "혼자",
                detail = "사진이 이 폰에만 남아요 · 로그인 없이 바로 시작해요",
                sub = "폰을 잃어버리면 사진도 함께 사라져요",
                checked = state.pendingKind == SpaceKind.Personal,
                onClick = { onIntent(SpaceListIntent.KindSelected(SpaceKind.Personal)) },
            )
            Spacer(Modifier.height(9.dp))
            KindOption(
                title = "같이",
                detail = "초대한 사람들과 같이 채우고 같이 봐요 · 로그인이 필요해요",
                sub = "만들면 초대 코드가 바로 나와요",
                checked = state.pendingKind == SpaceKind.Shared,
                onClick = { onIntent(SpaceListIntent.KindSelected(SpaceKind.Shared)) },
            )

            SectionLabel("이름", Modifier.padding(top = Gap.xl, bottom = Gap.s))
            Field(
                value = state.pendingName,
                placeholder = "예) 우리 여름, 제주 한 달",
                onValueChange = { onIntent(SpaceListIntent.NameTyped(it)) },
            )
        }

        Spacer(Modifier.height(Gap.xl))
        PrimaryButton(
            text = if (state.working) "만드는 중…" else "만들기",
            enabled = state.canCreate(),
            onClick = { onIntent(SpaceListIntent.CreateConfirmed) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.pendingKind == SpaceKind.Personal) {
            Text(
                text = "로그인 없이 바로 만들어져요.",
                style = MemoryType.Micro,
                color = MemoryColors.Ink2,
                modifier = Modifier.padding(top = Gap.s),
            )
        }
    }
}

/**
 * 시트 안쪽 여백. 아래쪽은 넉넉히 둡니다 — 홈 인디케이터 자리와
 * 손가락이 시트를 잡는 자리를 버튼과 겹치지 않게 하려는 것입니다.
 */
@Composable
private fun SheetBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.padding(start = Gap.xl, end = Gap.xl, top = Gap.l, bottom = Gap.xxxl),
        content = content,
    )
}

/**
 * 혼자 / 같이 고르기.
 *
 * **세로로 쌓는 이유**: 줄마다 설명이 두 줄씩 붙습니다. `혼자|같이` 알약에는 설명이
 * 안 들어가고, 설명 없이 두면 사진이 폰 밖으로 나가는지 모르고 고르게 됩니다.
 *
 * 고른 칸은 **2px 레드 테두리**, 아닌 칸은 1px 잉크 40%. 라디오도 원이 아니라
 * **네모**입니다 — 이 디자인에 둥근 것은 없습니다.
 */
@Composable
private fun KindOption(
    title: String,
    detail: String,
    sub: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    // 패미컴 스타일에서는 **화면 안에 끼운 칸**입니다. 고른 칸만 왼쪽에 빨간 막대가
    // 서고 바닥이 밝아집니다 — 달력의 '고른 날' 과 같은 규칙입니다. 라디오 네모는
    // 뺐습니다: 빨간 막대와 밝아진 바닥이 이미 고른 것을 말하는데, 표식이 셋이 되면
    // 무엇을 봐야 할지 흐려집니다.
    if (PLASTIC_TRIAL) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(PlasticShapes.Chip)
                .background(if (checked) PlasticColors.PlateHi else PlasticColors.Plate)
                .selectable(selected = checked, role = Role.RadioButton, onClick = onClick)
                .padding(end = 15.dp, top = 13.dp, bottom = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(Gap.m),
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(PlasticSize.KindBar)
                    .background(if (checked) PlasticColors.Red else PlasticColors.Plate)
            )
            Column {
                Text(title, style = MemoryType.Headline, color = PlasticColors.OnPlate)
                Text(
                    text = detail,
                    style = MemoryType.Label,
                    color = PlasticColors.OnPlateDim,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(sub, style = MemoryType.Label, color = PlasticColors.OnPlateDim)
            }
        }
        return
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(MemoryColors.Surface)
            .then(
                if (checked) Modifier.border(2.dp, MemoryColors.Accent)
                else Modifier.border(MemoryStroke.Border, MemoryColors.Line2)
            )
            .selectable(selected = checked, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(Gap.m),
    ) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(16.dp)
                .border(MemoryStroke.Border, MemoryColors.Line),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Box(Modifier.size(8.dp).background(MemoryColors.Accent))
            }
        }
        Column {
            Text(title, style = MemoryType.Headline)
            Text(
                detail,
                style = MemoryType.Label,
                color = MemoryColors.Ink2,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(sub, style = MemoryType.Label, color = MemoryColors.Ink3)
        }
    }
}

// ---------------------------------------------------------------------------
// 초대 코드로 참여

/** 시안에 따로 그려져 있지 않아 '새 짜국' 과 같은 뼈대로 갑니다. */
@Composable
private fun JoinSheet(state: SpaceListState, onIntent: (SpaceListIntent) -> Unit) {
    SheetBody {
        SheetTitle("초대 코드로 참여")

        SectionLabel("받은 코드", Modifier.padding(bottom = Gap.s))
        Field(
            value = state.pendingCode,
            placeholder = "예) K7QF2M",
            onValueChange = { onIntent(SpaceListIntent.CodeTyped(it)) },
        )
        Text(
            text = "코드를 보낸 사람의 짜국에 멤버로 들어가요.",
            style = MemoryType.Micro,
            color = bodyDim,
            modifier = Modifier.padding(top = Gap.s),
        )

        Spacer(Modifier.height(Gap.xl))
        PrimaryButton(
            text = if (state.working) "확인 중…" else "참여하기",
            enabled = state.canJoin(),
            onClick = { onIntent(SpaceListIntent.JoinConfirmed) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------------
// 로그인 (시안 3a)

/** 같이 쓰기·참여 때만 끼어드는 화면. 앱을 켤 때는 뜨지 않습니다. */
@Composable
private fun SignInSheet(
    sheet: SpaceListSheet.SignIn,
    working: Boolean,
    onIntent: (SpaceListIntent) -> Unit,
) {
    SheetBody {
        Box(
            Modifier
                .size(13.dp)
                .then(if (PLASTIC_TRIAL) Modifier.clip(PlasticShapes.Chip) else Modifier)
                .background(if (PLASTIC_TRIAL) PlasticColors.Red else MemoryColors.Accent)
        )
        Spacer(Modifier.height(14.dp))
        // 시트에서는 25단이 너무 큽니다 — 제목만으로 시트가 반을 먹습니다.
        Text("같이 보려면 계정이 필요해요", style = MemoryType.Title, color = bodyInk)
        Spacer(Modifier.height(Gap.s))
        Text(
            text = "로그인은 멤버가 아닌 사람이 우리 사진을 못 보게 막는 잠금장치예요. " +
                "계정 확인 말고 다른 데는 쓰지 않아요.",
            style = MemoryType.Label,
            color = bodyDim,
        )

        Spacer(Modifier.height(Gap.xl))

        GoogleButton(
            text = if (working) "로그인 중…" else "Google로 계속하기",
            enabled = !working,
            onClick = { onIntent(SpaceListIntent.SignInTapped) },
        )

        // 만들기에서 왔을 때만 빠져나갈 길을 둡니다. 참여로 왔으면 혼자로 갈
        // 곳이 없습니다 — 남의 짜국에 혼자 들어갈 수는 없으니까요.
        if (sheet.next == SpaceListSheet.Next.Create) {
            Text(
                text = "그냥 혼자 쓸래요 — 이 폰에만 저장할게요",
                style = MemoryType.Body,
                color = bodyInk,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onIntent(SpaceListIntent.SignInGaveUp) }
                    .padding(top = 14.dp),
            )
        }

        Text(
            text = "지금은 Google 계정만 돼요.",
            style = MemoryType.Micro,
            color = bodyDim,
            modifier = Modifier.padding(top = Gap.l),
        )
    }
}

/**
 * 구글 버튼만 **레드가 아닙니다.** 우리 것이 아니라 남의 서비스로 넘어가는 문이라
 * 앱의 강조색을 입히면 우리가 하는 일처럼 보입니다. 흰 바탕에 잉크 선 —
 * 구글이 권하는 모양이기도 합니다.
 *
 * 패미컴 스타일에서도 **흰 면 그대로 둡니다.** 다른 버튼은 다 고무·플라스틱이 됐지만,
 * 이 버튼만은 구글이 정한 모양을 지켜야 합니다. 하우징에 앉혀 기기에 달린 것처럼
 * 보이게 하되, 버튼 얼굴 자체는 손대지 않습니다.
 */
@Composable
private fun GoogleButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    if (PLASTIC_TRIAL) {
        Box(
            Modifier
                .fillMaxWidth()
                .raisedPlastic(PlasticShapes.Housing)
                .padding(PlasticSize.ButtonInset)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(PlasticShapes.Pill)
                    .background(MemoryColors.Surface)
                    .clickable(enabled = enabled, onClick = onClick)
                    .padding(horizontal = Gap.l, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                GoogleMark(Modifier.size(19.dp))
                Text(text, style = MemoryType.Headline, color = MemoryColors.Ink)
            }
        }
        return
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(MemoryColors.Surface)
            .border(MemoryStroke.Border, MemoryColors.Line)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Gap.l, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        GoogleMark(Modifier.size(19.dp))
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

// ---------------------------------------------------------------------------
// 초대 코드 (시안 2b)

/**
 * 만든 직후. 뒤로 대신 아래 '짜국 열기' 로 나갑니다 — 하드웨어 뒤로가기는 목록으로.
 *
 * 시안의 "7일 동안 쓸 수 있어요" 는 넣지 않았습니다. 코드에 만료가 **없어서**
 * 사실이 아닙니다. 만료를 만들게 되면 그때 같이 넣습니다.
 */
@Composable
private fun InvitedSheet(sheet: SpaceListSheet.Invited, onIntent: (SpaceListIntent) -> Unit) {
    SheetBody {
        Text("'${sheet.spaceName}' 짜국을 만들었어요", style = MemoryType.Title, color = bodyInk)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "이 코드를 받은 사람이 들어올 수 있어요. 지금 보내 두면 나중에 다시 찾지 않아도 돼요.",
            style = MemoryType.Label,
            color = bodyDim,
        )

        Spacer(Modifier.height(18.dp))
        // 코드는 **화면에 띄웁니다.** 패미컴 스타일에서 검정 판은 "기기가 보여 주는 것"
        // 이고, 이 코드야말로 기기가 방금 만들어 낸 값입니다.
        Box(
            Modifier
                .fillMaxWidth()
                .then(
                    if (PLASTIC_TRIAL) {
                        Modifier.sunken(PlasticShapes.Screen)
                    } else {
                        Modifier.background(MemoryColors.Surface).border(MemoryStroke.Border, MemoryColors.Line)
                    }
                )
                .padding(horizontal = Gap.xl, vertical = 18.dp),
        ) {
            // 코드는 글자 단 밖입니다 — UI 글이 아니라 화면의 주인공(콘텐츠)이라서요.
            // 자간을 넓게 벌려 한 글자씩 옮겨 적기 쉽게 합니다.
            Text(
                text = sheet.code,
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = 8.sp,
                color = if (PLASTIC_TRIAL) PlasticColors.OnPlate else MemoryColors.Ink,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(Gap.s)) {
            SoftButton(
                text = "코드 복사",
                onClick = { onIntent(SpaceListIntent.InviteCopied(sheet.code)) },
                modifier = Modifier.weight(1f),
            )
            SoftButton(
                text = "공유하기",
                onClick = { onIntent(SpaceListIntent.InviteShared(sheet.code)) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Gap.l))
        PrimaryButton(
            text = "짜국 열기",
            onClick = { onIntent(SpaceListIntent.InviteOpenTapped) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
