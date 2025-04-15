package com.example.pruebared.presentation

import android.app.Application
import android.content.res.AssetManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


class FallDetectionViewModel(application: Application) :
    AndroidViewModel(application), SensorEventListener {

    private val context = application.applicationContext
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val sequenceLength = 40
    private val inputBuffer = Array(1) { Array(sequenceLength) { FloatArray(3) } }
    private var bufferIndex = 0
    private var lastFallTime = 0L

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

    private val modelScope = this.viewModelScope + Dispatchers.Default

    init {
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        modelScope.launch {
            try {
                val assetManager = context.assets

                if (!verifyModel(assetManager)) {
                    _statusMessage.postValue("❌ Modelo no encontrado o corrupto")
                    return@launch
                }

                val modelBuffer = try {
                    loadModelFileMapped() // Usaremos la versión mejorada
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

                    Log.d("ModelInfo", "Input shape: ${inputTensor.shape().contentToString()}")
                    Log.d("ModelInfo", "Output shape: ${outputTensor.shape().contentToString()}")

                    this@FallDetectionViewModel.tflite = tflite
                    modelReady = true
                    _statusMessage.postValue("✔ Modelo cargado correctamente")
                } catch (e: IllegalStateException) {
                    _statusMessage.postValue("❌ Modelo incompatible: ${e.message}")
                } catch (e: IllegalArgumentException) {
                    _statusMessage.postValue("❌ Error en modelo: ${e.message}")
                    Log.d("IllegalArgumentException", "Error en modelo: ${e.message}")
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
                bufferIndex = 0

                if (modelReady) {
                    val input = inputBuffer.map { it.clone() }.toTypedArray()
                    modelScope.launch {
                        try {
                            val output = Array(1) { FloatArray(2) }
                            tflite.run(input, output)
                            val probFall = output[0][1]
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

    @Throws(IOException::class)
    private fun loadModelFileMapped(): ByteBuffer {
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd("modelo_convertido.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor).apply {
            channel.position(fileDescriptor.startOffset)
        }

        return inputStream.use { inputStream ->
            inputStream.channel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            ).also { buffer ->
                buffer.order(ByteOrder.nativeOrder())
                Log.d(
                    "ModelLoading",
                    "Modelo mapeado correctamente. Tamaño: ${buffer.capacity()} bytes"
                )
            }
        }
    }

    private fun verifyModel(assetManager: AssetManager): Boolean {
        return try {
            val inputStream = assetManager.open("modelo_convertido.tflite")
            val bytes = inputStream.readBytes()
            bytes.isNotEmpty() // Verificación básica
        } catch (e: Exception) {
            false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
        if (::tflite.isInitialized) tflite.close()
    }

}
