#!/usr/bin/env python3
"""
Train a small TensorFlow/Keras resource-risk classifier and export TFLite.
The training examples are generated from transparent heuristic scenarios.
They are suitable for a reproducible academic prototype, not clinical or
production-grade device-failure prediction.
"""
from pathlib import Path
import argparse
import numpy as np
import tensorflow as tf

def make_dataset(samples: int, seed: int = 42):
    rng = np.random.default_rng(seed)
    battery = rng.uniform(0.03, 1.0, samples)
    ram = rng.uniform(0.10, 0.99, samples)
    storage = rng.uniform(0.10, 0.99, samples)
    cpu = rng.uniform(0.0, 1.0, samples)
    charging = rng.integers(0, 2, samples).astype(np.float32)

    # Synthetic labels: 0 healthy, 1 monitor, 2 high resource risk.
    pressure = (
        (ram > 0.88).astype(int)
        + (storage > 0.90).astype(int)
        + ((battery < 0.12) & (charging < 0.5)).astype(int)
        + (cpu > 0.82).astype(int)
    )
    labels = np.where(pressure >= 2, 2, np.where(pressure == 1, 1, 0))
    x = np.column_stack([battery, ram, storage, cpu, charging]).astype(np.float32)
    y = tf.keras.utils.to_categorical(labels, num_classes=3).astype(np.float32)
    order = rng.permutation(samples)
    return x[order], y[order]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="app/src/main/assets/device_health_model.tflite")
    parser.add_argument("--samples", type=int, default=30000)
    args = parser.parse_args()

    tf.keras.utils.set_random_seed(42)
    x, y = make_dataset(args.samples)
    split = int(len(x) * 0.85)
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(5,), name="resource_features"),
        tf.keras.layers.Dense(24, activation="relu"),
        tf.keras.layers.Dense(16, activation="relu"),
        tf.keras.layers.Dense(3, activation="softmax", name="risk_probabilities"),
    ])
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )
    model.fit(
        x[:split], y[:split],
        validation_data=(x[split:], y[split:]),
        epochs=18,
        batch_size=128,
        verbose=2,
        callbacks=[tf.keras.callbacks.EarlyStopping(
            monitor="val_loss", patience=3, restore_best_weights=True
        )],
    )
    loss, accuracy = model.evaluate(x[split:], y[split:], verbose=0)
    print(f"Synthetic validation accuracy: {accuracy:.4f}; loss: {loss:.4f}")

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite = converter.convert()
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(tflite)
    print(f"Wrote {len(tflite)} bytes to {output}")

if __name__ == "__main__":
    main()
