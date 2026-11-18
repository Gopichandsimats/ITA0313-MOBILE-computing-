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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

private val Navy = androidx.compose.ui.graphics.Color(0xFF101B35)
private val Blue = androidx.compose.ui.graphics.Color(0xFF5578F7)
private val Mint = androidx.compose.ui.graphics.Color(0xFF35C9A5)
private val SurfaceDark = androidx.compose.ui.graphics.Color(0xFF192642)
private val Muted = androidx.compose.ui.graphics.Color(0xFF9EACC7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val metrics = DeviceMetrics(this)
        val detector = AnomalyDetector()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Blue,
                    secondary = Mint,
                    background = Navy,
                    surface = SurfaceDark,
                    onBackground = androidx.compose.ui.graphics.Color.White,
                    onSurface = androidx.compose.ui.graphics.Color.White
                )
            ) {
                var snapshot by remember { mutableStateOf(metrics.capture()) }
                var finding by remember { mutableStateOf(detector.analyze(snapshot)) }
                var selectedTab by remember { mutableIntStateOf(0) }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(3000)
                        snapshot = metrics.capture()
                        finding = detector.analyze(snapshot)
                    }
                }

                Scaffold(
                    containerColor = Navy,
                    bottomBar = {
                        NavigationBar(containerColor = SurfaceDark) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Text("⌂", fontSize = 20.sp) },
                                label = { Text("Overview") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Text("✦", fontSize = 20.sp) },
                                label = { Text("AI insights") }
                            )
                        }
                    }
                ) { padding ->
                    if (selectedTab == 0) {
                        OverviewScreen(snapshot, finding, Modifier.padding(padding))
                    } else {
                        InsightsScreen(finding, snapshot, Modifier.padding(padding))
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewScreen(snapshot: DeviceSnapshot, finding: HealthFinding, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DEVICE CARE", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Your device, in control.", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Live performance overview", color = Muted, fontSize = 13.sp)
                }
                Box(Modifier.size(44.dp).background(Blue.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) {
                    Text("DC", color = Blue, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("AI DEVICE HEALTH", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(finding.headline, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Adaptive on-device analysis", color = Muted, fontSize = 12.sp)
                    }
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(82.dp)) {
                            drawArc(Muted.copy(alpha = .2f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(Mint, -90f, finding.score * 3.6f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                        }
                        Text("${finding.score}", fontSize = 23.sp, fontWeight = FontWeight.Bold)
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
                    Text("QUICK RECOMMENDATION", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(finding.details.first(), fontSize = 14.sp, lineHeight = 20.sp)
                    Text("Updated every 3 seconds", color = Muted, fontSize = 11.sp)
                }
            }
        }
        item {
            Text("Privacy first: resource readings are processed locally on this device.", color = Muted, fontSize = 11.sp)
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
private fun InsightsScreen(finding: HealthFinding, snapshot: DeviceSnapshot, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("AI INSIGHTS", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Understand your device", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Recommendations are generated on-device from live resource readings and recent baselines.", color = Muted, fontSize = 13.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(finding.headline, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
            Text("Model note: this first version uses a transparent, history-based anomaly detector with adaptive baselines and explicit thresholds. It is not a cloud AI service and does not claim to predict hardware failure.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
