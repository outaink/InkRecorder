package com.outaink.inkrecorder.ui.mic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.outaink.inkrecorder.ui.PermissionAction
import com.outaink.inkrecorder.ui.PermissionStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MicViewModel @Inject constructor(
    private val micStateMachine: MicStateMachine,
    private val permissionStateMachine: PermissionStateMachine,
) : ViewModel() {

    companion object {
        const val TAG = "RecorderViewModel"
    }

    val uiState: StateFlow<MicUiState> = micStateMachine.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = MicUiState.Initial()
    )

    fun dispatch(action: PermissionAction) {
        viewModelScope.launch {
            permissionStateMachine.dispatch(action)
        }
    }

    fun dispatch(action: MicAction) {
        viewModelScope.launch {
            micStateMachine.dispatch(action)
        }
    }
}