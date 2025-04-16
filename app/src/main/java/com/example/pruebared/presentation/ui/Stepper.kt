package com.example.pruebared.presentation.ui


import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onValueChange((value - 1).coerceAtLeast(valueRange.first)) },
            enabled = value > valueRange.first,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrementar")
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = { onValueChange((value + 1).coerceAtMost(valueRange.last)) },
            enabled = value < valueRange.last,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Incrementar")
        }
    }
}
