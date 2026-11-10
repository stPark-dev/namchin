package dev.stpark.namchin.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.data.ReferenceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import dev.stpark.namchin.ml.SceneEmbedder
import kotlinx.coroutines.Dispatchers

class ReferencesViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ReferenceRepository.get(app)

    /** null = 아직 로딩 중. */
    val references: StateFlow<List<Reference>?> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    init {
        // 이전 버전에서 저장한 레퍼런스의 장면 임베딩을 채운다 (비슷한 배경 카드 추천용)
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { repository.backfillEmbeddings { SceneEmbedder.get(app).embed(it) } }
                .onSuccess { if (it > 0) Log.i(TAG, "임베딩 채움: ${it}장") }
                .onFailure { Log.w(TAG, "임베딩 채우기 실패", it) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    private companion object {
        const val TAG = "ReferencesViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
