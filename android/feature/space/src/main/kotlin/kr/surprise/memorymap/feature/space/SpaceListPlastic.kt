package kr.surprise.memorymap.feature.space

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kr.surprise.memorymap.core.designsystem.R as DesignR
import kr.surprise.memorymap.core.designsystem.component.FRAMES_RATIO
import kr.surprise.memorymap.core.designsystem.component.PhotoFramesScene
import kr.surprise.memorymap.core.designsystem.theme.PlasticColors
import kr.surprise.memorymap.core.designsystem.theme.PlasticShapes
import kr.surprise.memorymap.core.designsystem.theme.PlasticSize
import kr.surprise.memorymap.core.designsystem.theme.Pretendard
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap
import kr.surprise.memorymap.core.designsystem.theme.pressable
import kr.surprise.memorymap.core.designsystem.theme.raisedPlastic
import kr.surprise.memorymap.core.designsystem.theme.sunken
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceKind

/**
 * **짜국 목록 — 패미컴 컨트롤러 스타일.**
 *
 * [SpaceListScreen] 이 상태를 들고 이 파일이 그립니다. 여기에는 상태도 Intent 도
 * 없습니다 — 받은 것을 그리고, 누른 것을 올려 보낼 뿐입니다.
 *
 * 옮긴 규칙:
 * - 화면 바탕 = 회색 플라스틱 몸통
 * - 사진이 놓이는 판 = 검정 페이스플레이트
 * - 짜국 카드 = 버튼 하우징에 **움푹 끼운** 사진. 사진은 손대지 않습니다
 * - 주 동작 = 빨간 A 버튼, 보조 = 검은 고무 알약
 *
 * 글자 크기는 [kr.surprise.memorymap.core.designsystem.theme.MemoryType] 의 단
 * (25/17/15/13.5/12.5/11) 을 그대로 씁니다. 시안의 px 값을 sp 로 옮기면 안 됩니다 —
 * 시안은 300px 폭이라 실제 폰보다 좁아서, 그대로 옮기면 글씨가 작아집니다.
 */
@Composable
internal fun PlasticListBody(state: SpaceListState, onIntent: (SpaceListIntent) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(PlasticColors.Body)
            .padding(horizontal = Gap.s)
    ) {
        Brand()
        Stripes()

        // 검정 페이스플레이트. 남는 세로를 다 먹고, 그 안에서만 목록이 구릅니다.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .sunken(PlasticShapes.Screen)
        ) {
            when (val ui = state.spaces) {
                is SpacesUi.Loading -> PlateHint(stringResource(R.string.list_loading))
                is SpacesUi.Failed -> PlateHint(stringResource(R.string.list_failed_short))
                is SpacesUi.Ready ->
                    if (ui.items.isEmpty()) {
                        PlateEmpty()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(Gap.m),
                            verticalArrangement = Arrangement.spacedBy(Gap.m),
                        ) {
                            items(ui.items, key = { it.id.value }) { space ->
                                PlasticCard(space) { onIntent(SpaceListIntent.SpaceTapped(space.id)) }
                            }
                        }
                    }
            }
        }

        Controls(
            onCreate = { onIntent(SpaceListIntent.CreateTapped) },
            onJoin = { onIntent(SpaceListIntent.JoinTapped) },
        )
    }
}

/** 로고 자리. 기울임은 쓰지 않습니다 — Pretendard 에 진짜 이탤릭이 없어 흉내만 나옵니다. */
@Composable
private fun Brand() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gap.xs).padding(top = Gap.xs, bottom = Gap.s),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(R.string.list_title),
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
            letterSpacing = (-0.5).sp,
            color = PlasticColors.Red,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "MAP & CALENDAR",
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            color = PlasticColors.TrimLo,
            modifier = Modifier.padding(bottom = Gap.xs),
        )
    }
}

/** 몸통에 새긴 회색 줄무늬 셋. 컨트롤러 얼굴의 그 줄입니다. */
@Composable
private fun Stripes() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Gap.xs).padding(bottom = Gap.s),
        verticalArrangement = Arrangement.spacedBy(Gap.xs),
    ) {
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(PlasticSize.Stripe)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.Trim)
            )
        }
    }
}

@Composable
private fun PlasticCard(space: Space, onClick: () -> Unit) {
    Column(Modifier.pressable(onClick = onClick)) {
        // 하우징 — 볼록한 플라스틱. 그 안에 사진을 움푹 끼웁니다.
        Box(Modifier.fillMaxWidth().raisedPlastic(PlasticShapes.Housing).padding(PlasticSize.HousingInset)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(PlasticSize.Photo)
                    .sunken(PlasticShapes.Chip, face = PlasticColors.PlateLo)
            ) {
                space.coverPhotoUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (space.kind == SpaceKind.Personal) {
                OnlyHere(Modifier.align(Alignment.TopStart).padding(Gap.s))
            }
        }

        Spacer(Modifier.height(Gap.s))

        // 이름줄은 검정 판 위에 그대로 놓입니다 — 여기에도 베벨을 주면 자글자글해집니다.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Gap.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = space.name,
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = PlasticColors.OnPlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = space.metaShort(),
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = PlasticColors.OnPlateDim,
                )
            }
            Crew(space.members.map { it.initial })
        }
    }
}

/** "사진 13 · 8곳 · 7.27" — 몰드된 라벨처럼 짧게 끊습니다. */
@Composable
private fun Space.metaShort(): String {
    if (photoCount == 0) return stringResource(R.string.card_meta_short_empty)
    val on = lastPhotoOn ?: return stringResource(R.string.card_meta_short, photoCount, regionCount)
    return stringResource(
        R.string.card_meta_short_dated,
        photoCount, regionCount, on.monthValue, on.dayOfMonth,
    )
}

@Composable
private fun OnlyHere(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(DesignR.string.component_only_on_this_phone),
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = PlasticColors.Plate,
        modifier = modifier
            .clip(PlasticShapes.Pill)
            .background(PlasticColors.Body)
            .padding(horizontal = Gap.s, vertical = Gap.xs),
    )
}

/** 멤버는 작은 플라스틱 칩. 셋까지 보이고 넘으면 어두운 칩에 +N. */
@Composable
private fun Crew(initials: List<String>) {
    val shown = initials.take(3)
    val rest = initials.size - shown.size

    Row {
        shown.forEachIndexed { index, text -> CrewChip(text, index, filled = false) }
        if (rest > 0) CrewChip("+$rest", shown.size, filled = true)
    }
}

@Composable
private fun CrewChip(text: String, index: Int, filled: Boolean) {
    Box(
        Modifier
            .offset(x = -(PlasticSize.ChipOverlap * index))
            .size(PlasticSize.Chip)
            .clip(PlasticShapes.Chip)
            .background(if (filled) PlasticColors.TrimLo else PlasticColors.Body),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (filled) PlasticColors.Body else PlasticColors.Plate,
        )
    }
}

/**
 * 아래 조작부 — 고무 알약과 빨간 A 버튼.
 *
 * **SELECT · START 라벨은 뺐습니다.** 컨트롤러에는 그 글자가 찍혀 있지만, 사진첩
 * 앱에서는 무엇을 하는 버튼인지 알려 주지 않는 장식일 뿐이라 뜬금없어 보입니다.
 * 형태(고무 알약 · 빨간 원 · 플라스틱 하우징)만으로 이미 컨트롤러로 읽힙니다.
 *
 * 둘 다 같은 플라스틱 하우징에 앉혀 높이를 맞춥니다.
 */
@Composable
private fun Controls(onCreate: () -> Unit, onJoin: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gap.xs, vertical = Gap.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap.m),
    ) {
        Box(
            Modifier
                .weight(1f)
                .raisedPlastic(PlasticShapes.Housing)
                .padding(PlasticSize.ButtonInset)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(PlasticSize.Button)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.Rubber)
                    .pressable(onClick = onJoin),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.list_join),
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PlasticColors.OnRubber,
                )
            }
        }

        Box(
            Modifier
                .raisedPlastic(PlasticShapes.Housing)
                .padding(PlasticSize.ButtonInset)
        ) {
            Box(
                Modifier
                    .size(PlasticSize.Button)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.Red)
                    .pressable(onClick = onCreate),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "＋",
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = PlasticColors.OnRed,
                )
            }
        }
    }
}

@Composable
private fun PlateHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            fontFamily = Pretendard,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            color = PlasticColors.OnPlateDim,
        )
    }
}

@Composable
private fun PlateEmpty() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = Gap.xxl),
        verticalArrangement = Arrangement.Center,
    ) {
        PhotoFramesScene(Modifier.fillMaxWidth(0.42f).aspectRatio(FRAMES_RATIO))
        Spacer(Modifier.height(Gap.l))
        Text(
            text = stringResource(R.string.list_empty_title),
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = PlasticColors.OnPlate,
        )
        Spacer(Modifier.height(Gap.s))
        Text(
            text = stringResource(R.string.list_empty_hint),
            fontFamily = Pretendard,
            fontWeight = FontWeight.Normal,
            fontSize = 12.5.sp,
            color = PlasticColors.OnPlateDim,
        )
    }
}
