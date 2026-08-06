package kr.surprise.memorymap.feature.space

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kr.surprise.memorymap.core.designsystem.component.FRAMES_RATIO
import kr.surprise.memorymap.core.designsystem.component.PhotoFramesScene
import kr.surprise.memorymap.core.designsystem.theme.PlasticColors
import kr.surprise.memorymap.core.designsystem.theme.PlasticShapes
import kr.surprise.memorymap.core.designsystem.theme.Pretendard
import kr.surprise.memorymap.core.designsystem.theme.raisedPlastic
import kr.surprise.memorymap.core.designsystem.theme.sunken
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceKind

/**
 * **시험용 화면 — 패미컴 컨트롤러 스타일의 짜국 목록.**
 *
 * 지금 앱의 목록은 [SpaceListScreen] 안의 `ListBody` 이고, 이 파일은 같은 상태를
 * 다른 옷으로 그린 것뿐입니다. 상태·Intent·시트는 하나도 건드리지 않습니다 —
 * 켜고 끄는 것은 `SpaceListScreen` 의 `PLASTIC_TRIAL` 하나입니다.
 *
 * 옮긴 규칙:
 * - 화면 바탕 = 회색 플라스틱 몸통
 * - 사진이 놓이는 판 = 검정 페이스플레이트
 * - 짜국 카드 = 버튼 하우징에 **움푹 끼운** 사진. 사진은 손대지 않습니다
 * - 주 동작 = 빨간 A 버튼, 보조 = SELECT 고무 알약
 */
@Composable
internal fun PlasticListBody(state: SpaceListState, onIntent: (SpaceListIntent) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(PlasticColors.Body)
            .padding(horizontal = 8.dp)
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
                is SpacesUi.Loading -> PlateHint("불러오는 중이에요")
                is SpacesUi.Failed -> PlateHint("목록을 불러오지 못했어요")
                is SpacesUi.Ready ->
                    if (ui.items.isEmpty()) {
                        PlateEmpty()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(10.dp),
                            verticalArrangement = Arrangement.spacedBy(11.dp),
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
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "짜국",
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = (-0.5).sp,
            color = PlasticColors.Red,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "MAP & CALENDAR",
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            letterSpacing = 1.4.sp,
            color = PlasticColors.TrimLo,
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
}

/** 몸통에 새긴 회색 줄무늬 셋. 컨트롤러 얼굴의 그 줄입니다. */
@Composable
private fun Stripes() {
    Column(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.Trim)
            )
        }
    }
}

/** 카드 사진의 높이. 시안의 92/300 비를 폰 너비에 맞춰 키운 값입니다. */
private val PhotoHeight = 108.dp

@Composable
private fun PlasticCard(space: Space, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        // 하우징 — 볼록한 플라스틱. 그 안에 사진을 움푹 끼웁니다.
        Box(Modifier.fillMaxWidth().raisedPlastic(PlasticShapes.Housing).padding(5.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(PhotoHeight)
                    .sunken(PlasticShapes.Chip, face = PlasticColors.PlateLo, rim = 2.dp)
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
                OnlyHere(Modifier.align(Alignment.TopStart).padding(10.dp))
            }
        }

        Spacer(Modifier.height(7.dp))

        // 이름줄은 검정 판 위에 그대로 놓입니다 — 여기에도 베벨을 주면 자글자글해집니다.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = space.name,
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PlasticColors.OnPlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = space.metaShort(),
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    color = PlasticColors.OnPlateDim,
                )
            }
            Crew(space.members.map { it.initial })
        }
    }
}

/** "사진 13 · 8곳 · 7.27" — 몰드된 라벨처럼 짧게 끊습니다. */
private fun Space.metaShort(): String {
    if (photoCount == 0) return "아직 비어 있어요"
    return buildString {
        append(photoCount).append(" · ").append(regionCount).append("곳")
        lastPhotoOn?.let { append(" · ").append(it.monthValue).append(".").append(it.dayOfMonth) }
    }
}

@Composable
private fun OnlyHere(modifier: Modifier = Modifier) {
    Text(
        text = "이 폰에만",
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 8.sp,
        letterSpacing = 0.6.sp,
        color = PlasticColors.Plate,
        modifier = modifier
            .clip(PlasticShapes.Pill)
            .background(PlasticColors.Body)
            .padding(horizontal = 7.dp, vertical = 3.dp),
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
            .offset(x = (-2 * index).dp)
            .size(17.dp)
            .clip(PlasticShapes.Chip)
            .background(if (filled) PlasticColors.TrimLo else PlasticColors.Body),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            color = if (filled) PlasticColors.Body else PlasticColors.Plate,
        )
    }
}

/**
 * 아래 조작부 — SELECT 알약과 빨간 A 버튼.
 *
 * 라벨(SELECT·START)을 버튼 **밖 몸통에** 찍습니다. 실제 컨트롤러가 그렇고,
 * 그래야 버튼 안에는 우리 말만 남습니다.
 */
@Composable
private fun Controls(onCreate: () -> Unit, onJoin: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.Rubber)
                    .clickable(onClick = onJoin),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "초대 코드로 참여",
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = PlasticColors.OnRubber,
                )
            }
            Text(
                text = "SELECT",
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 7.sp,
                letterSpacing = 1.6.sp,
                color = PlasticColors.Red,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 3.dp),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .raisedPlastic(PlasticShapes.Housing)
                    .padding(7.dp)
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(PlasticShapes.Pill)
                        .background(PlasticColors.Red)
                        .clickable(onClick = onCreate),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "＋",
                        fontFamily = Pretendard,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = PlasticColors.OnRed,
                    )
                }
            }
            Text(
                text = "START",
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 7.sp,
                letterSpacing = 1.4.sp,
                color = PlasticColors.Red,
                modifier = Modifier.padding(top = 3.dp),
            )
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
            fontSize = 12.sp,
            color = PlasticColors.OnPlateDim,
        )
    }
}

@Composable
private fun PlateEmpty() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        PhotoFramesScene(Modifier.fillMaxWidth(0.42f).aspectRatio(FRAMES_RATIO))
        Spacer(Modifier.height(14.dp))
        Text(
            text = "아직 짜국이 없어요",
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = PlasticColors.OnPlate,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "아래 빨간 버튼으로 하나 만들어 보세요.",
            fontFamily = Pretendard,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = PlasticColors.OnPlateDim,
        )
    }
}
