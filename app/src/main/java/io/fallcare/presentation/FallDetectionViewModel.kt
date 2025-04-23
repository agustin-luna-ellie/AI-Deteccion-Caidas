package io.fallcare.presentation

import android.Manifest.permission.ACTIVITY_RECOGNITION
import android.Manifest.permission.BODY_SENSORS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WAKE_LOCK
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


class FallDetectionViewModel(application: Application) :
    AndroidViewModel(application) {

    private val _x = MutableLiveData(0f)
    val x: LiveData<Float> = _x

    private val _y = MutableLiveData(0f)
    val y: LiveData<Float> = _y

    private val _z = MutableLiveData(0f)
    val z: LiveData<Float> = _z

    private val _fallDetected = MutableLiveData(false)
    val fallDetected: LiveData<Boolean> = _fallDetected

    private val _statusMessage = MutableLiveData("")
    val statusMessage: LiveData<String> = _statusMessage

    private val _permissionState = MutableLiveData(PermissionState.IDLE)
    val permissionState: LiveData<PermissionState> = _permissionState

    fun updateSensorValues(x: Float, y: Float, z: Float) {
        _x.value = x
        _y.value = y
        _z.value = z
    }

    fun updateStatusMessage(statusMessage: String) {
        _statusMessage.value = statusMessage
    }

    fun evaluatePermissionResults(results: Map<String, Boolean>) {
        val allGranted = !results.values.contains(false)
        _permissionState.value = if (allGranted)
            PermissionState.GRANTED
        else
            PermissionState.DENIED

    }

    fun getNormalPermissions(context: Context): Map<String, Boolean> {
        val permissions = mutableMapOf<String, Boolean>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            permissions[POST_NOTIFICATIONS] =
                ContextCompat.checkSelfPermission(
                    context,
                    POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        permissions[ACTIVITY_RECOGNITION] =
            ContextCompat.checkSelfPermission(
                context,
                ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

        permissions[BODY_SENSORS] =
            ContextCompat.checkSelfPermission(
                context,
                BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED

        permissions[WAKE_LOCK] =
            ContextCompat.checkSelfPermission(
                context,
                WAKE_LOCK
            ) == PackageManager.PERMISSION_GRANTED

        return permissions
    }

    enum class PermissionState {
        IDLE, GRANTED, DENIED
    }

}
