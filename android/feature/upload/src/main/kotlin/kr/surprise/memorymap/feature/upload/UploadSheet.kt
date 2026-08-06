package kr.surprise.memorymap.feature.upload

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PhotoThumb
import kr.surprise.memorymap.core.designsystem.component.PrimaryButton
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.PLASTIC_TRIAL
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap

/**
 * 사진 올리기. 두 탭에서 같은 시트를 엽니다.
 *
 * 사진마다 어디·언제를 보고 고치는 일이라 **거의 다 펴진 시트**로 뜹니다.
 * 높이는 부르는 쪽이 정합니다 — 시트 안에서는 스스로 화면을 다 쓸 수 없습니다.
 */
@Composable
fun UploadSheet(
    state: UploadState,
    onIntent: (UploadIntent) -> Unit,
    onPickPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 패미컴 스타일 시험 중에는 몸통 위에 화면을 끼우고 조작을 화면 밖으로 냅니다.
    // 스위치는 designsystem 에 하나뿐입니다.
    if (PLASTIC_TRIAL) {
        Box(modifier.fillMaxWidth()) { PlasticUploadBody(state, onIntent, onPickPhotos) }
        return
    }

    Column(modifier.fillMaxWidth().background(MemoryColors.Paper)) {
        if (state.editingRegionOf != null) {
            RegionPicker(state, onIntent)
            return@Column
        }

        Header(count = state.items.size)
        Divider()

        val failure = state.step as? UploadStep.Failed
        if (failure != null) {
            FailureCard(savedLocally = failure.savedLocally) { onIntent(UploadIntent.RetryTapped) }
        }

        if (state.items.isEmpty()) {
            EmptyPick(onPickPhotos)
        } else {
            AutoNotice()
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = Gap.xl, end = Gap.xl, top = Gap.xs, bottom = Gap.s),
            ) {
                items(state.items, key = { it.uri }) { item ->
                    ItemRow(item, onIntent)
                }
            }

            Divider(inset = false)
            Column(Modifier.padding(start = Gap.xl, end = Gap.xl, top = Gap.m, bottom = Gap.xxl)) {
                state.splitNotice()?.let {
                    Text(
                        it,
                        style = MemoryType.Micro,
                        color = MemoryColors.Ink2,
                        modifier = Modifier.padding(bottom = Gap.s),
                    )
                }
                PrimaryButton(
                    text = when (state.step) {
                        UploadStep.Uploading -> "올리는 중…"
                        UploadStep.Reading -> "사진 읽는 중…"
                        else -> "${state.items.size}장 올리기"
                    },
                    enabled = state.canUpload(),
                    onClick = { onIntent(UploadIntent.Confirmed) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 시트라 닫기 버튼을 두지 않습니다 — 끌어 내리거나 뒤를 눌러 닫습니다. */
@Composable
private fun Header(count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = Gap.xl, end = Gap.xl, top = Gap.m, bottom = Gap.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("사진 올리기", style = MemoryType.Title, modifier = Modifier.weight(1f))
        if (count > 0) {
            Text(
                text = "${count}장",
                style = MemoryType.Micro,
                color = MemoryColors.Ink,
                modifier = Modifier
                    .background(MemoryColors.Surface)
                    .border(MemoryStroke.Border, MemoryColors.Line)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
    }
}

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

/** 자동으로 채웠다는 것과 **고칠 수 있다는 것**을 같이 알립니다. */
@Composable
private fun AutoNotice() {
    Row(
        Modifier.fillMaxWidth().padding(start = Gap.xl, end = Gap.xl, top = Gap.m, bottom = Gap.xs),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("ⓘ", style = MemoryType.Micro, color = MemoryColors.Ink2)
        Text(
            "지역·날짜는 사진에서 자동으로 읽었어요. 눌러서 고치면 '자동' 표시가 사라져요.",
            style = MemoryType.Micro,
            color = MemoryColors.Ink2,
        )
    }
}

/**
 * 못 올렸을 때. **왜 안 됐는지와 사진이 어디 있는지**를 같이 말합니다 —
 * 실패만 알리면 사용자는 사진을 잃었다고 생각합니다.
 */
@Composable
private fun FailureCard(savedLocally: Boolean, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = Gap.xl, end = Gap.xl, top = Gap.m)
            .background(MemoryColors.Surface)
            .border(MemoryStroke.Border, MemoryColors.Accent)
            .padding(horizontal = 14.dp, vertical = Gap.m),
    ) {
        Text("지금은 올릴 수 없어요", style = MemoryType.Body, color = MemoryColors.AccentDeep)
        Text(
            text = if (savedLocally) {
                "사진은 폰에 저장해 뒀어요. 연결되면 여기서 다시 시도해 주세요."
            } else {
                "잠시 뒤에 다시 시도해 주세요."
            },
            style = MemoryType.Label,
            color = MemoryColors.AccentDeep,
            modifier = Modifier.padding(top = 3.dp),
        )
        Text(
            text = "다시 시도",
            style = MemoryType.Micro,
            color = MemoryColors.AccentDeep,
            modifier = Modifier
                .padding(top = Gap.s)
                .background(MemoryColors.Surface)
                .border(MemoryStroke.Border, MemoryColors.AccentDeep)
                .clickable(onClick = onRetry)
                .padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun EmptyPick(onPickPhotos: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = Gap.xxxl),
    ) {
        Text("올릴 사진을 골라 주세요", style = MemoryType.Title)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(text = "사진 고르기", onClick = onPickPhotos, modifier = Modifier.fillMaxWidth())
    }
}

/** 사진 한 장 — 왼쪽에 그림, 오른쪽에 '어디' 와 '언제' 두 줄. */
@Composable
private fun ItemRow(item: UploadItem, onIntent: (UploadIntent) -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(Gap.m),
        ) {
            PhotoThumb(url = item.uri, modifier = Modifier.size(62.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldRow(
                    label = "어디",
                    value = item.region?.displayName ?: "고르기",
                    dimmed = item.region == null,
                    auto = item.regionAuto,
                    onClick = { onIntent(UploadIntent.RegionFieldTapped(item.uri)) },
                )
                FieldRow(
                    label = "언제",
                    value = "${item.takenOn.monthValue}월 ${item.takenOn.dayOfMonth}일",
                    dimmed = false,
                    auto = item.dateAuto,
                    onClick = { onIntent(UploadIntent.DateFieldTapped(item.uri)) },
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(MemoryStroke.Border).background(MemoryColors.Fill))
    }
}

/**
 * '어디'·'언제' 한 줄. **네모 상자입니다** — 눌러서 고치는 칸이라 눌릴 수 있게
 * 생겨야 합니다. 예전에는 밑줄만 있는 줄이었는데, 사진마다 두 줄씩 쌓이니
 * 어디까지가 한 사진인지 알 수 없었습니다.
 */
@Composable
private fun FieldRow(
    label: String,
    value: String,
    dimmed: Boolean,
    auto: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MemoryColors.Surface)
            .border(MemoryStroke.Border, MemoryColors.Line)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Gap.s),
    ) {
        Text(label, style = MemoryType.Micro, color = MemoryColors.Ink2, modifier = Modifier.width(26.dp))
        Text(
            text = value,
            style = MemoryType.Body,
            color = if (dimmed) MemoryColors.Ink3 else MemoryColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (auto) {
            Text(
                "자동",
                style = MemoryType.Micro,
                color = MemoryColors.Ink2,
                modifier = Modifier.background(MemoryColors.Fill).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Icon(
            MemoryIcons.ChevronRight,
            contentDescription = null,
            tint = MemoryColors.Ink3,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun RegionPicker(state: UploadState, onIntent: (UploadIntent) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 2.dp, end = Gap.l, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clickable { onIntent(UploadIntent.Dismissed) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(MemoryIcons.Back, contentDescription = "뒤로", tint = MemoryColors.Ink, modifier = Modifier.size(18.dp))
            }
            Text("어디에서 찍었나요", style = MemoryType.Title)
        }
        Divider()

        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Gap.xl, vertical = Gap.m)
                .background(MemoryColors.Surface)
                .border(MemoryStroke.Border, MemoryColors.Line)
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            if (state.regionQuery.isEmpty()) {
                Text("지역 검색 — 강릉, 제주…", style = MemoryType.Body, color = MemoryColors.Ink3)
            }
            BasicTextField(
                value = state.regionQuery,
                onValueChange = { onIntent(UploadIntent.RegionQueryTyped(it)) },
                singleLine = true,
                textStyle = MemoryType.Body.copy(color = MemoryColors.Ink),
                cursorBrush = SolidColor(MemoryColors.Accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        LazyColumn(Modifier.weight(1f).padding(horizontal = Gap.xl)) {
            items(state.regionResults, key = { it.code.value }) { region ->
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onIntent(UploadIntent.RegionChosen(region)) }
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(Gap.s),
                    ) {
                        Text(region.name, style = MemoryType.Body)
                        region.parentName?.let {
                            Text(it, style = MemoryType.Micro, color = MemoryColors.Ink2)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(MemoryStroke.Border).background(MemoryColors.Fill))
                }
            }
        }
    }
}
