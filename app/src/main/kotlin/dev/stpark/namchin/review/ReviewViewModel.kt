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

/** 연속 촬영 결과에서 베스트 컷을 고르고, 원하면 고른 한 장만 남기고 나머지를 갤러리에서 지운다. */
class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<ReviewState>(ReviewState.Hidden)
    val state: StateFlow<ReviewState> = _state.asStateFlow()
    private var job: Job? = null
    private var deleteJob: Job? = null

    fun review(uris: List<Uri>, reference: Reference?) {
        job?.cancel()
        _state.value = ReviewState.Analyzing
        job = viewModelScope.launch {
            // 분석 모델은 이번 결과 화면 동안만 메모리에 둔다
            val analyzed = ShotAnalyzer(getApplication()).use { analyzer ->
                uris.mapNotNull { uri ->
                    try {
                        analyzer.analyze(uri, reference?.pose)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // 한 장이 실패해도 나머지는 분석한다. 실패한 장은 베스트 후보에서 빠진다
                        Log.w(TAG, "분석 실패: $uri", e)
                        null
                    }
                }
            }
            _state.value = ReviewState.Ready(ShotReview.merge(uris.map { it.toString() }, analyzed), reference)
        }
    }

    /**
     * keepId 한 장만 남기고 나머지를 갤러리에서 지운다 (앱이 만든 사진이라 권한 없이 가능).
     * 화면은 바로 닫고, 끝나면 지우지 못한 장 수를 알린다.
     */
    fun keepOnly(keepId: String, onDone: (failed: Int) -> Unit) {
        val ready = _state.value as? ReviewState.Ready ?: return
        if (deleteJob?.isActive == true) return
        _state.value = ReviewState.Hidden
        val targets = ready.items.map { it.id }.filter { it != keepId }
        deleteJob = viewModelScope.launch {
            val failed = withContext(Dispatchers.IO) {
                targets.count { id ->
                    val deleted = runCatching {
                        getApplication<Application>().contentResolver.delete(Uri.parse(id), null, null)
                    }.onFailure { Log.w(TAG, "삭제 실패: $id", it) }.getOrDefault(0)
                    deleted != 1
                }
            }
            onDone(failed)
        }
    }

    fun close() {
        job?.cancel()
        _state.value = ReviewState.Hidden
    }

    private companion object {
        const val TAG = "ReviewViewModel"
    }
}
