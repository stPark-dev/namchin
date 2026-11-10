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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ImportUiState {
    data object Loading : ImportUiState
    data class Cropping(val bitmap: Bitmap, val rect: NormRect, val autoDetected: Boolean) : ImportUiState
    data object Analyzing : ImportUiState
    data class Saved(val id: Long) : ImportUiState
    data class Failed(@StringRes val message: Int) : ImportUiState
}

/** 이미지 불러오기 → 자동 크롭 제안 → (사용자 조정) → 포즈 분석 → 저장. */
class ImportViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ReferenceRepository.get(app)
    private val analyzer = ReferencePoseAnalyzer()
    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Loading)
    val state: StateFlow<ImportUiState> = _state.asStateFlow()
    private var loadedUri: Uri? = null

    fun load(uri: Uri) {
        if (uri == loadedUri) return
        loadedUri = uri
        _state.value = ImportUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                withContext(Dispatchers.Default) {
                    val bitmap = BitmapLoader.load(getApplication(), uri)
                    val luma = BitmapLoader.toLuma(bitmap)
                    val detected = ScreenshotCropDetector.detect(luma)
                    ImportUiState.Cropping(
                        bitmap = bitmap,
                        rect = detected?.let { CropGeometry.fromDetector(it, luma.width, luma.height) } ?: FULL,
                        autoDetected = detected != null,
                    )
                }
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
        viewModelScope.launch {
            _state.value = try {
                val bitmap = cropping.bitmap
                val cropped = withContext(Dispatchers.Default) {
                    BitmapLoader.crop(bitmap, CropGeometry.toPixels(cropping.rect, bitmap.width, bitmap.height))
                }
                val pose = analyzer.analyze(cropped)
                ImportUiState.Saved(repository.save(cropped, pose))
            } catch (e: Exception) {
                Log.w(TAG, "레퍼런스 분석/저장 실패", e)
                ImportUiState.Failed(R.string.import_error_analyze)
            }
        }
    }

    override fun onCleared() = analyzer.close()

    private companion object {
        const val TAG = "ImportViewModel"
        val FULL = NormRect(0f, 0f, 1f, 1f)
    }
}
