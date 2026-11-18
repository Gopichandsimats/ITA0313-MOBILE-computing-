package com.devicecare.app

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class ModelRisk { HEALTHY, MONITOR, HIGH_RISK }

data class ModelPrediction(
    val risk: ModelRisk,
    val confidence: Int,
    val probabilities: List<Float>,
    val modelAvailable: Boolean
)

/** Runs the bundled TensorFlow Lite resource-risk classifier entirely on-device. */
class DeviceHealthModel(context: Context) : AutoCloseable {
    private val interpreter: Interpreter? = try {
        val bytes = context.assets.open("device_health_model.tflite").use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.rewind()
        Interpreter(buffer)
    } catch (_: Exception) {
        null
    }

    fun predict(snapshot: DeviceSnapshot): ModelPrediction {
        val model = interpreter ?: return ModelPrediction(
            ModelRisk.MONITOR, 0, emptyList(), false
        )
        val input = arrayOf(floatArrayOf(
            snapshot.batteryPercent / 100f,
            snapshot.ramUsedPercent / 100f,
            snapshot.storageUsedPercent / 100f,
            snapshot.appCpuPercent / 100f,
            if (snapshot.isCharging) 1f else 0f
        ))
        val output = Array(1) { FloatArray(3) }
        return try {
            model.run(input, output)
            val values = output[0].map { it.coerceIn(0f, 1f) }
            val best = values.indices.maxByOrNull { values[it] } ?: 0
            ModelPrediction(
                risk = when (best) {
                    0 -> ModelRisk.HEALTHY
                    1 -> ModelRisk.MONITOR
                    else -> ModelRisk.HIGH_RISK
                },
                confidence = (values[best] * 100).toInt(),
                probabilities = values,
                modelAvailable = true
            )
        } catch (_: Exception) {
            ModelPrediction(ModelRisk.MONITOR, 0, emptyList(), false)
        }
    }

    override fun close() {
        interpreter?.close()
    }
}
