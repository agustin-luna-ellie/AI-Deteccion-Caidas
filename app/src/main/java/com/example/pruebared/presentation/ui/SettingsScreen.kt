package com.example.pruebared.presentation.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pruebared.presentation.FallDetectionViewModel


@Composable
fun SettingsScreen(
    viewModel: FallDetectionViewModel,
    onBack: () -> Unit
) {
    var sequenceLength by remember { mutableIntStateOf(viewModel.sequenceLength) }
    var samplingPeriod by remember { mutableIntStateOf(viewModel.samplingPeriod) }

    Column(
        modifier = Modifier
            .fillMaxSize(.95f)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Encabezado
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
        )

        // Configuración de Sequence Length
        SettingItem(
            name = "Tamaño de Buffer: $sequenceLength",
            value = sequenceLength,
            range = 20..100,
            onValueChange = { sequenceLength = it }
        )

        // Configuración de Sampling Period
        SettingItem(
            name = "Muestreo: $samplingPeriod (ms)",
            value = samplingPeriod,
            range = 10..100,
            onValueChange = { samplingPeriod = it }
        )

        // Botón de Guardar
        Button(
            onClick = {
                viewModel.updateSettings(sequenceLength, samplingPeriod)
                onBack()
            },
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth(.9f)
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
        modifier = Modifier.fillMaxWidth(0.95f),
        horizontalAlignment = Alignment.CenterHorizontally
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
            modifier = Modifier.padding(top = 8.dp)
        )

    }
}
