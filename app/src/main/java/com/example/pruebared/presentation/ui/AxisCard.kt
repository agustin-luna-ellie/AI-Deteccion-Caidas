package com.example.pruebared.presentation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs


@Composable
fun AxisCard(axis: String, value: Float, color: Color) {

    val animatedValue by animateFloatAsState(
        targetValue = abs(value),
        label = "$axis-animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$axis: %.2f".format(value),
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            LinearProgressIndicator(
                progress = {
                    (animatedValue / 20).coerceIn(0f, 1f) // Ajuste para escala visible
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = color,
            )
        }
    }
}
