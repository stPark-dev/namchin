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

class ReferencesViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ReferenceRepository.get(app)

    /** null = 아직 로딩 중. */
    val references: StateFlow<List<Reference>?> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
