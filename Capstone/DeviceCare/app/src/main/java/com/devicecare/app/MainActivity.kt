package com.devicecare.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
                var selectedScreen by remember { mutableIntStateOf(0) }
                var storage by remember { mutableStateOf(metrics.storageBreakdown()) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { storage = metrics.storageBreakdown() }

                fun openScreen(id: Int) {
                    selectedScreen = id
                    if (id == 2) {
                        val permissions = if (Build.VERSION.SDK_INT >= 33) arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                        ) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        val missing = permissions.filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
                        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
                        else storage = metrics.storageBreakdown()
                    }
                }

                DisposableEffect(Unit) { onDispose { aiModel.close() } }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(5000)
                        val next = metrics.capture()
                        snapshot = next
                        finding = detector.analyze(next)
                        prediction = aiModel.predict(next)
                        history = (history + next).takeLast(120)
                        if (selectedScreen == 2) storage = metrics.storageBreakdown()
                    }
                }

                Scaffold(
                    containerColor = Navy,
                    bottomBar = {
                        NavigationBar(containerColor = SurfaceDark) {
                            listOf(0 to "Home", 5 to "Tools", 10 to "AI", 11 to "History").forEach { (id, label) ->
                                NavigationBarItem(
                                    selected = selectedScreen == id || (id == 5 && selectedScreen in 1..9) || (id == 10 && selectedScreen == 10),
                                    onClick = { openScreen(id) },
                                    icon = { Text(when (id) { 0 -> "⌂"; 5 -> "▦"; 10 -> "✦"; else -> "⌁" }, fontSize = 19.sp) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    when (selectedScreen) {
                        0 -> OverviewScreen(snapshot, finding, prediction, history, { openScreen(it) }, Modifier.padding(padding))
                        1 -> BatteryScreen(snapshot, history, Modifier.padding(padding))
                        2 -> StorageScreen(snapshot, storage, { storage = metrics.storageBreakdown() }, { openScreen(9) }, Modifier.padding(padding))
                        3 -> MemoryScreen(snapshot, history, Modifier.padding(padding))
                        4 -> CpuScreen(snapshot, history, Modifier.padding(padding))
                        5 -> ToolsScreen({ openScreen(it) }, Modifier.padding(padding))
                        6 -> DeviceInfoScreen(snapshot, Modifier.padding(padding))
                        7 -> AppsScreen({ openSystemSettings(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS) }, Modifier.padding(padding))
                        8 -> BatteryCareScreen(snapshot, Modifier.padding(padding))
                        9 -> StorageCareScreen({ openSystemSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS) }, Modifier.padding(padding))
                        10 -> InsightsScreen(finding, prediction, snapshot, Modifier.padding(padding))
                        11 -> HistoryScreen(history, Modifier.padding(padding))
                        12 -> NetworkScreen({ openSystemSettings(Settings.ACTION_WIFI_SETTINGS) }, Modifier.padding(padding))
                        13 -> PrivacyScreen(Modifier.padding(padding))
                        else -> SettingsScreen({ openSystemSettings(Settings.ACTION_SETTINGS) }, Modifier.padding(padding))
                    }
                }
            }
        }
    }

    private fun openSystemSettings(action: String) {
        try { startActivity(Intent(action)) } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}

@Composable
private fun OverviewScreen(snapshot: DeviceSnapshot, finding: HealthFinding, prediction: ModelPrediction,
                           history: List<DeviceSnapshot>, navigate: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DEVICE CARE", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Your device, in control.", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Live performance overview • 5 sec refresh", color = Muted, fontSize = 12.sp)
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
                        Text("DEVICE HEALTH", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(predictionLabel(prediction), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(if (prediction.modelAvailable) "On-device AI • ${prediction.confidence}% model confidence" else "Adaptive rule-based analysis active", color = Muted, fontSize = 11.sp)
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
        item { Text("TAP A RESOURCE FOR DETAILS", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Battery", "${snapshot.batteryPercent}%", batteryTimeRemaining(snapshot), Icons.Default.BatteryFull, Modifier.weight(1f)) { navigate(1) }
                MetricCard("Memory", "${snapshot.ramUsedPercent}%", "${snapshot.ramUsedMb} / ${snapshot.ramTotalMb} MB", Icons.Default.Memory, Modifier.weight(1f)) { navigate(3) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Storage", "${snapshot.storageUsedPercent}%", String.format(Locale.US, "%.1f / %.1f GB", snapshot.storageUsedGb, snapshot.storageTotalGb), Icons.Default.Storage, Modifier.weight(1f)) { navigate(2) }
                MetricCard("App CPU", "${snapshot.appCpuPercent}%", "Device Care process only", Icons.Default.Speed, Modifier.weight(1f)) { navigate(4) }
            }
        }
        item {
            Text("MORE DEVICE TOOLS", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("Device information", "Battery temperature, voltage and Android details", "▣") { navigate(6) }
                QuickTile("Battery care", "Battery status, charging guidance and estimated runtime", "⚡") { navigate(8) }
                QuickTile("Storage breakdown", "See photos, videos, audio and other space", "▤") { navigate(2) }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RESOURCE TREND", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("RAM usage • latest samples", color = Muted, fontSize = 12.sp)
                    TrendChart(history.map { it.ramUsedPercent.toFloat() }, Blue, Modifier.fillMaxWidth().height(92.dp))
                    Text(finding.details.first(), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                       modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier.clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = Blue, modifier = Modifier.size(22.dp))
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 10.sp, maxLines = 2)
        }
    }
}

@Composable
private fun ToolsScreen(navigate: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("DEVICE TOOLBOX", "Explore your phone", "More than 10 dedicated pages for monitoring and device care") }
        item { QuickTile("Battery dashboard", "Charge level, estimated time, temperature and health status", "⚡") { navigate(1) } }
        item { QuickTile("Storage analyzer", "Break down occupied storage by media and app data", "▤") { navigate(2) } }
        item { QuickTile("Memory monitor", "RAM used, available and recent trend", "▦") { navigate(3) } }
        item { QuickTile("CPU monitor", "Device Care process activity and usage history", "◉") { navigate(4) } }
        item { QuickTile("Device information", "Battery, display-independent hardware readings", "▣") { navigate(6) } }
        item { QuickTile("Installed apps", "Open Android app manager to inspect app storage and permissions", "▧") { navigate(7) } }
        item { QuickTile("Battery care guide", "Battery usage tips and live charging information", "♡") { navigate(8) } }
        item { QuickTile("Storage care", "Review storage and open Android's system storage manager", "⌫") { navigate(9) } }
        item { QuickTile("Network settings", "Open Wi-Fi settings and manage connections", "⌁") { navigate(12) } }
        item { QuickTile("AI health insights", "On-device resource pattern analysis", "✦") { navigate(10) } }
        item { QuickTile("Performance history", "Review recent battery, RAM and storage trends", "⌁") { navigate(11) } }
        item { QuickTile("Privacy & permissions", "Understand what this app reads and stores", "◇") { navigate(13) } }
        item { QuickTile("System settings", "Open Android settings", "⚙") { navigate(14) } }
    }
}

@Composable
private fun BatteryScreen(snapshot: DeviceSnapshot, history: List<DeviceSnapshot>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("BATTERY DASHBOARD", "Battery life, clearly", "Live battery readings refresh every five seconds") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(154.dp)) {
                            drawArc(Muted.copy(alpha = .2f), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(14.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(Mint, -90f, snapshot.batteryPercent * 3.6f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(14.dp.toPx(), cap = StrokeCap.Round))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${snapshot.batteryPercent}%", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                            Text(if (snapshot.isCharging) "Charging" else "On battery", color = Muted, fontSize = 12.sp)
                        }
                    }
                    Text(batteryTimeRemaining(snapshot), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Runtime is an estimate based on the current battery charge counter and power draw when the phone exposes those readings.", color = Muted, fontSize = 11.sp)
                }
            }
        }
        item { InfoCard("Battery temperature", "${String.format(Locale.US, "%.1f", snapshot.batteryTemperatureC)} °C", "Reported by Android battery sensor") }
        item { InfoCard("Battery voltage", if (snapshot.batteryVoltageMv > 0) "${snapshot.batteryVoltageMv} mV" else "Unavailable", "Live battery voltage") }
        item { InfoCard("Current draw", if (snapshot.batteryCurrentUa != 0L) "${String.format(Locale.US, "%.0f", kotlin.math.abs(snapshot.batteryCurrentUa) / 1000.0)} mA" else "Not reported", "Power draw can vary with screen, signal and apps") }
        item { InfoCard("Charge counter", if (snapshot.chargeCounterUah > 0) "${snapshot.chargeCounterUah} mAh" else "Not reported", "Remaining charge reported by the device") }
        item { InfoCard("Battery condition", batteryHealthLabel(snapshot.batteryHealth), "Android-reported status; not a measured capacity-health percentage") }
        item { InfoCard("Battery technology", snapshot.batteryTechnology, "Reported by Android") }
        item { HistoryChartCard("Battery percentage trend", history.map { it.batteryPercent.toFloat() }, Mint) }
        item { Text("Important: Android does not expose a universal battery-health percentage or guaranteed time-to-empty for every phone. The remaining-time figure is approximate and may be unavailable on some devices.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp) }
    }
}

@Composable
private fun StorageScreen(snapshot: DeviceSnapshot, storage: StorageBreakdown, refresh: () -> Unit,
                          openCare: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("STORAGE ANALYZER", "Where is your space going?", "Internal storage and accessible media categories") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${snapshot.storageUsedPercent}% USED", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${String.format(Locale.US, "%.2f", snapshot.storageUsedGb)} GB used", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("${String.format(Locale.US, "%.2f", (snapshot.storageTotalGb - snapshot.storageUsedGb).coerceAtLeast(0.0))} GB available of ${String.format(Locale.US, "%.2f", snapshot.storageTotalGb)} GB", color = Muted, fontSize = 12.sp)
                    LinearProgressIndicator(progress = { snapshot.storageUsedPercent / 100f }, modifier = Modifier.fillMaxWidth().height(9.dp), color = Mint, trackColor = Navy)
                }
            }
        }
        item { StorageRow("Photos", storage.photosBytes, "Images visible to Device Care") }
        item { StorageRow("Videos", storage.videosBytes, "Videos visible to Device Care") }
        item { StorageRow("Audio", storage.audioBytes, "Music and audio visible to Device Care") }
        item { StorageRow("Device Care app data", storage.appDataBytes, "Private files belonging to this app") }
        item { StorageRow("Device Care cache", storage.appCacheBytes, "Temporary files belonging to this app") }
        item { StorageRow("System & other", storage.otherBytes, "Remaining used space not classified above; includes apps and protected system data") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = refresh, modifier = Modifier.weight(1f)) { Text("Refresh breakdown") }
                Button(onClick = openCare, modifier = Modifier.weight(1f)) { Text("Storage settings") }
            }
        }
        item { Text("Media categories require Android media permission. A category shown as 'Not accessible' is protected by Android privacy controls. System and other is a calculated remainder, not a direct list of every file.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp) }
    }
}

@Composable
private fun StorageRow(label: String, bytes: Long?, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(if (bytes == null) "Not accessible" else formatBytes(bytes), color = Mint, fontWeight = FontWeight.Bold)
            }
            Text(detail, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MemoryScreen(snapshot: DeviceSnapshot, history: List<DeviceSnapshot>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("MEMORY MONITOR", "RAM usage", "System memory information reported by Android") }
        item { InfoCard("RAM in use", "${snapshot.ramUsedMb} MB", "Estimated total memory minus available memory") }
        item { InfoCard("Total RAM", "${snapshot.ramTotalMb} MB", "Physical memory reported by Android") }
        item { InfoCard("Available RAM", "${(snapshot.ramTotalMb - snapshot.ramUsedMb).coerceAtLeast(0)} MB", "Memory currently available to the system") }
        item { InfoCard("Usage", "${snapshot.ramUsedPercent}%", "System-wide RAM usage estimate") }
        item { HistoryChartCard("RAM usage history", history.map { it.ramUsedPercent.toFloat() }, Blue) }
        item { Text("Android manages RAM automatically. This app monitors memory but does not force-stop other apps or claim to free system RAM.", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun CpuScreen(snapshot: DeviceSnapshot, history: List<DeviceSnapshot>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("PROCESSOR MONITOR", "App CPU activity", "A lightweight estimate for the Device Care process") }
        item { InfoCard("Device Care CPU", "${snapshot.appCpuPercent}%", "Process CPU estimate, not total phone CPU usage") }
        item { HistoryChartCard("CPU activity history", history.map { it.appCpuPercent.toFloat() }, Color(0xFFFFB86B)) }
        item { InfoCard("Sampling interval", "5 seconds", "Samples are collected while Device Care is open") }
        item { Text("Most Android versions restrict third-party apps from reading precise total CPU usage for the whole device. This screen intentionally reports only this app's process activity.", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun DeviceInfoScreen(snapshot: DeviceSnapshot, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("DEVICE INFORMATION", "Live hardware readings", "Values are supplied by Android and may vary by manufacturer") }
        item { InfoCard("Battery level", "${snapshot.batteryPercent}%", if (snapshot.isCharging) "Connected to power" else "Not charging") }
        item { InfoCard("Battery temperature", "${snapshot.batteryTemperatureC} °C", "Sensor reading") }
        item { InfoCard("Battery voltage", "${snapshot.batteryVoltageMv} mV", "Electrical voltage") }
        item { InfoCard("Battery technology", snapshot.batteryTechnology, "Battery type reported by OS") }
        item { InfoCard("RAM", "${snapshot.ramTotalMb} MB", "System memory") }
        item { InfoCard("Internal storage", "${String.format(Locale.US, "%.2f", snapshot.storageTotalGb)} GB", "Data partition capacity reported by Android") }
        item { InfoCard("Android version", Build.VERSION.RELEASE ?: "Unknown", "SDK level ${Build.VERSION.SDK_INT}") }
        item { InfoCard("Manufacturer / model", "${Build.MANUFACTURER} ${Build.MODEL}", "Device identity reported by Android") }
    }
}

@Composable
private fun AppsScreen(openSettings: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("APP MANAGER", "Manage installed apps", "Android controls access to other apps' private data") }
        item { InfoCard("App storage", "View per-app usage", "See each app's storage, cache and permissions in Android settings") }
        item { InfoCard("Permissions", "Review access", "Check camera, location, microphone and media access") }
        item { Button(onClick = openSettings, modifier = Modifier.fillMaxWidth()) { Text("Open Android app manager") } }
        item { Text("For privacy and security, Device Care does not scan another app's private files or clear another app's cache automatically.", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun BatteryCareScreen(snapshot: DeviceSnapshot, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("BATTERY CARE", "Make each charge count", "Practical guidance based on current readings") }
        item { InfoCard("Current charge", "${snapshot.batteryPercent}%", if (snapshot.isCharging) "Charging now" else "Running on battery") }
        item { InfoCard("Estimated runtime", batteryTimeRemaining(snapshot), "Approximation only; actual runtime changes with usage") }
        item { GuideCard("Reduce screen brightness", "The display can be a major power user. Use adaptive brightness or a lower comfortable level.") }
        item { GuideCard("Review background activity", "Open Android battery settings to identify apps with unusually high battery use.") }
        item { GuideCard("Avoid excess heat", "High temperatures can accelerate battery ageing. Keep the phone ventilated while charging.") }
        item { GuideCard("Use Battery Saver", "Enable Android Battery Saver when you need longer runtime away from a charger.") }
        item { Text("Battery percentage is the remaining charge level. It is not the same as long-term battery capacity health.", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun StorageCareScreen(openSettings: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("STORAGE CARE", "Free space safely", "Use Android's built-in storage controls") }
        item { GuideCard("Review large videos", "Back up videos you want to keep, then remove unwanted copies using your gallery app.") }
        item { GuideCard("Check downloads", "Delete old installation files and documents only when you no longer need them.") }
        item { GuideCard("Review app cache", "Android settings can show app cache. Clear it only when you understand the app may rebuild temporary files.") }
        item { GuideCard("Back up before deleting", "Keep a backup of important photos and documents before removing anything.") }
        item { Button(onClick = openSettings, modifier = Modifier.fillMaxWidth()) { Text("Open Android storage settings") } }
        item { Text("Device Care never deletes personal files automatically. Android restricts access to other apps' private folders.", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun NetworkScreen(openSettings: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("NETWORK TOOLS", "Connection controls", "Open your phone's network settings") }
        item { GuideCard("Wi-Fi", "Connect to trusted networks and review saved network settings.") }
        item { GuideCard("Mobile data", "Review data usage and background data restrictions in Android settings.") }
        item { GuideCard("Airplane mode", "Use airplane mode when you need to disable wireless radios temporarily.") }
        item { Button(onClick = openSettings, modifier = Modifier.fillMaxWidth()) { Text("Open Wi-Fi settings") } }
        item { Text("Live network speed and per-app data usage are not currently measured by this version.", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun PrivacyScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("PRIVACY CENTER", "Your data stays on device", "Device Care does not upload device telemetry") }
        item { GuideCard("Local processing", "Resource readings and TensorFlow Lite predictions are processed locally on your phone.") }
        item { GuideCard("Media permission", "Optional access lets the storage page total photos, videos and audio visible to Android. You can deny it.") }
        item { GuideCard("No automatic cleanup", "The app does not delete personal files or force-stop other applications.") }
        item { GuideCard("Session history", "Performance samples are kept in app memory during the current session and reset when the process ends.") }
    }
}

@Composable
private fun SettingsScreen(openSettings: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("SETTINGS", "Device Care preferences", "System-level controls are managed by Android") }
        item { InfoCard("Refresh interval", "5 seconds", "Live readings while the app is open") }
        item { InfoCard("AI processing", "On device", "TensorFlow Lite when the model asset is bundled") }
        item { InfoCard("App version", "1.2.0", "Device Care Android") }
        item { Button(onClick = openSettings, modifier = Modifier.fillMaxWidth()) { Text("Open Android settings") } }
    }
}

@Composable
private fun InsightsScreen(finding: HealthFinding, prediction: ModelPrediction, snapshot: DeviceSnapshot, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader("AI INSIGHTS", "Understand your device", "Predictions run locally. No device telemetry is sent to a server.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MODEL RESULT", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(predictionLabel(prediction), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (prediction.modelAvailable) {
                        Text("Model confidence: ${prediction.confidence}%", color = Muted, fontSize = 12.sp)
                        listOf("Healthy", "Monitor", "High risk").forEachIndexed { index, label ->
                            val value = prediction.probabilities.getOrElse(index) { 0f }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, fontSize = 13.sp); Text("${(value * 100).toInt()}%", color = Muted, fontSize = 12.sp)
                                }
                                LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth(), color = if (index == 2) Color(0xFFFF7777) else Mint, trackColor = Navy)
                            }
                        }
                    } else Text("Rule-based insights are shown until the trained model asset is bundled.", color = Muted, fontSize = 12.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("RECOMMENDATIONS", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    finding.details.forEachIndexed { index, detail ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Text("${index + 1}", color = Mint, fontWeight = FontWeight.Bold); Text(detail, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
        item { InfoCard("Battery", "${snapshot.batteryPercent}%", batteryTimeRemaining(snapshot)) }
        item { InfoCard("RAM", "${snapshot.ramUsedPercent}%", "${snapshot.ramUsedMb} / ${snapshot.ramTotalMb} MB") }
        item { InfoCard("Storage", "${snapshot.storageUsedPercent}%", "${String.format(Locale.US, "%.1f / %.1f GB", snapshot.storageUsedGb, snapshot.storageTotalGb)}") }
        item { Text("Research limitation: this model learns from generated scenarios labelled by resource thresholds. Its confidence is not a validated probability of hardware failure.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp) }
    }
}

@Composable
private fun HistoryScreen(history: List<DeviceSnapshot>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader("PERFORMANCE HISTORY", "Your recent device trends", "Samples are collected every five seconds while the app is open. Session history resets when the app process ends.") }
        item { HistoryChartCard("Battery level", history.map { it.batteryPercent.toFloat() }, Mint) }
        item { HistoryChartCard("RAM usage", history.map { it.ramUsedPercent.toFloat() }, Blue) }
        item { HistoryChartCard("Storage usage", history.map { it.storageUsedPercent.toFloat() }, Color(0xFFFFB86B)) }
        item { HistoryChartCard("Device Care CPU", history.map { it.appCpuPercent.toFloat() }, Color(0xFFB78BFA)) }
        item {
            val first = history.firstOrNull(); val last = history.lastOrNull()
            val delta = if (first != null && last != null) last.batteryPercent - first.batteryPercent else 0
            InfoCard("Battery session change", "${if (delta >= 0) "+" else ""}$delta percentage points", "Observed during this app session; not a battery runtime prediction")
        }
    }
}

@Composable
private fun PageHeader(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(eyebrow, color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun QuickTile(title: String, subtitle: String, symbol: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(Modifier.size(42.dp).background(Blue.copy(alpha = .18f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Text(symbol, color = Mint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Text("›", color = Mint, fontSize = 24.sp)
        }
    }
}

@Composable
private fun InfoCard(title: String, value: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Muted, fontSize = 11.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun GuideCard(title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Mint, fontWeight = FontWeight.SemiBold)
            Text(detail, color = White, fontSize = 13.sp, lineHeight = 19.sp)
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
            val points = values.mapIndexed { index, value -> Offset(index * step, size.height * (1f - value.coerceIn(0f, 100f) / 100f)) }
            points.zipWithNext().forEach { (a, b) -> drawLine(lineColor, a, b, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round) }
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

private fun batteryHealthLabel(health: Int): String = when (health) {
    android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
    android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
    android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
    android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
    android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
    android.os.BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
    else -> "Not reported"
}
