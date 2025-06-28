package com.outaink.inkrecorder.ui

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

sealed interface PermissionState {
    data object Initial : PermissionState
    data object Granted : PermissionState
    data object NotRequested : PermissionState
    data class RationaleNeeded(val rationale: String) : PermissionState
    data object Requesting : PermissionState
    data object PermanentlyDenied : PermissionState
}

sealed interface PermissionAction {
    data class Check(val permission: String) : PermissionAction
    data class Request(val permission: String)  : PermissionAction
    data object UserAcknowledgesRationale : PermissionAction
    data object UserGrants : PermissionAction
    data class UserDenies(val shouldShowRatinale: Boolean) : PermissionAction
}

private const val RATIONALE = "理由"

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionStateMachine @Inject constructor(
    @ApplicationContext private val context: Context
) :
    FlowReduxStateMachine<PermissionState, PermissionAction>(
        initialState = PermissionState.Initial
    ) {

    companion object {
        const val TAG = "PermissionStateMachine"
    }

    private fun checkIfPermissionGranted(permission: String) =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    init {
        spec {
            inState<PermissionState.Initial> {
                Log.d(TAG, "inState<PermissionState.Initial>")
                on<PermissionAction.Check> { action, state ->
                    state.override {
                        if (checkIfPermissionGranted(action.permission)) PermissionState.Granted
                        else PermissionState.NotRequested
                    }
                }
                Log.d(TAG, "on<PermissionAction.Check> end")
            }

            inState<PermissionState.NotRequested> {
                Log.d(TAG, "inState<PermissionState.NotRequested>")
                on<PermissionAction.Request> { action, state ->
                    state.override { PermissionState.Requesting }
                }
                Log.d(TAG, "on<PermissionAction.Request> end")
            }

            inState<PermissionState.Requesting> {
                Log.d(TAG, "inState<PermissionState.Requesting>")
                on<PermissionAction.UserGrants> { action, state ->
                    state.override { PermissionState.Granted }
                }

                on<PermissionAction.UserDenies> { action, state ->
                    if (action.shouldShowRatinale)
                        state.override { PermissionState.RationaleNeeded(rationale = RATIONALE) }
                    else
                        state.override { PermissionState.PermanentlyDenied }
                }
            }

            inState<PermissionState.PermanentlyDenied> {
                Log.d(TAG, "inState<PermissionState.PermanentlyDenied>")
                on<PermissionAction.Check> { action, state ->
                    val hasPermission = checkIfPermissionGranted(action.permission)
                    state.override {
                        if (hasPermission) PermissionState.Granted
                        else PermissionState.PermanentlyDenied
                    }
                }
            }

            inState<PermissionState.Granted> {
                Log.d(TAG, "inState<PermissionState.Granted>")
                on<PermissionAction.Check> { action, state ->
                    val hasPermission = checkIfPermissionGranted(action.permission)

                    state.override {
                        if (hasPermission) PermissionState.Granted
                        else PermissionState.RationaleNeeded(rationale = RATIONALE)
                    }
                }
            }

            inState<PermissionState.RationaleNeeded> {
                Log.d(TAG, "inState<PermissionState.RationaleNeeded>")
                on<PermissionAction.UserAcknowledgesRationale> { _, state ->
                    state.override {
                        PermissionState.Requesting
                    }
                }
            }
        }
    }


}