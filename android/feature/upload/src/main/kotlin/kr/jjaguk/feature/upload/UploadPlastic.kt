package kr.jjaguk.feature.upload

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kr.jjaguk.core.designsystem.theme.PlasticColors
import kr.jjaguk.core.designsystem.theme.PlasticShapes
import kr.jjaguk.core.designsystem.theme.PlasticSize
import kr.jjaguk.core.designsystem.theme.Pretendard
import kr.jjaguk.core.designsystem.theme.Space as Gap
import kr.jjaguk.core.designsystem.theme.pressable
import kr.jjaguk.core.designsystem.theme.raisedPlastic
import kr.jjaguk.core.designsystem.theme.sunken

/**
 * **사진 올리기 — 패미컴 컨트롤러 스타일.**
 *
 * 지도·달력과 같은 짜임새입니다. 몸통 위에 화면을 끼우고, 조작(취소·올리기)은 화면 밖입니다.
 *
 * **'어디'·'언제' 칸은 지도의 카트리지 슬롯과 같은 모양입니다.** 둘 다 "값을 꽂아 넣는
 * 자리" 라서 같게 뒀습니다 — 이 스타일에서 파인 홈은 무언가를 넣는 곳이라는 뜻입니다.
 *
 * ⚠️ 목업은 사진 줄 하나에 '어디·언제' 한 벌이었지만, 실제 상태는 **사진 한 장마다**
 * 제 지역·날짜를 듭니다([UploadState.items] 의 설명 참고). 목업이 보여 준 것은
 * 생김새지 짜임새가 아니라서, 칸 모양만 가져오고 줄은 사진마다 그립니다.
 */
@Composable
internal fun PlasticUploadBody(
    state: UploadState,
    onIntent: (UploadIntent) -> Unit,
    onPickPhotos: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PlasticColors.Body)
            .padding(horizontal = Gap.s)
    ) {
        if (state.editingRegionOf != null) {
            PlasticRegionPicker(state, onIntent)
            return@Column
        }

        Header(count = state.items.size)

        Column(
            Modifier
                .fillMaxWidth()
                .sunken(PlasticShapes.Screen)
                .padding(Gap.s)
        ) {
            (state.step as? UploadStep.Failed)?.let {
                FailurePlate(savedLocally = it.savedLocally) { onIntent(UploadIntent.RetryTapped) }
                Spacer(Modifier.height(Gap.s))
            }

            if (state.items.isEmpty()) {
                EmptyPlate(onPickPhotos)
            } else {
                // **여기가 시트 높이를 정합니다.** 목록은 사진 수만큼 자라다가
                // 이 한도에서 멈추고 그 뒤로는 구릅니다 — 시트가 화면을 삼키지 않으면서도
                // 사진이 많을 때 훑어 내릴 수 있습니다.
                //
                // 머리말과 아래 버튼은 이 밖에 있어 늘 보입니다.
                LazyColumn(
                    Modifier.heightIn(max = PlasticSize.UploadList),
                    contentPadding = PaddingValues(bottom = Gap.xs),
                ) {
                    items(state.items, key = { it.uri }) { item ->
                        PlasticItemRow(item, onIntent)
                    }
                }

                state.splitCounts()?.let { split ->
                    Text(
                        text = stringResource(R.string.upload_split_notice, split.places, split.days),
                        fontFamily = Pretendard,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = PlasticColors.OnPlateDim,
                        modifier = Modifier.padding(top = Gap.xs),
                    )
                }
            }
        }

        Controls(
            uploadLabel = when (state.step) {
                UploadStep.Uploading -> stringResource(R.string.upload_uploading)
                UploadStep.Reading -> stringResource(R.string.upload_reading_short)
                else -> null
            },
            canUpload = state.canUpload(),
            onCancel = { onIntent(UploadIntent.Dismissed) },
            onUpload = { onIntent(UploadIntent.Confirmed) },
        )
    }
}

@Composable
private fun Header(count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gap.xs, vertical = Gap.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.upload_title),
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = PlasticColors.Ink,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) {
            Text(
                text = stringResource(R.string.upload_count, count),
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = PlasticColors.TrimLo,
            )
        }
    }
}

/**
 * 사진 한 장 — 왼쪽에 그림, 오른쪽에 '어디'·'언제' 슬롯 둘.
 *
 * 사진은 화면 안에 **움푹 끼웁니다.** 목록 화면의 카드는 볼록한 하우징에 끼웠는데,
 * 거기는 카드가 기기 위에 놓인 물건이고 여기는 이미 화면 안이라 또 볼록하게 하면
 * 화면에서 플라스틱이 튀어나온 꼴이 됩니다.
 */
@Composable
private fun PlasticItemRow(item: UploadItem, onIntent: (UploadIntent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Gap.xs),
        horizontalArrangement = Arrangement.spacedBy(Gap.s),
    ) {
        Box(
            Modifier
                .size(PlasticSize.UploadThumb)
                .clip(PlasticShapes.Chip)
                .background(PlasticColors.PlateLo)
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Gap.xs),
        ) {
            SlotField(
                label = stringResource(R.string.upload_field_where),
                value = item.region?.displayName ?: stringResource(R.string.upload_field_pick),
                dimmed = item.region == null,
                auto = item.regionAuto,
                onClick = { onIntent(UploadIntent.RegionFieldTapped(item.uri)) },
            )
            SlotField(
                label = stringResource(R.string.upload_field_when),
                value = stringResource(R.string.upload_date, item.takenOn.monthValue, item.takenOn.dayOfMonth),
                dimmed = false,
                auto = item.dateAuto,
                onClick = { onIntent(UploadIntent.DateFieldTapped(item.uri)) },
            )
        }
    }
}

/**
 * 값을 꽂아 넣는 칸. 지도 검색칸과 같은 슬롯 모양입니다.
 *
 * 왼쪽의 작은 검은 홈이 라벨 대신 "여기에 넣는다" 를 말합니다. 라벨 글자는 그 옆에
 * 작게 남겨 뒀습니다 — 홈만으로는 어디인지 언제인지 가릴 수 없습니다.
 */
@Composable
private fun SlotField(
    label: String,
    value: String,
    dimmed: Boolean,
    auto: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(PlasticShapes.Chip)
            .background(PlasticColors.Plate)
            .pressable(onClick = onClick)
            .padding(horizontal = Gap.s, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap.s),
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 12.dp)
                .clip(PlasticShapes.Chip)
                .background(PlasticColors.Ink)
        )
        Text(
            text = label,
            fontFamily = Pretendard,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = PlasticColors.OnPlateDim,
            modifier = Modifier.width(24.dp),
        )
        Text(
            text = value,
            fontFamily = Pretendard,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            color = if (dimmed) PlasticColors.OnPlateDim else PlasticColors.OnPlate,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (auto) {
            Text(
                text = stringResource(R.string.upload_auto_badge),
                fontFamily = Pretendard,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = PlasticColors.OnPlateDim,
            )
        }
    }
}

/**
 * 못 올렸을 때. **왜 안 됐는지와 사진이 어디 있는지**를 같이 말합니다 —
 * 실패만 알리면 사용자는 사진을 잃었다고 생각합니다.
 *
 * 화면 안이라 빨간 **면**을 쓰지 않고 빨간 글자와 왼쪽 선만 씁니다.
 * 검정 판 위에 빨간 상자를 놓으면 아래 조작부의 A 버튼보다 세게 튑니다.
 */
@Composable
private fun FailurePlate(savedLocally: Boolean, onRetry: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.width(3.dp).height(PlasticSize.FailureBar).background(PlasticColors.Red))
        Column(Modifier.padding(start = Gap.s)) {
            Text(
                text = stringResource(R.string.upload_failed_title),
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = PlasticColors.RedHi,
            )
            Text(
                text = stringResource(
                    if (savedLocally) R.string.upload_failed_kept else R.string.upload_failed_plain
                ),
                fontFamily = Pretendard,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = PlasticColors.OnPlateDim,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = stringResource(R.string.upload_retry),
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = PlasticColors.OnRubber,
                modifier = Modifier
                    .padding(top = Gap.xs)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.Rubber)
                    .pressable(onClick = onRetry)
                    .padding(horizontal = Gap.m, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun EmptyPlate(onPickPhotos: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Gap.s, vertical = Gap.l),
    ) {
        Text(
            text = stringResource(R.string.upload_empty_title),
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = PlasticColors.OnPlate,
        )
        Text(
            text = stringResource(R.string.upload_empty_hint),
            fontFamily = Pretendard,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = PlasticColors.OnPlateDim,
            modifier = Modifier.padding(top = Gap.xs),
        )
        Text(
            text = stringResource(R.string.upload_empty_pick),
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = PlasticColors.OnRubber,
            modifier = Modifier
                .padding(top = Gap.m)
                .clip(PlasticShapes.Pill)
                .background(PlasticColors.Rubber)
                .pressable(onClick = onPickPhotos)
                .padding(horizontal = Gap.l, vertical = Gap.s),
        )
    }
}

/**
 * 아래 조작부 — 왼쪽 취소(고무 알약), 오른쪽 올리기(빨간 A 버튼).
 *
 * 올리기 버튼의 글자는 **↑ 하나**입니다. 지도·달력의 ＋ 와 같은 자리·같은 크기라
 * 손이 이미 아는 버튼이고, 여기서는 "올린다" 는 뜻만 바꿔 답니다.
 * 올리는 중에는 글자로 바뀝니다 — 그때는 무슨 일이 일어나는지가 더 중요합니다.
 */
@Composable
private fun Controls(
    uploadLabel: String?,
    canUpload: Boolean,
    onCancel: () -> Unit,
    onUpload: () -> Unit,
) {
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
                    .pressable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uploadLabel ?: stringResource(R.string.upload_cancel),
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
                    .background(if (canUpload) PlasticColors.Red else PlasticColors.ButtonOff)
                    .pressable(enabled = canUpload, onClick = onUpload),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "↑",
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = if (canUpload) PlasticColors.OnRed else PlasticColors.OnButtonOff,
                )
            }
        }
    }
}

/**
 * 지역 고르기. 카트리지 슬롯에 이름을 넣고, 나온 것을 화면 안에서 고릅니다.
 *
 * **[ColumnScope] 확장입니다** — 부르는 쪽 Column 의 남는 세로를 `weight` 로 받아야
 * 결과 목록이 화면을 채웁니다. 안에서 Column 을 새로 열면 그 높이를 알 수 없습니다.
 */
@Composable
private fun ColumnScope.PlasticRegionPicker(state: UploadState, onIntent: (UploadIntent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gap.xs, vertical = Gap.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap.s),
    ) {
        Box(
            Modifier
                .size(PlasticSize.MonthNav)
                .clip(PlasticShapes.Knob)
                .background(PlasticColors.Rubber)
                .pressable { onIntent(UploadIntent.Dismissed) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "‹",
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = PlasticColors.OnRubber,
            )
        }
        Text(
            text = stringResource(R.string.upload_region_title),
            fontFamily = Pretendard,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = PlasticColors.Ink,
        )
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = Gap.s)
            .sunken(PlasticShapes.Chip, face = PlasticColors.PlateLo)
            .padding(horizontal = Gap.m, vertical = 11.dp),
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
            if (state.regionQuery.isEmpty()) {
                Text(stringResource(R.string.upload_region_placeholder), style = pickerStyle, color = PlasticColors.OnPlateDim)
            }
            BasicTextField(
                value = state.regionQuery,
                onValueChange = { onIntent(UploadIntent.RegionQueryTyped(it)) },
                singleLine = true,
                textStyle = pickerStyle.copy(color = PlasticColors.OnPlate),
                cursorBrush = SolidColor(PlasticColors.Red),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // 지역 고르기도 같습니다 — 나온 만큼만 자라다가 한도에서 멈추고 구릅니다.
    LazyColumn(
        Modifier
            .heightIn(max = PlasticSize.UploadList)
            .fillMaxWidth()
            .sunken(PlasticShapes.Screen)
    ) {
        items(state.regionResults, key = { it.code.value }) { region ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onIntent(UploadIntent.RegionChosen(region)) }
                    .padding(horizontal = Gap.m, vertical = 13.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Gap.s),
            ) {
                Text(region.name, style = pickerStyle, color = PlasticColors.OnPlate)
                region.parentName?.let {
                    Text(
                        text = it,
                        fontFamily = Pretendard,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = PlasticColors.OnPlateDim,
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(PlasticColors.PlateLo))
        }
    }

    Spacer(Modifier.height(Gap.m))
}

private val pickerStyle = TextStyle(
    fontFamily = Pretendard,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
)
