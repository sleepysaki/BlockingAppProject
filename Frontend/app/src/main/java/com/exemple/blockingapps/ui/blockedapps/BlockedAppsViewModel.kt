package com.exemple.blockingapps.ui.blockedapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.common.BlockState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


data class InstalledApp(
    val packageName: String,
    val name: String,
    val category: String = "Other"
)


data class BlockedAppsUiState(
    val installedApps: List<InstalledApp> = emptyList(),
    val blockedPackages: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BlockedAppsViewModel : ViewModel() {

    private val _state = MutableStateFlow(BlockedAppsUiState(isLoading = true))
    val state: StateFlow<BlockedAppsUiState> = _state

    private val blockedSet = mutableSetOf<String>()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            delay(300)
            val apps = listOf(
                InstalledApp("com.google.android.youtube", "YouTube", "Video"),
                InstalledApp("com.zhiliaoapp.musically", "TikTok", "Social"),
                InstalledApp("com.facebook.katana", "Facebook", "Social"),
                InstalledApp("com.whatsapp", "WhatsApp", "Communication"),
                InstalledApp("com.android.chrome", "Chrome", "Browser"),
                InstalledApp("com.minecraft", "Minecraft", "Game")
            )
            _state.value = BlockedAppsUiState(
                installedApps = apps,
                blockedPackages = blockedSet.toSet(),
                isLoading = false,
                error = null
            )
        }
    }


    fun toggleBlock(packageName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            delay(150) // simulate small work

            if (blockedSet.contains(packageName)) {
                blockedSet.remove(packageName)
            } else {
                blockedSet.add(packageName)
            }

            _state.value = _state.value.copy(
                installedApps = _state.value.installedApps,
                blockedPackages = blockedSet.toSet(),
                isLoading = false,
                error = null
            )


            try {
                BlockState.blockedPackages = blockedSet.toSet()
            } catch (t: Throwable) {
            }
        }
    }
}
