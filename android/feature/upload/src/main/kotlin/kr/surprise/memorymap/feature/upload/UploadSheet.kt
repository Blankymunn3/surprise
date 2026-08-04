package kr.surprise.memorymap.feature.upload

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PhotoThumb
import kr.surprise.memorymap.core.designsystem.component.PrimaryButton
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.Space as Gap

/**
 * 사진 올리기. 두 탭에서 같은 화면을 엽니다.
 *
 * 입력칸을 네모 상자가 아니라 **줄**로 둔 이유: 상자가 둘 이상 쌓이면 화면이
 * 서류처럼 보이고, 값이 이미 채워져 있는 화면에서는 더 그렇습니다.
 */
@Composable
fun UploadSheet(
    state: UploadState,
    onIntent: (UploadIntent) -> Unit,
    onPickPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MemoryShapes.Sheet)
            .background(MemoryColors.Surface)
            .padding(start = Gap.xl, end = Gap.xl, top = 10.dp, bottom = 30.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(MemoryShapes.Pill)
                .background(MemoryColors.Line2)
        )
        Spacer(Modifier.height(Gap.l))

        if (state.pickingRegion) {
            RegionPicker(state, onIntent)
            return@Column
        }

        Text("사진 올리기", style = MemoryType.Title)
        Text(
            if (state.picked.isEmpty()) "사진을 골라 주세요" else "${state.picked.size}장 선택됨",
            style = MemoryType.Label,
            color = MemoryColors.Ink3,
            modifier = Modifier.padding(top = 2.dp, bottom = Gap.l),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(Gap.s)) {
            items(state.picked, key = { it.uri }) { picked ->
                PhotoThumb(url = picked.uri, modifier = Modifier.size(86.dp))
            }
            item {
                Box(
                    Modifier
                        .size(86.dp)
                        .clip(MemoryShapes.Thumb)
                        .background(MemoryColors.Fill)
                        .clickable(onClick = onPickPhotos),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MemoryIcons.Plus, contentDescription = "사진 더 고르기", tint = MemoryColors.Ink3)
                }
            }
        }

        Spacer(Modifier.height(Gap.l))

        FieldRow(
            label = "지역",
            value = state.region?.displayName ?: "고르기",
            auto = state.regionFromExif,
            dimmed = state.region == null,
            onClick = { onIntent(UploadIntent.RegionFieldTapped) },
        )
        FieldRow(
            label = "날짜",
            value = state.takenOn?.let { "${it.year}. ${it.monthValue}. ${it.dayOfMonth}." } ?: "오늘",
            auto = state.dateFromExif,
            dimmed = false,
            onClick = { },
        )

        val notice = state.mismatchNotice()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (state.regionFromExif || state.dateFromExif || notice != null) {
                Icon(MemoryIcons.Sparkle, contentDescription = null, tint = MemoryColors.Accent, modifier = Modifier.size(15.dp))
                Text(
                    notice ?: "사진에서 날짜와 지역을 찾았어요",
                    style = MemoryType.Label,
                    color = MemoryColors.Ink3,
                )
            }
        }

        PrimaryButton(
            text = when (state.step) {
                UploadStep.Uploading -> "올리는 중…"
                UploadStep.Reading -> "사진 읽는 중…"
                else -> "${state.picked.size}장 올리기"
            },
            enabled = state.canUpload(),
            onClick = { onIntent(UploadIntent.Confirmed) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    auto: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MemoryColors.Line))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Gap.m),
        ) {
            Text(label, style = MemoryType.Body, color = MemoryColors.Ink2, modifier = Modifier.size(width = 44.dp, height = 22.dp))
            Text(value, style = MemoryType.Body, color = if (dimmed) MemoryColors.Ink3 else MemoryColors.Ink)

            if (auto) {
                Box(
                    Modifier
                        .clip(MemoryShapes.Pill)
                        .background(MemoryColors.Fill)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("자동", style = MemoryType.Micro, color = MemoryColors.Accent)
                }
            }

            Spacer(Modifier.weight(1f))
            Icon(MemoryIcons.ChevronRight, contentDescription = null, tint = MemoryColors.Ink3, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RegionPicker(state: UploadState, onIntent: (UploadIntent) -> Unit) {
    Text("지역 고르기", style = MemoryType.Title)
    Spacer(Modifier.height(Gap.m))

    Box(
        Modifier
            .fillMaxWidth()
            .clip(MemoryShapes.Button)
            .background(MemoryColors.Fill)
            .padding(horizontal = Gap.l, vertical = 14.dp),
    ) {
        if (state.regionQuery.isEmpty()) {
            Text("지역 검색", style = MemoryType.Body, color = MemoryColors.Ink3)
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

    LazyColumn(Modifier.fillMaxWidth().height(280.dp)) {
        items(state.regionResults, key = { it.code.value }) { region ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onIntent(UploadIntent.RegionChosen(region)) }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(region.name, style = MemoryType.Body, modifier = Modifier.weight(1f))
                region.parentName?.let { Text(it, style = MemoryType.Label, color = MemoryColors.Ink3) }
            }
        }
    }
}
