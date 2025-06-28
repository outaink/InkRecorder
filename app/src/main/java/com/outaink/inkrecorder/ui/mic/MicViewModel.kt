package com.outaink.inkrecorder.ui.mic

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.outaink.inkrecorder.ui.PermissionAction
import com.outaink.inkrecorder.ui.PermissionState
import com.outaink.inkrecorder.ui.PermissionStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UiEvent {

}

@HiltViewModel
class MicViewModel @Inject constructor(
    private val micStateMachine: MicStateMachine,
    private val permissionStateMachine: PermissionStateMachine,
) : ViewModel() {

    companion object {
        const val TAG = "MicViewModel"
    }

    val uiState: StateFlow<MicUiState> = micStateMachine.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MicUiState.Initial()
    )

    private val _uiEventChannel = Channel<UiEvent>()
    val uiEvents = _uiEventChannel.receiveAsFlow()

    val permissionUiState: StateFlow<PermissionState> = permissionStateMachine.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PermissionState.Initial
    )

    fun dispatch(action: PermissionAction) {
        Log.d(TAG, "Action ${action.javaClass.name} dispatched.")
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