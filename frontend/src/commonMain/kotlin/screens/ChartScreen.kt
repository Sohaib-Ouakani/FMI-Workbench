package screens

import AccentCyan
import AccentGreen
import BgElevated
import BgSurface
import BorderSubtle
import TextPrimary
import TextSecondary
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.fmi_client.client.SimulationResult
import io.github.koalaplot.core.line.LinePlot
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.XYGraph
import kotlin.math.pow
import kotlin.math.round

// Engineering palette for data series — distinct and readable on dark bg
private val seriesColors = listOf(
    Color(0xFF003366),
    Color(0xFFCC3333),
    Color(0xFF339933),
    Color(0xFFFF9933),
    Color(0xFF8B34FF),
    Color(0xFF996633),
    Color(0xFFFF66B2),
    Color(0xFF33CCCC),
    Color(0xFF888888),
    Color(0xFFCCA300),
)

class ChartScreen(
    private val result: SimulationResult,
    private val variableUnits: Map<String, String> = emptyMap()
) : Screen {

    @OptIn(ExperimentalKoalaPlotApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val times   = result.timestamps.map { it.toFloat() }
        val timeMin = times.minOrNull() ?: 0f
        val timeMax = times.maxOrNull() ?: 1f

        val allValues = result.variables.values.flatten().map { it.toFloat() }
        val yMin      = allValues.minOrNull() ?: 0f
        val yMax      = allValues.maxOrNull() ?: 1f
        val yPadding  = ((yMax - yMin) * 0.08f).coerceAtLeast(0.001f)

        val variableNames = result.variables.keys.toList()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("SIMULATION RESULTS", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = result.config.experimentName,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    // "Done" badge
                    Box(
                        modifier = Modifier
                            .background(AccentGreen.copy(0.12f))
                            .border(
                                1.dp,
                                AccentGreen.copy(0.4f),
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "COMPLETE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                            ),
                        )
                    }
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, AccentCyan.copy(0.5f), Color.Transparent)
                            )
                        )
                )

                // ── Stats strip ───────────────────────────────────────────────
                StatsStrip(result, times.size, yMin, yMax)

                // ── Legend ────────────────────────────────────────────────────
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgSurface)
                        .border(1.dp, BorderSubtle)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    itemsIndexed(variableNames) { index, name ->
                        val colour = seriesColors[index % seriesColors.size]
                        val unit = variableUnits[name]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            // Series color dash
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(3.dp)
                                    .background(colour)
                            )
                            Text(
                                text = "$name [$unit]",
                                style = MaterialTheme.typography.bodySmall.copy(color = colour),
                            )
                        }
                    }
                }

                // ── Chart area ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(BgSurface)
                        .border(1.dp, BorderSubtle)
                        .padding(8.dp),
                ) {
                    @Suppress("DEPRECATION")
                    XYGraph(
                        xAxisModel = FloatLinearAxisModel(timeMin..timeMax),
                        yAxisModel  = FloatLinearAxisModel((yMin - yPadding)..(yMax + yPadding)),
                        xAxisTitle  = "Time (s)",
                        yAxisTitle  = "Value",
                        modifier    = Modifier.fillMaxSize(),
                    ) {
                        variableNames.forEachIndexed { index, name ->
                            val colour = seriesColors[index % seriesColors.size]
                            val values = result.variables[name] ?: return@forEachIndexed
                            val points = times.zip(values.map { it.toFloat() })
                                .map { (t, v) -> DefaultPoint(t, v) }

                            LinePlot(
                                data      = points,
                                lineStyle = LineStyle(
                                    brush       = SolidColor(colour),
                                    strokeWidth = 2.dp,
                                ),
                            )
                        }
                    }
                }

                // ── Actions ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EngButton(
                        text = "BACK TO CONFIG",
                        onClick = { navigator.pop() },
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                    )
                    EngButton(
                        text = "BACK TO INFO SCREEN",
                        onClick = {
                            // pop twice to reach HomeScreen
                            navigator.pop()
                            navigator.pop()
                        },
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                    )
                }
            }
        }
    }
}

// ── Stats strip ───────────────────────────────────────────────────────────────
private fun roundToString(value: Float, decimals: Int = 4): String {
    val factor = 10.0.pow(decimals).toFloat()
    val rounded = round(value * factor) / factor
    return rounded.toString()
}

@Composable
private fun StatsStrip(
    result: SimulationResult,
    pointCount: Int,
    yMin: Float,
    yMax: Float,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgElevated)
            .border(1.dp, BorderSubtle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatCell("T0", "${result.config.startTime} s")
        StatDivider()
        StatCell("T_END", "${result.config.stopTime ?: "—"} s")
        StatDivider()
        StatCell("Δt", "${result.config.stepSize} s")
        StatDivider()
        StatCell("POINTS", "$pointCount")
        StatDivider()
        StatCell("Y_MIN",  roundToString(yMin))
        StatDivider()
        StatCell("Y_MAX",  roundToString(yMax))
        StatDivider()
        StatCell("SERIES", "${result.variables.size}")
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 1.sp),
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextPrimary, fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(BorderSubtle)
    )
}
