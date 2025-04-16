package com.example.pruebared.presentation.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.example.pruebared.presentation.FallDetectionViewModel

@Composable
fun FallDetectionScreen(viewModel: FallDetectionViewModel) {

    var showSettings by remember { mutableStateOf(false) }
    val scrollState = rememberScalingLazyListState()

    Scaffold(
        positionIndicator = {
            PositionIndicator(
                scalingLazyListState = scrollState,
                modifier = Modifier.fillMaxSize()
            )
        },
        vignette = {
            Vignette(vignettePosition = VignettePosition.TopAndBottom)
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(Color.LightGray)
                .fillMaxSize()
        ) {
            // Botón de ajustes
            Box(modifier = Modifier.fillMaxWidth(.9f)) {
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
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
    }

    // Mostrar pantalla de configuración
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            confirmButton = {},
            dismissButton = {},
            //title = { Text("Configuracións") },
            text = {
                SettingsScreen(
                    viewModel = viewModel,
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
    val status by viewModel.statusMessage.observeAsState("...")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth(.7f)
            .fillMaxHeight(.7f)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "Estado: \n$status", fontSize = 10.sp)
        AxisCard("X", x, color = MaterialTheme.colorScheme.primary)
        AxisCard("Y", y, color = MaterialTheme.colorScheme.secondary)
        AxisCard("Z", z, color = MaterialTheme.colorScheme.tertiary)

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
