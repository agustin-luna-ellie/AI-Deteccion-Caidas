package io.fallcare.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold


@Composable
fun AeroBackgroundScreen(
    imageRes: Int,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScalingLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo de imagen
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Capa oscura opcional para mejorar contraste
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
        )

        Scaffold(
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = scrollState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        ) { content() }
    }
}
