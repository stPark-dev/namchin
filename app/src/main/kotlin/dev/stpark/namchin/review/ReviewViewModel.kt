package dev.stpark.namchin.review

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.stpark.namchin.core.bestshot.ReviewItem
import dev.stpark.namchin.core.bestshot.ShotReview
import dev.stpark.namchin.data.Reference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ReviewState {
    data object Hidden : ReviewState
    data object Analyzing : ReviewState

    /** @property items 찍은 순서. id는 사진 주소(Uri 문자열) */
    data class Ready(val items: List<ReviewItem>, val reference: Reference?) : ReviewState
}

/** 연속 촬영 결과에서 베스트 컷을 고르고, 원하면 나머지를 갤러리에서 지운다. */
class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val analyzer by lazy { ShotAnalyzer(app) }
    private var analyzerCreated = false
    private val _state = MutableStateFlow<ReviewState>(ReviewState.Hidden)
    val state: StateFlow<ReviewState> = _state.asStateFlow()
    private var job: Job? = null

    fun review(uris: List<Uri>, reference: Reference?) {
        job?.cancel()
        _state.value = ReviewState.Analyzing
        job = viewModelScope.launch {
            _state.value = try {
                analyzerCreated = true
                val shots = uris.map { analyzer.analyze(it, reference?.pose) }
                ReviewState.Ready(ShotReview.of(shots), reference)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "베스트 컷 분석 실패", e)
                // 분석이 실패해도 사진은 보여준다 (점수·표시 없이)
                ReviewState.Ready(uris.mapIndexed { i, u -> ReviewItem(u.toString(), null, i == 0, false) }, reference)
            }
        }
    }

    /** 베스트를 뺀 나머지를 갤러리에서 지운다 (앱이 만든 사진이라 권한 없이 가능). */
    fun keepBestOnly(onDone: () -> Unit) {
        val ready = _state.value as? ReviewState.Ready ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ready.items.filterNot { it.best }.forEach { item ->
                    runCatching { getApplication<Application>().contentResolver.delete(Uri.parse(item.id), null, null) }
                        .onFailure { Log.w(TAG, "삭제 실패: ${item.id}", it) }
                }
            }
            _state.value = ReviewState.Hidden
            onDone()
        }
    }

    fun close() {
        job?.cancel()
        _state.value = ReviewState.Hidden
    }

    override fun onCleared() {
        if (analyzerCreated) analyzer.close()
    }

    private companion object {
        const val TAG = "ReviewViewModel"
    }
}
