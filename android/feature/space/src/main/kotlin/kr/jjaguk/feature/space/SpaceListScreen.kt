package kr.jjaguk.feature.space

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.jjaguk.core.designsystem.component.PrimaryButton
import kr.jjaguk.core.designsystem.component.SoftButton
import kr.jjaguk.core.designsystem.theme.MemoryColors
import kr.jjaguk.core.designsystem.theme.MemoryType
import kr.jjaguk.core.designsystem.theme.PlasticColors
import kr.jjaguk.core.designsystem.theme.PlasticShapes
import kr.jjaguk.core.designsystem.theme.PlasticSize
import kr.jjaguk.core.designsystem.theme.PressStart
import kr.jjaguk.core.designsystem.theme.AppFont
import kr.jjaguk.core.designsystem.theme.Space as Gap
import kr.jjaguk.core.designsystem.theme.raisedPlastic
import kr.jjaguk.core.designsystem.theme.sunken
import kr.jjaguk.core.model.SpaceKind

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
        PlasticListBody(state, onIntent)
    }

    if (state.sheet != SpaceListSheet.None) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(SpaceListIntent.SheetDismissed) },
            sheetState = rememberModalBottomSheetState(),
            containerColor = PlasticColors.Body,
            // 시트 위쪽에도 2px 잉크 선을 긋습니다 — 지역 시트와 같은 규칙입니다.
            dragHandle = { SheetGrip() },
            shape = PlasticShapes.Device,
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

// ---------------------------------------------------------------------------
// 목록

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
    @Composable get() = PlasticColors.Ink

/** 몸통 위의 흐린 글자 (설명·각주) */
private val bodyDim: Color
    @Composable get() = PlasticColors.TrimLo

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
        color = PlasticColors.Ink,
        modifier = Modifier.padding(bottom = Gap.m),
    )
}

/** "어떻게 쓸까요" 같은 구역 이름표. */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MemoryType.Micro,
        color = PlasticColors.TrimLo,
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
}

// ---------------------------------------------------------------------------
// 새 짜국 (시안 2a)

@Composable
private fun CreateSheet(state: SpaceListState, onIntent: (SpaceListIntent) -> Unit) {
    SheetBody {
        SheetTitle(stringResource(R.string.create_title))

        Column {
            SectionLabel(stringResource(R.string.create_kind_label), Modifier.padding(bottom = Gap.s))
            KindOption(
                title = stringResource(R.string.create_kind_solo),
                detail = stringResource(R.string.create_kind_solo_detail),
                sub = stringResource(R.string.create_kind_solo_sub),
                checked = state.pendingKind == SpaceKind.Personal,
                onClick = { onIntent(SpaceListIntent.KindSelected(SpaceKind.Personal)) },
            )
            Spacer(Modifier.height(9.dp))
            KindOption(
                title = stringResource(R.string.create_kind_shared),
                detail = stringResource(R.string.create_kind_shared_detail),
                sub = stringResource(R.string.create_kind_shared_sub),
                checked = state.pendingKind == SpaceKind.Shared,
                onClick = { onIntent(SpaceListIntent.KindSelected(SpaceKind.Shared)) },
            )

            SectionLabel(stringResource(R.string.create_name_label), Modifier.padding(top = Gap.xl, bottom = Gap.s))
            Field(
                value = state.pendingName,
                placeholder = stringResource(R.string.create_name_placeholder),
                onValueChange = { onIntent(SpaceListIntent.NameTyped(it)) },
            )
        }

        Spacer(Modifier.height(Gap.xl))
        PrimaryButton(
            text = stringResource(if (state.working) R.string.create_working else R.string.create_confirm),
            enabled = state.canCreate(),
            onClick = { onIntent(SpaceListIntent.CreateConfirmed) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.pendingKind == SpaceKind.Personal) {
            Text(
                text = stringResource(R.string.create_solo_note),
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
}

// ---------------------------------------------------------------------------
// 초대 코드로 참여

/** 시안에 따로 그려져 있지 않아 '새 짜국' 과 같은 뼈대로 갑니다. */
@Composable
private fun JoinSheet(state: SpaceListState, onIntent: (SpaceListIntent) -> Unit) {
    SheetBody {
        SheetTitle(stringResource(R.string.join_title))

        SectionLabel(stringResource(R.string.join_code_label), Modifier.padding(bottom = Gap.s))
        Field(
            value = state.pendingCode,
            placeholder = stringResource(R.string.join_code_placeholder),
            onValueChange = { onIntent(SpaceListIntent.CodeTyped(it)) },
        )
        Text(
            text = stringResource(R.string.join_note),
            style = MemoryType.Micro,
            color = bodyDim,
            modifier = Modifier.padding(top = Gap.s),
        )

        Spacer(Modifier.height(Gap.xl))
        PrimaryButton(
            text = stringResource(if (state.working) R.string.join_working else R.string.join_confirm),
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
                .clip(PlasticShapes.Chip)
                .background(PlasticColors.Red)
        )
        Spacer(Modifier.height(14.dp))
        // 시트에서는 25단이 너무 큽니다 — 제목만으로 시트가 반을 먹습니다.
        Text(stringResource(R.string.signin_title), style = MemoryType.Title, color = bodyInk)
        Spacer(Modifier.height(Gap.s))
        Text(
            text = stringResource(R.string.signin_why),
            style = MemoryType.Label,
            color = bodyDim,
        )

        Spacer(Modifier.height(Gap.xl))

        GoogleButton(
            text = stringResource(if (working) R.string.signin_working else R.string.signin_google),
            enabled = !working,
            onClick = { onIntent(SpaceListIntent.SignInTapped) },
        )

        // 만들기에서 왔을 때만 빠져나갈 길을 둡니다. 참여로 왔으면 혼자로 갈
        // 곳이 없습니다 — 남의 짜국에 혼자 들어갈 수는 없으니까요.
        if (sheet.next == SpaceListSheet.Next.Create) {
            Text(
                text = stringResource(R.string.signin_give_up),
                style = MemoryType.Body,
                color = bodyInk,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onIntent(SpaceListIntent.SignInGaveUp) }
                    .padding(top = 14.dp),
            )
        }

        Text(
            text = stringResource(R.string.signin_only_google),
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
        Text(stringResource(R.string.invited_title, sheet.spaceName), style = MemoryType.Title, color = bodyInk)
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.invited_note),
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
                    Modifier.sunken(PlasticShapes.Screen)
                )
                .padding(horizontal = Gap.xl, vertical = 18.dp),
        ) {
            // 코드는 글자 단 밖입니다 — UI 글이 아니라 화면의 주인공(콘텐츠)이라서요.
            // 카트리지 뒷면 시리얼처럼 PS2P 로 새깁니다 (2026-08-09 검수 시안,
            // 코드 글자표가 라틴·숫자뿐이라 이 서체로 다 찍힙니다). 크기는 8의 배수.
            Text(
                text = sheet.code,
                fontFamily = PressStart,
                fontSize = 24.sp,
                letterSpacing = 6.sp,
                color = PlasticColors.OnPlate,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(Gap.s)) {
            SoftButton(
                text = stringResource(R.string.invited_copy),
                onClick = { onIntent(SpaceListIntent.InviteCopied(sheet.code)) },
                modifier = Modifier.weight(1f),
            )
            SoftButton(
                text = stringResource(R.string.invited_share),
                onClick = { onIntent(SpaceListIntent.InviteShared(sheet.code)) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Gap.l))
        PrimaryButton(
            text = stringResource(R.string.invited_open),
            onClick = { onIntent(SpaceListIntent.InviteOpenTapped) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
