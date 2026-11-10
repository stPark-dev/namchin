package dev.stpark.namchin.ui.reference

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.stpark.namchin.R
import dev.stpark.namchin.core.tagging.Tag
import dev.stpark.namchin.core.tagging.TagEdit
import dev.stpark.namchin.core.tagging.TagEdit.ChipState
import dev.stpark.namchin.core.tagging.TagVocabulary
import dev.stpark.namchin.ui.theme.Brand

/**
 * 분류별 태그 칩.
 * - 노랑(켜짐): 이 사진의 태그
 * - ✨ 테두리(제안): 비슷한 사진에게서 배운 장소·계절. 칩을 누르거나 "이대로 확정"으로만 받아들인다
 * 칩 하나를 누르면 그 칩만 반영해 확정한다 (검토하지 않은 제안이 번지지 않게).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditor(
    referenceId: Long,
    own: Set<Tag>,
    learned: Set<Tag>,
    confirmed: Boolean,
    onConfirm: (Set<Tag>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 빠르게 연달아 눌러도 앞 선택이 사라지지 않게, 저장 반영 전까지 화면 상태를 기준으로 계산
    var current by remember(referenceId, own) { mutableStateOf(own) }
    val states = TagEdit.states(current, if (confirmed) emptySet() else learned)
    fun confirm(tags: Set<Tag>) {
        current = tags
        onConfirm(tags)
    }

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tags_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (confirmed) {
                    Text(stringResource(R.string.tags_confirmed), style = MaterialTheme.typography.labelMedium)
                } else {
                    TextButton(onClick = { confirm(TagEdit.confirmAll(current, learned)) }) {
                        Text(stringResource(R.string.tags_confirm))
                    }
                }
            }
            if (!confirmed) Text(stringResource(R.string.tags_suggested), style = MaterialTheme.typography.bodySmall)
            TagVocabulary.CATEGORIES.forEach { category ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        category.tags.forEach { tag ->
                            val state = states.getValue(tag)
                            FilterChip(
                                selected = state == ChipState.ON,
                                onClick = { confirm(TagEdit.toggle(current, tag)) },
                                label = { Text(tag.name) },
                                leadingIcon = if (state == ChipState.SUGGESTED) {
                                    { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp), tint = Brand.YellowDeep) }
                                } else {
                                    null
                                },
                                border = if (state == ChipState.SUGGESTED) BorderStroke(1.5.dp, Brand.YellowDeep) else FilterChipDefaults.filterChipBorder(true, state == ChipState.ON),
                            )
                        }
                    }
                }
            }
        }
    }
}
