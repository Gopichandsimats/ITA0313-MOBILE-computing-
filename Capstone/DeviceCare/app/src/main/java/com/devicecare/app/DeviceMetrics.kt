package com.devicecare.app

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.ActivityManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class DeviceSnapshot(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val ramUsedPercent: Int,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val storageUsedPercent: Int,
    val storageUsedGb: Double,
    val storageTotalGb: Double,
    val appCpuPercent: Int,
    val batteryCurrentUa: Long = 0L,
    val chargeCounterUah: Long = 0L,
    val batteryTemperatureC: Float = 0f,
    val batteryVoltageMv: Int = 0,
    val batteryHealth: Int = 0,
    val batteryTechnology: String = "Unknown",
    val capturedAt: Long = System.currentTimeMillis()
)

data class StorageBreakdown(
    val photosBytes: Long?,
    val videosBytes: Long?,
    val audioBytes: Long?,
    val appDataBytes: Long,
    val appCacheBytes: Long,
    val totalUsedBytes: Long,
    val totalBytes: Long
) {
    val knownBytes: Long get() = (photosBytes ?: 0L) + (videosBytes ?: 0L) +
        (audioBytes ?: 0L) + appDataBytes + appCacheBytes
    val otherBytes: Long get() = (totalUsedBytes - knownBytes).coerceAtLeast(0L)
}

class DeviceMetrics(private val context: Context) {
    private var previousCpuTime = android.os.Process.getElapsedCpuTime()
    private var previousWallTime = System.currentTimeMillis()

    fun capture(): DeviceSnapshot {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val battery = if (level >= 0 && scale > 0) (level * 100f / scale).toInt() else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val current = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val chargeCounter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val temperature = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) ?: 0
        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memory)
        val totalRam = memory.totalMem / (1024L * 1024L)
        val availableRam = memory.availMem / (1024L * 1024L)
        val usedRam = max(0L, totalRam - availableRam)
        val ramPercent = if (totalRam > 0) (usedRam * 100 / totalRam).toInt() else 0

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val availableBytes = stat.availableBytes
        val usedBytes = max(0L, totalBytes - availableBytes)
        val totalGb = totalBytes / 1_073_741_824.0
        val usedGb = usedBytes / 1_073_741_824.0
        val storagePercent = if (totalBytes > 0) (usedBytes * 100 / totalBytes).toInt() else 0

        val now = System.currentTimeMillis()
        val cpuNow = android.os.Process.getElapsedCpuTime()
        val wallDelta = max(1L, now - previousWallTime)
        val cpuDelta = max(0L, cpuNow - previousCpuTime)
        val cpuPercent = min(100, (cpuDelta * 100 / wallDelta).toInt())
        previousCpuTime = cpuNow
        previousWallTime = now

        return DeviceSnapshot(
            batteryPercent = battery.coerceIn(0, 100),
            isCharging = charging,
            ramUsedPercent = ramPercent.coerceIn(0, 100),
            ramUsedMb = usedRam,
            ramTotalMb = totalRam,
            storageUsedPercent = storagePercent.coerceIn(0, 100),
            storageUsedGb = usedGb,
            storageTotalGb = totalGb,
            appCpuPercent = cpuPercent.coerceIn(0, 100),
            batteryCurrentUa = current,
            chargeCounterUah = chargeCounter,
            batteryTemperatureC = temperature,
            batteryVoltageMv = voltage,
            batteryHealth = health,
            batteryTechnology = technology
        )
    }

    /** Media categories are reported only when Android grants access; inaccessible categories stay null. */
    fun storageBreakdown(): StorageBreakdown {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val used = (total - stat.availableBytes).coerceAtLeast(0L)
        fun mediaSize(uri: android.net.Uri): Long? = try {
            var sum = 0L
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                if (index >= 0) while (cursor.moveToNext()) sum += cursor.getLong(index).coerceAtLeast(0L)
            }
            sum
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
        fun directorySize(file: java.io.File?): Long {
            if (file == null || !file.exists()) return 0L
            return try { file.walkTopDown().filter { it.isFile }.sumOf { it.length() } } catch (_: Exception) { 0L }
        }
        val appData = directorySize(context.filesDir) + directorySize(context.noBackupFilesDir)
        val appCache = directorySize(context.cacheDir) + directorySize(context.externalCacheDir)
        return StorageBreakdown(
            photosBytes = mediaSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            videosBytes = mediaSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            audioBytes = mediaSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            appDataBytes = appData,
            appCacheBytes = appCache,
            totalUsedBytes = used,
            totalBytes = total
        )
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "Unavailable"
    val gb = bytes / 1_073_741_824.0
    val mb = bytes / 1_048_576.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.0f MB", mb)
        else -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1024.0)
    }
}

fun batteryTimeRemaining(snapshot: DeviceSnapshot): String {
    if (snapshot.isCharging) return "Charging — remaining time updates after unplugging"
    val current = abs(snapshot.batteryCurrentUa)
    if (snapshot.chargeCounterUah > 0L && current > 100L) {
        val hours = snapshot.chargeCounterUah.toDouble() / current.toDouble()
        if (hours.isFinite() && hours in 0.02..100.0) {
            val minutes = (hours * 60).toLong()
            return "About ${minutes / 60}h ${minutes % 60}m at current drain"
        }
    }
    return "Estimating — keep the app open while unplugged to collect a discharge trend"
}
