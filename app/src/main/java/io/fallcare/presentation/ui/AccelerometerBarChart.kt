package io.fallcare.presentation.ui

import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

@Composable
fun AccelerometerBarChart(x: Float, y: Float, z: Float) {
    AndroidView(factory = { context ->
        BarChart(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                250
            )
            description.isEnabled = true
            setDrawGridBackground(false)
            //axisLeft.axisMinimum = -20f
            //axisLeft.axisMaximum = 20f
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.TOP
            legend.isEnabled = true
        }
    }, update = { chart ->
        val entries = listOf(
            BarEntry(0f, x),
            BarEntry(1f, y),
            BarEntry(2f, z)
        )

        val dataSet = BarDataSet(entries, "")
        dataSet.colors = listOf(Color.BLUE, Color.RED, Color.GREEN)
        dataSet.valueTextSize = 8f

        chart.data = BarData(dataSet)
        chart.invalidate()
    })
}
