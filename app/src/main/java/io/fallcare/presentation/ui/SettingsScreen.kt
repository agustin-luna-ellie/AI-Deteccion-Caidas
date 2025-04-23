package io.fallcare.presentation.ui


import android.annotation.SuppressLint
import android.app.Service.MODE_PRIVATE
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.fallcare.util.Constants.ACTION_UPDATE_SETTINGS
import io.fallcare.util.Constants.PROB_FALL_KEY
import io.fallcare.util.Constants.SAMPLING_PERIOD_KEY
import io.fallcare.util.Constants.SEQUENCE_LENGTH_KEY
import io.fallcare.util.Constants.SETTINGS_KEY
import io.fallcare.util.saveSettings


@SuppressLint("DefaultLocale")
@Composable
fun SettingsScreen(
    context: Context,
    onBack: () -> Unit
) {

    val prefs = context.getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE)

    var sequenceLength by remember {
        mutableIntStateOf(
            prefs.getInt(SEQUENCE_LENGTH_KEY, 40).coerceIn(20, 200)
        )
    }
    var samplingPeriod by remember {
        mutableIntStateOf(
            prefs.getInt(SAMPLING_PERIOD_KEY, 31).coerceIn(20, 200)
        )
    }
    var probFall by remember {
        mutableFloatStateOf(
            prefs.getFloat(PROB_FALL_KEY, 0.5f).coerceIn(0.1f, 1.0f)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Encabezado
        Text(
            text = "Configuración:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Probabilidad: ${String.format("%.1f", probFall)}",
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = probFall,
            onValueChange = { probFall = it },
            valueRange = 0.1f..1.0f,
            steps = 10,
            modifier = Modifier.fillMaxWidth()
        )

        SettingItem(
            name = "Tamaño de Buffer: $sequenceLength",
            value = sequenceLength,
            range = 20..200,
            onValueChange = { sequenceLength = it }
        )

        // Configuración de Sampling Period
        SettingItem(
            name = "Muestreo: $samplingPeriod[ms]",
            value = samplingPeriod,
            range = 5..200,
            onValueChange = { samplingPeriod = it }
        )

        // Botón de Guardar
        Button(
            onClick = {
                context.saveSettings(
                    sequenceLength = sequenceLength,
                    samplingPeriod = samplingPeriod,
                    probFall = probFall
                )
                context.sendBroadcast(
                    Intent(context.ACTION_UPDATE_SETTINGS).apply {
                        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    }
                )
                onBack()
            }
        ) {
            Text("Guardar")
        }

    }
}

@Composable
private fun SettingItem(
    name: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall
        )

        // Selector numérico.
        Stepper(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
        )

    }
}
