package io.fallcare.presentation.ui

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fallcare.ui.theme.CoralPink
import io.fallcare.R
import io.fallcare.presentation.FallDetectionViewModel

@Composable
fun FallDetectionScreen(
    context: Context,
    viewModel: FallDetectionViewModel
) {

    var showSettings by remember { mutableStateOf(false) }

    AeroBackgroundScreen(imageRes = R.drawable.aero_mint_green) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize()
        ) {
            // Botón de ajustes
            IconButton(
                modifier = Modifier.fillMaxHeight(.2f),
                onClick = { showSettings = true },
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configuración"
                )
            }

            // Contenido principal
            MainContent(viewModel)
        }
    }

    // Mostrar pantalla de configuración
    if (showSettings) {
        AlertDialog(
            modifier = Modifier.fillMaxHeight(),
            onDismissRequest = { showSettings = false },
            confirmButton = {},
            dismissButton = {},
            text = {
                SettingsScreen(
                    context = context,
                    onBack = { showSettings = false }
                )
            }
        )
    }
}

@Composable
private fun MainContent(viewModel: FallDetectionViewModel) {
    val x by viewModel.x.observeAsState(0f)
    val y by viewModel.y.observeAsState(0f)
    val z by viewModel.z.observeAsState(0f)
    val fallDetected by viewModel.fallDetected.observeAsState(false)
    val statusMessage by viewModel.statusMessage.observeAsState("")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(bottom = 24.dp)
            .fillMaxWidth(.8f)
            .verticalScroll(rememberScrollState())
    ) {

        AccelerometerBarChart(x = x, y = y, z = z)

        GlassCard(title = "Status", color = CoralPink, content = {
            Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = Color.Black)
        })

        AnimatedVisibility(
            modifier = Modifier.fillMaxWidth(.9f),
            visible = fallDetected,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Text(
                text = "CAÍDA DETECTADA",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }
    }
}
