package com.devicecare.app

import kotlin.math.sqrt

enum class HealthLevel { GOOD, WATCH, ACTION }

data class HealthFinding(
    val level: HealthLevel,
    val score: Int,
    val headline: String,
    val details: List<String>
)

/**
 * Lightweight on-device, history-based anomaly detector.
 * It compares current resource readings with a rolling baseline using robust
 * deviation scores, then adds transparent safety limits for actionable advice.
 * It does not upload telemetry or claim to be a medical/benchmark-grade model.
 */
class AnomalyDetector(private val windowSize: Int = 20) {
    private val ramHistory = ArrayDeque<Double>()
    private val storageHistory = ArrayDeque<Double>()
    private val batteryHistory = ArrayDeque<Double>()

    fun analyze(snapshot: DeviceSnapshot): HealthFinding {
        val ram = snapshot.ramUsedPercent.toDouble()
        val storage = snapshot.storageUsedPercent.toDouble()
        val battery = snapshot.batteryPercent.toDouble()

        val ramDeviation = deviation(ram, ramHistory)
        val storageDeviation = deviation(storage, storageHistory)
        val batteryDeviation = deviation(battery, batteryHistory)

        add(ramHistory, ram)
        add(storageHistory, storage)
        add(batteryHistory, battery)

        var risk = 0
        val details = mutableListOf<String>()

        if (ram >= 90) {
            risk += 35
            details += "RAM pressure is high. Close unused apps and review background activity."
        } else if (ram >= 80 || ramDeviation >= 2.5) {
            risk += 20
            details += "RAM usage is elevated compared with the recent device baseline."
        }
        if (storage >= 90) {
            risk += 35
            details += "Storage is nearly full. Remove files or apps you no longer need."
        } else if (storage >= 80 || storageDeviation >= 2.5) {
            risk += 18
            details += "Storage usage is high; keeping free space can help updates and app operations."
        }
        if (battery <= 15 && !snapshot.isCharging) {
            risk += 25
            details += "Battery is low. Enable Battery Saver or connect a charger."
        } else if (batteryDeviation >= 3.0 && !snapshot.isCharging) {
            risk += 10
            details += "Battery level changed faster than its recent baseline; check battery usage settings."
        }
        if (snapshot.appCpuPercent >= 80) {
            risk += 15
            details += "This app's recent CPU activity is high. The value represents Device Care process usage, not total system CPU."
        }

        val score = (100 - risk).coerceIn(0, 100)
        val level = when {
            risk >= 45 -> HealthLevel.ACTION
            risk >= 20 -> HealthLevel.WATCH
            else -> HealthLevel.GOOD
        }
        val headline = when (level) {
            HealthLevel.GOOD -> "Your device looks healthy"
            HealthLevel.WATCH -> "A few resources need attention"
            HealthLevel.ACTION -> "Resource pressure detected"
        }
        if (details.isEmpty()) {
            details += "No major resource warning was detected in the available readings."
            details += "Keep apps updated and review battery usage periodically."
        }
        return HealthFinding(level, score, headline, details)
    }

    private fun deviation(value: Double, history: ArrayDeque<Double>): Double {
        if (history.size < 5) return 0.0
        val mean = history.average()
        val variance = history.map { (it - mean) * (it - mean) }.average()
        val sd = sqrt(variance).coerceAtLeast(1.0)
        return kotlin.math.abs(value - mean) / sd
    }

    private fun add(history: ArrayDeque<Double>, value: Double) {
        history.addLast(value)
        while (history.size > windowSize) history.removeFirst()
    }
}
