package com.example.pruebared.presentation
import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
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
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.Locale
import java.util.concurrent.Executors
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.nio.MappedByteBuffer



class FallDetectionViewModel(application: Application) :
    AndroidViewModel(application), SensorEventListener {

    private val tag = this::class.java.simpleName

    private val context = application.applicationContext
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val modelScope = this.viewModelScope + Dispatchers.Default
    private var isListening: Boolean = false
    private var lastFallTime = 0L

    // Networking variables
    private val serverIP = "192.168.1.203" // Update with your PC's IP address
    private val serverPort = 9000
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private val executor = Executors.newSingleThreadExecutor()

    // Configuration variables
    var sequenceLength by mutableIntStateOf(40)
        private set
    var samplingPeriod by mutableIntStateOf(31)
        private set

    // Fixed: Proper data structure for sliding window
    private var dataBuffer = ArrayDeque<FloatArray>(sequenceLength)
    private lateinit var tflite: Interpreter
    private var modelReady = false
    private var bufferFilled = false
    private var samplesCollected = 0

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

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("modelo_convertido.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    init {
        // Initialize buffer
        initializeBuffer()

        // Connect to server first
        connectToServer()

        // Initialize sensor


        accelerometer?.let { sensor ->
            try {
                sensorManager.registerListener(
                    this,
                    sensor,
                    samplingPeriod * 1000, // Convert to microseconds
                    0
                )
                isListening = true
                _statusMessage.postValue("✅ Sensor inicializado")
            } catch (e: Exception) {
                Log.e(tag, "Error al registrar el listener del acelerómetro: ${e.message}")
                _statusMessage.postValue("❌ Error de sensor: ${e.message}")
            }
        }

        // Load TFLite model
        modelScope.launch {
            try {
                _statusMessage.postValue("⏳ Cargando modelo...")

                // Load model from assets
                val modelBuffer = try {
                    loadModelFile()
                } catch (e: Exception) {
                    _statusMessage.postValue("❌ Error cargando modelo: ${e.message}")
                    return@launch
                }

                // Create TFLite interpreter
                try {
                    val options = Interpreter.Options().apply {
                        numThreads = 1
                        useNNAPI = false // Start with CPU for compatibility
                    }

                    this@FallDetectionViewModel.tflite = Interpreter(modelBuffer, options)
                    modelReady = true
                    _statusMessage.postValue("✔ Modelo cargado (CPU)")
                } catch (e: Exception) {
                    _statusMessage.postValue("❌ Error de modelo: ${e.message}")
                    Log.e(tag, "Error creating TFLite interpreter: ${e.message}")
                }

            } catch (e: Exception) {
                _statusMessage.postValue("❌ Error inesperado: ${e.message}")
            }
        }
    }

    private fun initializeBuffer() {
        dataBuffer.clear()
        samplesCollected = 0
        bufferFilled = false
    }

    private fun connectToServer() {
        executor.execute {
            try {
                _statusMessage.postValue("⏳ Conectando al servidor...")
                socket = Socket(serverIP, serverPort)
                writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                _statusMessage.postValue("✅ Conectado al servidor")
                Log.d(tag, "Connected to server at $serverIP:$serverPort")
            } catch (e: Exception) {
                _statusMessage.postValue("❌ Error de conexión: ${e.message}")
                Log.e(tag, "Error connecting to server: ${e.message}")

                // Schedule reconnect attempt after 5 seconds
                modelScope.launch {
                    delay(5000)
                    connectToServer()
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { data ->
            // Fixed: Correct axis mapping - data comes as z,x,y but we need x,y,z

            val xVal = data.values[0]
            val yVal = data.values[1]
            val zVal = data.values[2]

            // Update UI values
            _x.postValue(xVal)
            _y.postValue(yVal)
            _z.postValue(zVal)

            // Process data synchronously to avoid threading issues
            processNewSensorData(xVal, yVal, zVal)
        }
    }

    private fun processNewSensorData(x: Float, y: Float, z: Float) {
        // Create new data point
        val newDataPoint = floatArrayOf(x, y, z)

        synchronized(this) {
            // Add to buffer
            if (!bufferFilled) {
                // Initial filling phase
                dataBuffer.add(newDataPoint)
                samplesCollected++

                if (samplesCollected == sequenceLength) {
                    bufferFilled = true
                    Log.d(tag, "✅ Buffer inicial completado, comenzando inferencia en tiempo real.")
                }

                // Send only raw data during buffer filling
                sendRawDataToPC(x, y, z)
                return
            }

            // Sliding window: remove oldest, add newest
            if (dataBuffer.size >= sequenceLength) {
                dataBuffer.removeFirst() // Remove oldest sample
            }
            dataBuffer.add(newDataPoint) // Add new sample

            // Run inference if model is ready and buffer is full
            if (modelReady && dataBuffer.size == sequenceLength) {
                runInferenceAndSendData(x, y, z)
            } else {
                // If model not ready, just send raw data
                sendRawDataToPC(x, y, z)
            }
        }
    }

    private fun runInferenceAndSendData(currentX: Float, currentY: Float, currentZ: Float) {
        // Prepare data outside of coroutine to avoid threading issues
        val inputArray = Array(1) { Array(sequenceLength) { FloatArray(3) } }

        // Create input array while synchronized
        synchronized(this) {
            if (dataBuffer.size != sequenceLength) {
                Log.w(tag, "Buffer size mismatch: ${dataBuffer.size} vs $sequenceLength")
                // Send raw data as fallback
                sendRawDataToPC(currentX, currentY, currentZ)
                return
            }

            // Copy data from buffer to input array safely
            dataBuffer.forEachIndexed { index, dataPoint ->
                inputArray[0][index][0] = dataPoint[0] // x
                inputArray[0][index][1] = dataPoint[1] // y
                inputArray[0][index][2] = dataPoint[2] // z
            }
        }

        // Run inference in coroutine
        modelScope.launch {
            try {
                // Run inference (now thread-safe since input array is prepared)
                val output = Array(1) { FloatArray(2) }
                synchronized(tflite) { // Ensure TFLite interpreter thread safety
                    tflite.run(inputArray, output)
                }
                val probFall = output[0][1] // Fall probability (class 1)

                // Update UI with probability
                _fallProbability.postValue(probFall)

                // Send ONLY prediction data (includes x,y,z + prediction)
                sendPredictionToPC(currentX, currentY, currentZ, probFall * 100)

                // Fall detection logic
                val now = System.currentTimeMillis()
                if (probFall > 0.5 && now - lastFallTime > 2000) {
                    lastFallTime = now
                    _fallDetected.postValue(true)

                    withContext(Dispatchers.Main) {
                        try {
                            MediaPlayer.create(
                                context,
                                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                            )?.start()
                        } catch (e: Exception) {
                            Log.e(tag, "Error playing alarm: ${e.message}")
                        }
                    }

                    delay(3000)
                    _fallDetected.postValue(false)
                }

            } catch (e: Exception) {
                Log.e(tag, "Error en inferencia: ${e.message}")
                _statusMessage.postValue("❌ Error de inferencia: ${e.message}")

                // Send raw data as fallback
                sendRawDataToPC(currentX, currentY, currentZ)
            }
        }
    }


    private fun sendRawDataToPC(x: Float, y: Float, z: Float) {
        val data = String.format(Locale.US, "x:%.3f,y:%.3f,z:%.3f", x, y, z)
        sendToServer(data)
    }

    private fun sendPredictionToPC(x: Float, y: Float, z: Float, fallProb: Float) {
        val data = String.format(Locale.US, "x:%.3f,y:%.3f,z:%.3f,pred:%.2f", x, y, z, fallProb)
        sendToServer(data)
    }

    private fun sendToServer(data: String) {
        executor.execute {
            try {
                writer?.apply {
                    write(data + "\n")
                    flush()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error sending data: ${e.message}")
                // Try to reconnect
                try {
                    socket?.close()
                    writer?.close()
                } catch (ignored: Exception) {}

                connectToServer()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    // Function to update settings
    fun updateSettings(newSequenceLength: Int, newSamplingPeriod: Int) {
        sequenceLength = newSequenceLength.coerceIn(20, 100)
        samplingPeriod = newSamplingPeriod.coerceIn(10, 100)
        resetBuffer() // Reset buffer with new size
    }

    private fun resetBuffer() {
        initializeBuffer()

        // Re-register sensor with new sampling period
        if (isListening) {
            sensorManager.unregisterListener(this)
            accelerometer?.let { sensor ->
                try {
                    sensorManager.registerListener(
                        this,
                        sensor,
                        samplingPeriod * 1000,
                        0
                    )
                } catch (e: Exception) {
                    Log.e(tag, "Error re-registering sensor: ${e.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up resources
        sensorManager.unregisterListener(this)
        if (::tflite.isInitialized) {
            synchronized(tflite) {
                tflite.close()
            }
        }

        // Close network connection
        executor.execute {
            try {
                writer?.close()
                socket?.close()
                Log.d(tag, "Connection closed")
            } catch (e: Exception) {
                Log.e(tag, "Error closing connection: ${e.message}")
            }
        }
        executor.shutdown()
    }
}