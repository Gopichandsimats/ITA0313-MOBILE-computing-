package com.devicecare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

private val Navy = Color(0xFF101B35)
private val Blue = Color(0xFF5578F7)
private val Mint = Color(0xFF35C9A5)
private val SurfaceDark = Color(0xFF192642)
private val Muted = Color(0xFF9EACC7)
private val White = Color.White

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val metrics = DeviceMetrics(this)
        val detector = AnomalyDetector()
        val aiModel = DeviceHealthModel(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Blue, secondary = Mint, background = Navy,
                surface = SurfaceDark, onBackground = White, onSurface = White
            )) {
                var snapshot by remember { mutableStateOf(metrics.capture()) }
                var finding by remember { mutableStateOf(detector.analyze(snapshot)) }
                var prediction by remember { mutableStateOf(aiModel.predict(snapshot)) }
                var history by remember { mutableStateOf(listOf(snapshot)) }
                var selectedTab by remember { mutableIntStateOf(0) }

                DisposableEffect(Unit) {
                    onDispose { aiModel.close() }
                }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(5000)
                        val next = metrics.capture()
                        snapshot = next
                        finding = detector.analyze(next)
                        prediction = aiModel.predict(next)
                        history = (history + next).takeLast(48)
                    }
                }

                Scaffold(
                    containerColor = Navy,
                    bottomBar = {
                        NavigationBar(containerColor = SurfaceDark) {
                            NavigationBarItem(
                                selected = selectedTab == 0, onClick = { selectedTab = 0 },
                                icon = { Text("⌂", fontSize = 19.sp) }, label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1, onClick = { selectedTab = 1 },
                                icon = { Text("✦", fontSize = 19.sp) }, label = { Text("AI") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2, onClick = { selectedTab = 2 },
                                icon = { Text("⌁", fontSize = 19.sp) }, label = { Text("History") }
                            )
                        }
                    }
                ) { padding ->
                    when (selectedTab) {
                        0 -> OverviewScreen(snapshot, finding, prediction, history, Modifier.padding(padding))
                        1 -> InsightsScreen(finding, prediction, snapshot, Modifier.padding(padding))
                        else -> HistoryScreen(history, Modifier.padding(padding))
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewScreen(
    snapshot: DeviceSnapshot,
    finding: HealthFinding,
    prediction: ModelPrediction,
    history: List<DeviceSnapshot>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DEVICE CARE", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Your device, in control.", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Live performance overview", color = Muted, fontSize = 13.sp)
                }
                Box(Modifier.size(46.dp).background(Blue.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) {
                    Text("DC", color = Blue, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("TENSORFLOW LITE DEVICE SCORE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(predictionLabel(prediction), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(if (prediction.modelAvailable) "On-device model • ${prediction.confidence}% confidence" else "Model not bundled yet • fallback analysis active", color = Muted, fontSize = 11.sp)
                    }
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(78.dp)) {
                            drawArc(Muted.copy(alpha = .2f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                            val score = if (prediction.modelAvailable) prediction.probabilities.getOrNull(0)?.times(100f) ?: finding.score.toFloat() else finding.score.toFloat()
                            drawArc(Mint, -90f, score.coerceIn(0f, 100f) * 3.6f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                        }
                        Text(if (prediction.modelAvailable) "${prediction.confidence}%" else "${finding.score}", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Text("LIVE RESOURCES", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Battery", "${snapshot.batteryPercent}%", if (snapshot.isCharging) "Charging" else "On battery", Icons.Default.BatteryFull, Modifier.weight(1f))
                MetricCard("Memory", "${snapshot.ramUsedPercent}%", "${snapshot.ramUsedMb} / ${snapshot.ramTotalMb} MB", Icons.Default.Memory, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Storage", "${snapshot.storageUsedPercent}%", String.format(Locale.US, "%.1f / %.1f GB", snapshot.storageUsedGb, snapshot.storageTotalGb), Icons.Default.Storage, Modifier.weight(1f))
                MetricCard("App CPU", "${snapshot.appCpuPercent}%", "Device Care process", Icons.Default.Speed, Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("RESOURCE TREND", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("RAM usage • latest samples", color = Muted, fontSize = 12.sp)
                    TrendChart(history.map { it.ramUsedPercent.toFloat() }, Blue, Modifier.fillMaxWidth().height(92.dp))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("QUICK RECOMMENDATION", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(finding.details.first(), fontSize = 14.sp, lineHeight = 20.sp)
                    Text("Refreshes every 5 seconds • readings stay on this device", color = Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = Blue, modifier = Modifier.size(22.dp))
            Text(value, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun InsightsScreen(
    finding: HealthFinding,
    prediction: ModelPrediction,
    snapshot: DeviceSnapshot,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("AI INSIGHTS", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Understand your device", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Predictions run locally using TensorFlow Lite. No device telemetry is sent to a server.", color = Muted, fontSize = 13.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MODEL RESULT", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(predictionLabel(prediction), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (prediction.modelAvailable) {
                        Text("Model confidence: ${prediction.confidence}%", color = Muted, fontSize = 12.sp)
                        val labels = listOf("Healthy", "Monitor", "High risk")
                        prediction.probabilities.forEachIndexed { index, value ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(labels[index], fontSize = 13.sp)
                                    Text("${(value * 100).toInt()}%", color = Muted, fontSize = 12.sp)
                                }
                                LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth(), color = if (index == 2) Color(0xFFFF7777) else Mint, trackColor = Navy)
                            }
                        }
                    } else {
                        Text("Run the GitHub Actions workflow to train and package the TFLite model. Rule-based insights are shown until the model asset is available.", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("RECOMMENDATIONS", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    finding.details.forEachIndexed { index, detail ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Text("${index + 1}", color = Mint, fontWeight = FontWeight.Bold)
                            Text(detail, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CURRENT SNAPSHOT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Battery: ${snapshot.batteryPercent}%")
                    Text("RAM used: ${snapshot.ramUsedPercent}%")
                    Text("Storage used: ${snapshot.storageUsedPercent}%")
                    Text("App process CPU: ${snapshot.appCpuPercent}%")
                }
            }
        }
        item {
            Text("Research limitation: this model learns from generated scenarios labelled by resource thresholds. Its confidence is not a validated probability of hardware failure. Collect and label real device data before making reliability claims.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun HistoryScreen(history: List<DeviceSnapshot>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("PERFORMANCE HISTORY", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Your recent device trends", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Samples are collected every five seconds while the app is open. This session history resets when the app process ends.", color = Muted, fontSize = 12.sp)
        }
        item { HistoryChartCard("Battery level", history.map { it.batteryPercent.toFloat() }, Mint) }
        item { HistoryChartCard("RAM usage", history.map { it.ramUsedPercent.toFloat() }, Blue) }
        item { HistoryChartCard("Storage usage", history.map { it.storageUsedPercent.toFloat() }, Color(0xFFFFB86B)) }
        item {
            val first = history.firstOrNull()
            val last = history.lastOrNull()
            val delta = if (first != null && last != null) last.batteryPercent - first.batteryPercent else 0
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("BATTERY SESSION SUMMARY", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(if (delta < 0) "Battery changed by $delta percentage points during this session." else "Battery changed by +$delta percentage points during this session.")
                    Text("This is a simple observed change, not a calibrated battery-life estimate.", color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun HistoryChartCard(title: String, values: List<Float>, lineColor: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text("0–100%", color = Muted, fontSize = 11.sp)
            TrendChart(values, lineColor, Modifier.fillMaxWidth().height(130.dp))
            Text("${values.size} samples • 5-second interval", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TrendChart(values: List<Float>, lineColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val grid = Muted.copy(alpha = .16f)
        for (i in 0..4) {
            val y = size.height * i / 4f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        if (values.size == 1) {
            val y = size.height * (1f - values.first().coerceIn(0f, 100f) / 100f)
            drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(size.width / 2f, y))
        } else {
            val step = size.width / (values.size - 1)
            val points = values.mapIndexed { index, value ->
                Offset(index * step, size.height * (1f - value.coerceIn(0f, 100f) / 100f))
            }
            points.zipWithNext().forEach { (a, b) ->
                drawLine(lineColor, a, b, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
            points.forEach { drawCircle(lineColor, radius = 2.5.dp.toPx(), center = it) }
        }
    }
}

private fun predictionLabel(prediction: ModelPrediction): String = when {
    !prediction.modelAvailable -> "Adaptive analysis active"
    prediction.risk == ModelRisk.HEALTHY -> "Healthy resource profile"
    prediction.risk == ModelRisk.MONITOR -> "Monitor resource usage"
    else -> "High resource pressure"
}
