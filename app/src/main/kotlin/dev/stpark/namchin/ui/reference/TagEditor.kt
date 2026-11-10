package dev.stpark.namchin.ui.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.stpark.namchin.R
import dev.stpark.namchin.core.tagging.Tag
import dev.stpark.namchin.core.tagging.TagVocabulary

/**
 * 분류별 태그 칩. 칩을 누르면 그 상태로 확정되고, 이후 비슷한 사진의 제안에 쓰인다.
 * @param tags 지금 보여줄 태그 (확정 전이면 제안)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditor(tags: Set<Tag>, confirmed: Boolean, onConfirm: (Set<Tag>) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tags_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (confirmed) {
                    Text(stringResource(R.string.tags_confirmed), style = MaterialTheme.typography.labelMedium)
                } else {
                    TextButton(onClick = { onConfirm(tags) }) { Text(stringResource(R.string.tags_confirm)) }
                }
            }
            if (!confirmed) Text(stringResource(R.string.tags_suggested), style = MaterialTheme.typography.bodySmall)
            TagVocabulary.CATEGORIES.forEach { category ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        category.tags.forEach { tag ->
                            val on = tag in tags
                            FilterChip(
                                selected = on,
                                onClick = { onConfirm(if (on) tags - tag else tags + tag) },
                                label = { Text(tag.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}
