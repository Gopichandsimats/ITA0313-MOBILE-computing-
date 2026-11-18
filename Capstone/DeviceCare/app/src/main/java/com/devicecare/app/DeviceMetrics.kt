package com.devicecare.app

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.ActivityManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
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
    val capturedAt: Long = System.currentTimeMillis()
)

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
            appCpuPercent = cpuPercent.coerceIn(0, 100)
        )
    }
}
