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
import android.graphics.Bitmap
import dev.stpark.namchin.core.color.ColorLut
import dev.stpark.namchin.image.BitmapLoader
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface ReviewState {
    data object Hidden : ReviewState
    data object Analyzing : ReviewState

    /** @property items 찍은 순서. id는 사진 주소(Uri 문자열) */
    data class Ready(val items: List<ReviewItem>, val reference: Reference?) : ReviewState
}

/** 색감(레퍼런스 색감 또는 필터)을 입힌 미리보기. */
data class ColorPreview(val shotId: String, val look: Look, val strength: Float, val bitmap: Bitmap)

/** 연속 촬영 결과에서 베스트 컷을 고르고, 원하면 고른 한 장만 남긴다. 고른 필터는 남는 사진에 입힌다. */
class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<ReviewState>(ReviewState.Hidden)
    val state: StateFlow<ReviewState> = _state.asStateFlow()
    private var job: Job? = null
    /** 마치기는 한 번에 하나씩 (같은 사진을 동시에 덮어쓰지 않게) */
    private val finishing = Mutex()

    private val _color = MutableStateFlow<ColorPreview?>(null)
    val color: StateFlow<ColorPreview?> = _color.asStateFlow()
    private var colorJob: Job? = null

    /**
     * 색 변환표(Look.tableKey별)와 미리보기 바탕 (같은 사진에서 슬라이더만 움직이면 다시 만들지 않음).
     * 메인 스레드에서만 만지고, 무거운 계산은 Deferred로 백그라운드에서 한 번만 한다 (미리보기와 저장이 공유).
     */
    private val luts = mutableMapOf<Any, Deferred<ColorLut>>()
    private val previewBases = mutableMapOf<String, Deferred<Bitmap>>()

    /** 선택한 사진에 색감을 입힌 미리보기. 연달아 불리면 마지막 요청만 반영한다. */
    fun previewColor(shotId: String, look: Look, strength: Float) {
        val ready = _state.value as? ReviewState.Ready ?: return
        if (look.needsReference && ready.reference == null) return
        colorJob?.cancel()
        colorJob = viewModelScope.launch {
            try {
                val lut = lut(ready.reference, shotId, look)
                val base = cached(previewBases, shotId) {
                    BitmapLoader.load(getApplication(), Uri.parse(shotId), PREVIEW_DIMENSION)
                }
                val bitmap = withContext(Dispatchers.Default) { ColorMatcher.applied(base, lut, strength) }
                // 메인으로 돌아온 뒤 확인하므로, 그사이 새 요청이 왔다면 옛 결과는 버려진다
                ensureActive()
                _color.value = ColorPreview(shotId, look, strength, bitmap)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "색감 미리보기 실패", e)
            }
        }
    }

    fun clearColor() {
        colorJob?.cancel()
        _color.value = null
    }

    /** @param reference look이 레퍼런스 색감이면 null이 아니어야 한다 */
    private suspend fun lut(reference: Reference?, shotId: String, look: Look): ColorLut =
        cached(luts, look.tableKey(shotId)) { buildLut(reference, shotId, look) }

    private fun buildLut(reference: Reference?, shotId: String, look: Look): ColorLut = when (look) {
        is Look.Reference -> ColorMatcher.lut(getApplication(), requireNotNull(reference), Uri.parse(shotId), look.keepLightness)
        is Look.Preset -> look.filter.lut()
    }

    /** 메인에서 캐시를 찾고, 없으면 백그라운드 계산을 하나만 띄워 공유한다. 실패한 계산은 캐시에서 뺀다. */
    private suspend fun <K, V> cached(cache: MutableMap<K, Deferred<V>>, key: K, compute: () -> V): V {
        val deferred = cache.getOrPut(key) { viewModelScope.async(Dispatchers.Default) { compute() } }
        return try {
            deferred.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (cache[key] === deferred) cache.remove(key)
            throw e
        }
    }

    private fun clearColorCache() {
        clearColor()
        // 화면을 닫았으면 아직 계산 중인 표·바탕도 멈춘다
        luts.values.forEach { it.cancel() }
        previewBases.values.forEach { it.cancel() }
        luts.clear()
        previewBases.clear()
    }

    fun review(uris: List<Uri>, reference: Reference?) {
        job?.cancel()
        clearColorCache()
        // 한 장이면 고를 게 없으니 분석 없이 바로 (필터로 마무리만)
        if (uris.size == 1) {
            _state.value = ReviewState.Ready(
                listOf(ReviewItem(uris[0].toString(), score = null, best = true, eyesClosed = false)),
                reference,
            )
            return
        }
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
     * 마친 뒤 알림.
     * @property recolored 필터를 입힌 장 수
     * @property originalLost 덮어쓰다 실패했는데 원본도 되돌리지 못한 장 수
     */
    data class FinishResult(val deleteFailed: Int, val recolorFailed: Int, val recolored: Int, val originalLost: Int)

    /**
     * 결과 화면 마치기: keepId가 있으면 그 한 장만 남기고 나머지는 갤러리에서 지운다 (앱이 만든 사진이라 권한 없이 가능).
     * look을 골랐으면 남는 사진에 필터를 입혀 원본 자리에 덮어쓴다.
     * 화면은 바로 닫고, 앞의 마치기가 아직 돌고 있으면 그 뒤에 차례로 한다. 할 일이 없으면 알리지 않는다.
     */
    fun finish(keepId: String?, look: Look?, strength: Float, onDone: (FinishResult) -> Unit) {
        val ready = _state.value as? ReviewState.Ready ?: return
        val usable = look?.takeIf { !it.needsReference || ready.reference != null }
        val plan = FinishPlan.of(ready.items.map { it.id }, keepId, usable)
        _state.value = ReviewState.Hidden
        clearColorCache()
        if (plan.isEmpty) return
        viewModelScope.launch {
            // 화면을 떠나도 반쯤 하다 멈추지 않게
            val result = withContext(Dispatchers.IO + NonCancellable) {
                finishing.withLock { carryOut(plan, ready.reference, usable, strength) }
            }
            onDone(result)
        }
    }

    private suspend fun carryOut(plan: FinishPlan, reference: Reference?, look: Look?, strength: Float): FinishResult {
        val app = getApplication<Application>()
        val deleteFailed = plan.delete.count { id ->
            val deleted = runCatching { app.contentResolver.delete(Uri.parse(id), null, null) }
                .onFailure { Log.w(TAG, "삭제 실패: $id", it) }.getOrDefault(0)
            deleted != 1
        }
        val failures = plan.recolor.mapNotNull { id ->
            runCatching {
                ColorMatcher.overwriteColored(app, Uri.parse(id), buildLut(reference, id, requireNotNull(look)), strength)
            }.onFailure { Log.w(TAG, "필터 입히기 실패: $id", it) }.exceptionOrNull()
        }
        return FinishResult(
            deleteFailed = deleteFailed,
            recolorFailed = failures.size,
            recolored = plan.recolor.size - failures.size,
            originalLost = failures.count { it is ColorMatcher.OriginalLostException },
        )
    }

    fun close() {
        job?.cancel()
        clearColorCache()
        _state.value = ReviewState.Hidden
    }

    private companion object {
        const val TAG = "ReviewViewModel"
        const val PREVIEW_DIMENSION = 1440
    }
}
