package com.example.pruebared.presentation

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pruebared.presentation.util.loadModelFileMapped
import com.example.pruebared.presentation.util.verifyModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter


class FallDetectionViewModel(application: Application) :
    AndroidViewModel(application), SensorEventListener {

    private val tag = this::class.java.simpleName

    private val context = application.applicationContext
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val mHandlerThread = HandlerThread("Accelerometer-Thread")
    private val modelScope = this.viewModelScope + Dispatchers.Default
    private var isListening: Boolean = false
    private var bufferIndex = 0
    private var lastFallTime = 0L


    // Variables de configuración
    var sequenceLength by mutableIntStateOf(40)
        private set
    var samplingPeriod by mutableIntStateOf(31)
        private set

    private var inputBuffer = Array(1) { Array(sequenceLength) { FloatArray(3) } }
    private lateinit var tflite: Interpreter
    private var modelReady = false

    private val _x = MutableLiveData(0f)
    val x: LiveData<Float> = _x

    private val _y = MutableLiveData(0f)
    val y: LiveData<Float> = _y

    private val _z = MutableLiveData(0f)
    val z: LiveData<Float> = _z

    private val _fallDetected = MutableLiveData(false)
    val fallDetected: LiveData<Boolean> = _fallDetected

    private val _statusMessage = MutableLiveData("Cargando modelo...")
    val statusMessage: LiveData<String> = _statusMessage

    private val _fallProbability = MutableLiveData(0f)
    val fallProbability: LiveData<Float> = _fallProbability


    init {

        if (isListening) throw IllegalThreadStateException("thread is already running")

        accelerometer?.let { sensor ->
            mHandlerThread.start()
            val handler = Handler(mHandlerThread.looper)
            try {
                sensorManager.registerListener(
                    this,
                    sensor,
                    samplingPeriod,
                    sequenceLength,
                    handler
                )
                isListening = true
            } catch (e: Exception) {
                Log.e(tag, "Error al registrar el listener del acelerómetro: ${e.message}")
            }

        }

        modelScope.launch {
            try {
                val assetManager = context.assets

                if (!verifyModel(assetManager)) {
                    _statusMessage.postValue("❌ Modelo no encontrado o corrupto")
                    return@launch
                }

                val modelBuffer = try {
                    context.loadModelFileMapped()
                } catch (e: Exception) {
                    _statusMessage.postValue("❌ Error cargando modelo: ${e.message}")
                    return@launch
                }

                // Temporary interpreter to test NNAPI
                val inputDummy = Array(1) { Array(sequenceLength) { FloatArray(3) } }
                val outputDummy = Array(1) { FloatArray(2) }

                try {
                    val nnapiOptions = Interpreter.Options().apply {
                        numThreads = 1
                        useNNAPI = true
                    }

                    val nnapiInterpreter = Interpreter(modelBuffer, nnapiOptions)

                    // 🧪 Try dummy inference to confirm NNAPI compatibility
                    nnapiInterpreter.run(inputDummy, outputDummy)

                    // ✅ Success with NNAPI
                    this@FallDetectionViewModel.tflite = nnapiInterpreter
                    modelReady = true
                    _statusMessage.postValue("✔ Modelo cargado con NNAPI")

                } catch (e: Exception) {
                    Log.w(tag, "❌ NNAPI incompatible, fallback to CPU: ${e.message}")

                    val cpuOptions = Interpreter.Options().apply {
                        numThreads = 1
                        useNNAPI = false
                    }

                    val cpuInterpreter = Interpreter(modelBuffer, cpuOptions)
                    cpuInterpreter.run(inputDummy, outputDummy) // Validate once

                    this@FallDetectionViewModel.tflite = cpuInterpreter
                    modelReady = true
                    _statusMessage.postValue("⚠️ Cargado sin NNAPI (modo CPU)")
                }

            } catch (e: Exception) {
                _statusMessage.postValue("❌ Error inesperado: ${e.message}")
            }
        }



    }


    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { data ->
            val xVal = data.values[0]
            val yVal = data.values[1]
            val zVal = data.values[2]

            _x.postValue(xVal)
            _y.postValue(yVal)
            _z.postValue(zVal)

            inputBuffer[0][bufferIndex][0] = zVal
            inputBuffer[0][bufferIndex][1] = xVal
            inputBuffer[0][bufferIndex][2] = yVal
            bufferIndex++

            if (bufferIndex >= sequenceLength) {

                Log.d(
                    tag,
                    "Buffer[${System.currentTimeMillis()}]: ${inputBuffer.contentDeepToString()}"
                )

                bufferIndex = 0

                if (modelReady) {
                    val input = inputBuffer.map { it.clone() }.toTypedArray()
                    modelScope.launch {
                        try {
                            val output = Array(1) { FloatArray(2) }
                            tflite.run(input, output)
                            val probFall = output[0][1]

                            _fallProbability.postValue(probFall)

                            val now = System.currentTimeMillis()

                            if (probFall > 0.5 && now - lastFallTime > 2000) {
                                lastFallTime = now
                                _fallDetected.postValue(true)

                                withContext(Dispatchers.Main) {
                                    MediaPlayer.create(
                                        context,
                                        android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                                    )?.start()
                                }

                                delay(3000)
                                _fallDetected.postValue(false)
                            }
                        } catch (e: Exception) {
                            _statusMessage.postValue("❌ Error de inferencia: ${e.message}")
                        }
                    }
                }

            }
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}


    // Función para actualizar configuración
    fun updateSettings(newSequenceLength: Int, newSamplingPeriod: Int) {
        sequenceLength = newSequenceLength.coerceIn(20, 100)
        samplingPeriod = newSamplingPeriod.coerceIn(10, 100)
        resetBuffer() // Reiniciar el buffer con el nuevo tamaño
    }


    private fun resetBuffer() {
        inputBuffer = Array(1) { Array(sequenceLength) { FloatArray(3) } }
        bufferIndex = 0
    }


    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
        mHandlerThread.quit()
        isListening = false
        if (::tflite.isInitialized) tflite.close()
    }

}
