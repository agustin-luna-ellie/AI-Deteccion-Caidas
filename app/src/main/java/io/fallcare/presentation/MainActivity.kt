package io.fallcare.presentation


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.fallcare.core.FallDetectionService
import io.fallcare.presentation.ui.FallDetectionScreen
import io.fallcare.presentation.ui.PermissionDeniedScreen
import io.fallcare.util.Constants.FALL_DETECTED_DATA
import io.fallcare.util.appTimeStamp
import io.fallcare.util.logger
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {

    private val tag = this::class.java.simpleName

    private val viewModel: FallDetectionViewModel by viewModels()
    private var fallDetectionService: FallDetectionService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as FallDetectionService.LocalBinder
            fallDetectionService = binder.getService()
            isBound = true

            // Observar cambios en el servicio
            fallDetectionService?.sensorData?.observe(this@MainActivity) { (x, y, z) ->
                viewModel.updateSensorValues(x, y, z)
            }

            fallDetectionService?.statusMessage?.observe(this@MainActivity) {
                viewModel.updateStatusMessage(it)
            }

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            fallDetectionService = null
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent.extras?.getBoolean(FALL_DETECTED_DATA)
        logger("MainActivity", "appTimeStamp: $appTimeStamp, FALL_DETECTED_DATA: $data")

        setContent {
            MaterialTheme {
                val permissionsState = rememberMultiplePermissionsState(
                    permissions = viewModel.getNormalPermissions(this@MainActivity).keys.toList()
                )

                val lifecycleOwner = LocalLifecycleOwner.current
                val permissionState by viewModel.permissionState.observeAsState()

                LaunchedEffect(permissionsState) {
                    if (permissionsState.allPermissionsGranted) {
                        viewModel.evaluatePermissionResults(
                            permissionsState.permissions.associate {
                                it.permission to it.status.isGranted
                            }
                        )
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.evaluatePermissionResults(
                                permissionsState.permissions.associate {
                                    it.permission to it.status.isGranted
                                }
                            )
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                when (permissionState) {
                    FallDetectionViewModel.PermissionState.GRANTED -> {
                        FallDetectionScreen(
                            viewModel = viewModel,
                            context = LocalContext.current
                        )
                        LaunchedEffect(Unit) {
                            startServices()
                        }
                    }

                    FallDetectionViewModel.PermissionState.DENIED -> {
                        PermissionDeniedScreen(
                            context = LocalContext.current,
                            onRetry = { permissionsState.launchMultiplePermissionRequest() }
                        )
                    }

                    else -> {
                        SideEffect {
                            if (!permissionsState.allPermissionsGranted &&
                                !permissionsState.shouldShowRationale
                            ) {
                                permissionsState.launchMultiplePermissionRequest()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showFallAlert() {
        // Mostrar alerta de caída
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun startServices() {
        // Iniciar y vincular el servicio
        FallDetectionService.startService(this@MainActivity)
        bindService(
            Intent(this@MainActivity, FallDetectionService::class.java),
            connection,
            Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
        )
    }

}
