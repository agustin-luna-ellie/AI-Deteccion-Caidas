package com.example.pruebared.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.wear.compose.material.MaterialTheme
import com.example.pruebared.presentation.ui.FallDetectionScreen

class MainActivity : ComponentActivity() {

    private val viewModel: FallDetectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FallDetectionScreen(viewModel)
            }
        }
    }
}

