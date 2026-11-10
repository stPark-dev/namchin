package dev.stpark.namchin.importer

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.stpark.namchin.R
import dev.stpark.namchin.core.crop.ScreenshotCropDetector
import dev.stpark.namchin.data.ReferenceRepository
import dev.stpark.namchin.image.BitmapLoader
import dev.stpark.namchin.ml.ReferencePoseAnalyzer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Loading : ImportUiState
    data class Cropping(val bitmap: Bitmap, val rect: NormRect, val autoDetected: Boolean) : ImportUiState
    data object Analyzing : ImportUiState
    data class Saved(val id: Long) : ImportUiState
    data class Failed(@StringRes val message: Int) : ImportUiState
}

/**
 * 이미지 불러오기 → 자동 크롭 제안 → (사용자 조정) → 포즈 분석 → 저장.
 * 액티비티당 하나. 가져오기 작업은 token으로 구분하고, 끝나면 reset()으로 비트맵을 놓는다.
 */
class ImportViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ReferenceRepository.get(app)
    private val analyzer by lazy { ReferencePoseAnalyzer() }
    private var analyzerCreated = false
    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state.asStateFlow()
    private var token: String? = null
    private var job: Job? = null

    /** 같은 token이면(재구성·화면 복원) 진행 중인 작업을 유지한다. */
    fun start(token: String, uri: Uri) {
        if (token == this.token) return
        reset()
        this.token = token
        _state.value = ImportUiState.Loading
        job = viewModelScope.launch {
            _state.value = try {
                withContext(Dispatchers.Default) { loadForCropping(uri) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "이미지 불러오기 실패: $uri", e)
                ImportUiState.Failed(R.string.import_error_load)
            }
        }
    }

    fun updateCrop(rect: NormRect) {
        val current = _state.value as? ImportUiState.Cropping ?: return
        _state.value = current.copy(rect = rect)
    }

    fun confirm() {
        val cropping = _state.value as? ImportUiState.Cropping ?: return
        _state.value = ImportUiState.Analyzing
        job = viewModelScope.launch {
            _state.value = try {
                val bitmap = cropping.bitmap
                val cropped = withContext(Dispatchers.Default) {
                    BitmapLoader.crop(bitmap, CropGeometry.toPixels(cropping.rect, bitmap.width, bitmap.height))
                }
                analyzerCreated = true
                val pose = analyzer.analyze(cropped)
                ImportUiState.Saved(repository.save(cropped, pose))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "레퍼런스 분석/저장 실패", e)
                ImportUiState.Failed(R.string.import_error_analyze)
            }
        }
    }

    /** 진행 중 작업을 취소하고 비트맵 참조를 놓는다. 저장은 repository 쪽에서 끝까지 완료된다. */
    fun reset() {
        job?.cancel()
        job = null
        token = null
        _state.value = ImportUiState.Idle
    }

    private fun loadForCropping(uri: Uri): ImportUiState.Cropping {
        val bitmap = BitmapLoader.load(getApplication(), uri)
        val luma = BitmapLoader.toLuma(bitmap)
        val detected = ScreenshotCropDetector.detect(luma)
        return ImportUiState.Cropping(
            bitmap = bitmap,
            rect = detected?.let { CropGeometry.fromDetector(it, luma.width, luma.height) } ?: FULL,
            autoDetected = detected != null,
        )
    }

    override fun onCleared() {
        if (analyzerCreated) analyzer.close()
    }

    private companion object {
        const val TAG = "ImportViewModel"
        val FULL = NormRect(0f, 0f, 1f, 1f)
    }
}
