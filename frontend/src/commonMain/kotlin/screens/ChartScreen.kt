package com.example.fmi_client.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.fmi_client.client.SimulationResult
import io.github.koalaplot.core.line.LinePlot
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.XYGraph

// Distinct colours for up to 6 series
private val seriesColours = listOf(
    Color(0xFF1f77b4),
    Color(0xFFff7f0e),
    Color(0xFF2ca02c),
    Color(0xFFd62728),
    Color(0xFF9467bd),
    Color(0xFF8c564b),
)

class ChartScreen(private val result: SimulationResult) : Screen {

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val times = result.timestamps.map { it.toFloat() }
        val timeMin = times.minOrNull() ?: 0f
        val timeMax = times.maxOrNull() ?: 1f

        val allValues = result.variables.values.flatten().map { it.toFloat() }
        val yMin = allValues.minOrNull() ?: 0f
        val yMax = allValues.maxOrNull() ?: 1f
        // Add a small margin so lines don't sit on the axis edges
        val yPadding = ((yMax - yMin) * 0.05f).coerceAtLeast(0.001f)

        val variableNames = result.variables.keys.toList()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Simulation Results", style = MaterialTheme.typography.headlineSmall)

            Text(
                text = "Stop: ${result.config.stopTime}s  |  " +
                    "Step: ${result.config.stepSize}s  |  " +
                    "${result.timestamps.size} data points",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── Legend ────────────────────────────────────────────────────────
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(variableNames) { index, name ->
                    val colour = seriesColours[index % seriesColours.size]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(12.dp),
                            color = colour,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {}
                        Text(name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // ── Chart ─────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                @Suppress("DEPRECATION")
                XYGraph(
                    xAxisModel = FloatLinearAxisModel(timeMin..timeMax),
                    yAxisModel = FloatLinearAxisModel((yMin - yPadding)..(yMax + yPadding)),
                    xAxisTitle = "Time (s)",
                    yAxisTitle = "Value",
                    modifier = Modifier.fillMaxSize(),
                ) {
                    variableNames.forEachIndexed { index, name ->
                        val colour = seriesColours[index % seriesColours.size]
                        val values = result.variables[name] ?: return@forEachIndexed
                        val points = times.zip(values.map { it.toFloat() })
                            .map { (t, v) -> DefaultPoint(t, v) }

                        LinePlot(
                            data = points,                     // ← 'data' instead of 'points'
                            lineStyle = LineStyle(
                                brush = SolidColor(colour),
                                strokeWidth = 2.dp,
                            )
                        )
                    }
                }
            }

            // ── Back button ───────────────────────────────────────────────────
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navigator.pop() },
            ) {
                Text("Back")
            }
        }
    }
}
