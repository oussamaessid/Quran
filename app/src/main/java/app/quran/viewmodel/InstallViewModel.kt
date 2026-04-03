package app.quran.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quran.data.DataInstallManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class InstallState {
    object Checking         : InstallState()
    object AlreadyInstalled : InstallState()
    data class Downloading(
        val done    : Int,
        val total   : Int,
        val label   : String,
        val progress: Float = done.toFloat() / total.coerceAtLeast(1)
    ) : InstallState()
    data class Error(val message: String) : InstallState()
    object Done : InstallState()
}

class InstallViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<InstallState>(InstallState.Checking)
    val state: StateFlow<InstallState> = _state.asStateFlow()

    init { checkAndInstall() }

    fun retry() { checkAndInstall() }

    private fun checkAndInstall() {
        viewModelScope.launch {
            _state.value = InstallState.Checking

            if (DataInstallManager.isInstalled(getApplication())) {
                _state.value = InstallState.AlreadyInstalled
                return@launch
            }

            _state.value = InstallState.Downloading(0, DataInstallManager.TOTAL_FILES, "Préparation…")

            val success = DataInstallManager.install(
                context    = getApplication(),
                concurrency = 16
            ) { done, label ->
                _state.value = InstallState.Downloading(
                    done  = done,
                    total = DataInstallManager.TOTAL_FILES,
                    label = label
                )
            }

            _state.value = if (success) InstallState.Done
            else InstallState.Error("Échec du téléchargement.\nVérifiez votre connexion et réessayez.")
        }
    }
}