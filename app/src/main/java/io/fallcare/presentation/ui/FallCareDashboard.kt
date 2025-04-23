package io.fallcare.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fallcare.ui.theme.CoralPink
import com.fallcare.ui.theme.MintGreen
import com.fallcare.ui.theme.SilverGray
import com.fallcare.ui.theme.SkyBlue
import com.fallcare.ui.theme.SunshineYellow
import org.w3c.dom.Text


@Composable
fun FallCareDashboard() {
    val gradientColors = listOf(CoralPink, MintGreen, SunshineYellow, SilverGray, SkyBlue)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 32.dp, bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        gradientColors.forEachIndexed { i, color ->
            GlassCard(
                title = "Zona ${i + 1}",
                color = color,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                content = {
                    Text("statusMessage", color = Color.Black)
                }
            )
        }
    }
}

