package com.example.pruebared.presentation

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.flex.FlexDelegate

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var textView: TextView
    private lateinit var tflite: Interpreter

    private val sequenceLength = 40
    private val inputBuffer = Array(1) { Array(sequenceLength) { FloatArray(3) } }
    private var bufferIndex = 0
    private var lastFallTime = 0L
    private var modelReady = false

    private fun playAlarm() {
        val mp = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
        mp?.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI Setup
        val scrollView = ScrollView(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        textView = TextView(this).apply {
            textSize = 18f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            text = "Initializing..."
        }
        layout.addView(textView)
        scrollView.addView(layout)
        setContentView(scrollView)

        // Sensor setup
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        } else {
            textView.text = "No accelerometer found!"
        }

        // Load TFLite model safely in background thread
        Thread {
            try {
                val options = Interpreter.Options().apply {
                    addDelegate(FlexDelegate())
                    setNumThreads(2)
                    // Avoid GPU delegate for Wear OS unless confirmed support
                    // addDelegate(GpuDelegate())
                }
                val modelBuffer = loadModelFile("modelo_convertido.tflite")
                tflite = Interpreter(modelBuffer, options)
                modelReady = true
                runOnUiThread {
                    textView.text = "Model loaded successfully ✔️"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    textView.text = "❌ Failed to load model: ${e.message}"
                }
            }
        }.start()
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            inputBuffer[0][bufferIndex][0] = z
            inputBuffer[0][bufferIndex][1] = x
            inputBuffer[0][bufferIndex][2] = y
            bufferIndex++

            if (bufferIndex >= sequenceLength) {
                bufferIndex = 0

                if (modelReady && ::tflite.isInitialized) {
                    try {
                        val input = inputBuffer // Shape: [1][40][3]
                        val output = Array(1) { FloatArray(2) }

                        tflite.run(input, output)

                        val probFall = output[0][1]
                        val currentTime = System.currentTimeMillis()

                        if (probFall > 0.5 && currentTime - lastFallTime > 2000) {
                            lastFallTime = currentTime
                            playAlarm()
                        }

                        runOnUiThread {
                            textView.text = """
                                X: ${"%.2f".format(x)}
                                Y: ${"%.2f".format(y)} 
                                Z: ${"%.2f".format(z)}
                                Fall Probability: ${"%.1f".format(probFall * 100)}%
                            """.trimIndent()
                        }

                    } catch (e: Exception) {
                        Log.e("TFLite", "Inference failed: ${e.message}", e)
                        runOnUiThread {
                            textView.text = "❌ Inference error: ${e.message}"
                        }
                    }
                } else {
                    Log.w("TFLite", "Model not ready for inference yet")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        if (::tflite.isInitialized) {
            tflite.close()
        }
    }
}