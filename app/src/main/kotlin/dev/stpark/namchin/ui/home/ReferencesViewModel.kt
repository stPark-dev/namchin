package dev.stpark.namchin.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.data.ReferenceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import dev.stpark.namchin.ml.SceneEmbedder
import dev.stpark.namchin.ml.AutoTagger
import dev.stpark.namchin.core.tagging.Tag
import kotlinx.coroutines.Dispatchers

class ReferencesViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ReferenceRepository.get(app)

    /** 레퍼런스별 배운 태그 제안 (목록이 바뀔 때 백그라운드에서 한 번 계산). */
    val learnedTags: StateFlow<Map<Long, Set<Tag>>> = repository.observeAll()
        .map { Reference.learnedTagsById(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyMap())

    /** null = 아직 로딩 중. */
    val references: StateFlow<List<Reference>?> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    init {
        // 이전 버전에서 저장한 레퍼런스의 장면 임베딩을 채운다 (비슷한 배경 카드 추천용)
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { repository.backfillEmbeddings { SceneEmbedder.get(app).embed(it) } }
                .onSuccess { if (it > 0) Log.i(TAG, "임베딩 채움: ${it}장") }
                .onFailure { Log.w(TAG, "임베딩 채우기 실패", it) }
            runCatching { repository.backfillTags(AutoTagger::tags) }
                .onSuccess { if (it > 0) Log.i(TAG, "자동 태그 채움: ${it}장") }
                .onFailure { Log.w(TAG, "자동 태그 채우기 실패", it) }
        }
    }

    /** 사용자가 고른 태그로 확정한다. 이후 비슷한 사진의 태그 제안에 쓰인다. */
    fun confirmTags(id: Long, tags: Set<Tag>) {
        viewModelScope.launch { repository.updateTags(id, tags, confirmed = true) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    private companion object {
        const val TAG = "ReferencesViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
