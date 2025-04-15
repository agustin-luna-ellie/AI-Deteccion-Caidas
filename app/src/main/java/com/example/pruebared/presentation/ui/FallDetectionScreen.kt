package com.example.pruebared.presentation.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.pruebared.presentation.FallDetectionViewModel

@Composable
fun FallDetectionScreen(viewModel: FallDetectionViewModel) {
    val x by viewModel.x.observeAsState(0f)
    val y by viewModel.y.observeAsState(0f)
    val z by viewModel.z.observeAsState(0f)
    val fallDetected by viewModel.fallDetected.observeAsState(false)
    val status by viewModel.statusMessage.observeAsState("...")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(Color.LightGray)
            .fillMaxSize()
    ) {
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
}
