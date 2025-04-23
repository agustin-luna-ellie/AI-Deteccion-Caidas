package io.fallcare.core


import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.fallcare.data.FallEntity
import io.fallcare.data.FallModel
import io.fallcare.data.FireStoreRepository
import io.fallcare.util.Constants.ACTION_UPDATE_SETTINGS
import io.fallcare.util.Constants.NOTIFICATION_ID
import io.fallcare.util.Constants.PROB_FALL_KEY
import io.fallcare.util.Constants.SAMPLING_PERIOD_KEY
import io.fallcare.util.Constants.SEQUENCE_LENGTH_KEY
import io.fallcare.util.Constants.SETTINGS_KEY
import io.fallcare.util.androidID
import io.fallcare.util.appTimeStamp
import io.fallcare.util.createForegroundNotification
import io.fallcare.util.loadModelFileMapped
import io.fallcare.util.logger
import io.fallcare.util.verifyModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter


class FallDetectionService : Service(), SensorEventListener {

    private val tag = "FallDetectionService"

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var mHandlerThread = HandlerThread("Accelerometer-Thread")
    private var modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isListening: Boolean = false
    private val binder = LocalBinder()

    // Configuración del sensor
    private var configSequenceLength = 40
    private var configSamplingPeriod = 31
    private var configProbFall = .5f

    private var lastFallTime = 0L
    private var boundClients = 0

    private var bufferList = ArrayList<FallModel>()
    private lateinit var tflite: Interpreter
    private var modelReady = false

    // Comunicación con la Activity

    private val _sensorData = MutableLiveData<Triple<Float, Float, Float>>()
    val sensorData: LiveData<Triple<Float, Float, Float>> = _sensorData

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    inner class LocalBinder : Binder() {
        fun getService(): FallDetectionService = this@FallDetectionService
    }

    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            logger("settingsReceiver", "intent.action: [${intent?.action}]")
            if (intent?.action == ACTION_UPDATE_SETTINGS) {
                loadSettingsFromPreferences()
                reconfigureSensor()
                _statusMessage.postValue("✔ Parámetros actualizados dinámicamente")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val notification = createForegroundNotification()
        InstallReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        else
            startForeground(NOTIFICATION_ID, notification)

        sensorManager = getSystemService(SensorManager::class.java)
        sensorManager?.let {
            accelerometer = it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        boundClients++
        if (boundClients == 1)
            startAccelerometer()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        boundClients--
        return super.onUnbind(intent)
    }

    private fun loadSettingsFromPreferences() {
        val prefs = getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE)
        configSequenceLength = prefs.getInt(SEQUENCE_LENGTH_KEY, 40).coerceIn(20, 200)
        configSamplingPeriod = prefs.getInt(SAMPLING_PERIOD_KEY, 31).coerceIn(20, 200)
        configProbFall = prefs.getFloat(PROB_FALL_KEY, 0.5f).coerceIn(0.1f, 1.0f)
        logger(
            tag, "loadSettingsFromPreferences: " +
                    "sequence_length=$configSequenceLength " +
                    "sampling_period=$configSamplingPeriod " +
                    "prob_fall=$configProbFall "
        )
        _statusMessage.postValue("✔ Configuración actualizada")
    }

    private fun reconfigureSensor() {

        sensorManager?.unregisterListener(this)
        mHandlerThread.quitSafely()
        mHandlerThread.join()

        val newThread = HandlerThread("Accelerometer-Thread")
        newThread.start()
        val handler = Handler(newThread.looper)
        mHandlerThread = newThread

        bufferList.clear()

        accelerometer?.let { sensor ->
            sensorManager?.registerListener(
                this@FallDetectionService,
                sensor,
                configSamplingPeriod,
                configSequenceLength,
                handler
            )
        }
    }


    // Lógica de detección de caídas
    override fun onSensorChanged(event: SensorEvent) {

        bufferList.add(FallModel(x = event.values[0], y = event.values[1], z = event.values[2]))
        _sensorData.postValue(Triple(event.values[0], event.values[1], event.values[2]))

        if (bufferList.size >= configSequenceLength) {

            val internalBuffer = ArrayList(bufferList.map { it.copy() })
            bufferList.clear()

            if (modelReady) {
                modelScope.launch {
                    try {
                        val input = getBufferFromModel(internalBuffer)
                        val output = Array(1) { FloatArray(2) }
                        tflite.run(input, output)
                        val probFall = output[0][1]
                        val now = appTimeStamp

                        if (probFall > configProbFall && now - lastFallTime > 2000) {

                            //abrir Actividad.
                            FireStoreRepository.saveFallData(
                                androidID, FallEntity(
                                    data = internalBuffer,
                                    sequenceLength = configSequenceLength,
                                    samplingPeriod = configSamplingPeriod,
                                    probFall = configProbFall
                                )
                            )
                            lastFallTime = now

                            withContext(Dispatchers.Main) {
                                MediaPlayer.create(
                                    this@FallDetectionService,
                                    DEFAULT_ALARM_ALERT_URI
                                )?.start()
                            }
                            delay(3000)

                        }
                    } catch (e: Exception) {
                        _statusMessage.postValue("❌ Error de inferencia: ${e.message}")
                    }
                }
            }

        }

    }

    private fun startAccelerometer() {

        if (isListening) throw IllegalThreadStateException("thread is already running")
        accelerometer?.let { sensor ->
            mHandlerThread.start()
            val handler = Handler(mHandlerThread.looper)
            try {
                loadSettingsFromPreferences()
                sensorManager?.registerListener(
                    this@FallDetectionService,
                    sensor,
                    configSamplingPeriod,
                    configSequenceLength,
                    handler
                )
                isListening = true
            } catch (e: Exception) {
                _statusMessage.postValue("Error al registrar el listener del acelerómetro: ${e.message}")
            }

        }

        modelScope.launch {
            try {
                if (!verifyModel(assets)) {
                    _statusMessage.postValue("❌  Modelo no encontrado o corrupto")
                    return@launch
                }

                val modelBuffer = try {
                    loadModelFileMapped()
                } catch (e: Exception) {
                    _statusMessage.postValue("❌ Error cargando modelo: ${e.message}")
                    return@launch
                }

                val options = Interpreter.Options().apply {
                    numThreads = 1
                    useNNAPI = true
                }

                try {
                    // Verificación adicional del modelo
                    val tflite = Interpreter(modelBuffer, options)

                    // Prueba de compatibilidad
                    val inputTensor = tflite.getInputTensor(0)
                    val outputTensor = tflite.getOutputTensor(0)

                    logger(tag, "Input shape: ${inputTensor.shape().contentToString()}")
                    logger(tag, "Output shape: ${outputTensor.shape().contentToString()}")

                    this@FallDetectionService.tflite = tflite
                    modelReady = true
                    _statusMessage.postValue("✔ Modelo cargado correctamente")
                } catch (e: IllegalStateException) {
                    _statusMessage.postValue("❌ Modelo incompatible: ${e.message}")
                } catch (e: IllegalArgumentException) {
                    _statusMessage.postValue("❌ Error en modelo: ${e.message}")
                }
            } catch (e: Exception) {
                _statusMessage.postValue("❌ Error inesperado: ${e.message}")
            }

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        close()
    }

    private fun close() {
        unregisterReceiver(settingsReceiver)
        sensorManager?.unregisterListener(this@FallDetectionService)
        mHandlerThread.quit()
        isListening = false
        if (::tflite.isInitialized) tflite.close()
    }

    private fun getBufferFromModel(buffer: ArrayList<FallModel>): Array<Array<FloatArray>> {
        val trimmedList = buffer.takeLast(configSequenceLength)
        val outputBuffer = Array(1) { Array(configSequenceLength) { FloatArray(3) } }

        for ((index, fall) in trimmedList.withIndex()) {
            if (index < configSequenceLength) {
                outputBuffer[0][index][0] = fall.x
                outputBuffer[0][index][1] = fall.y
                outputBuffer[0][index][2] = fall.z
            }
        }
        return outputBuffer
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, FallDetectionService::class.java)
            startForegroundService(context, intent)
        }
    }

    private fun InstallReceiver() {
        when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.TIRAMISU ->
                registerReceiver(
                    settingsReceiver,
                    IntentFilter(ACTION_UPDATE_SETTINGS),
                    RECEIVER_NOT_EXPORTED
                )

            Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                registerReceiver(
                    settingsReceiver,
                    IntentFilter(ACTION_UPDATE_SETTINGS),
                    RECEIVER_EXPORTED
                )

            else -> registerReceiver(settingsReceiver, IntentFilter(ACTION_UPDATE_SETTINGS))
        }
    }

}