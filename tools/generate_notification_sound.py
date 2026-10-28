#!/usr/bin/env python3
"""Generate SamPlay's original three-note notification chime (no sampled audio)."""
import math
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 44100
DURATION = 0.9
NOTES = ((0.00, 523.25, 0.46), (0.13, 659.25, 0.43), (0.26, 783.99, 0.55))
OUTPUT = Path(__file__).resolve().parents[1] / "app/src/main/res/raw/samplay_notification.wav"

frames = bytearray()
for i in range(round(SAMPLE_RATE * DURATION)):
    t = i / SAMPLE_RATE
    sample = 0.0
    for start, frequency, length in NOTES:
        elapsed = t - start
        if 0 <= elapsed < length:
            attack = min(1.0, elapsed / 0.012)
            release = min(1.0, (length - elapsed) / 0.08)
            envelope = attack * release * math.exp(-4.0 * elapsed / length)
            tone = math.sin(2 * math.pi * frequency * elapsed)
            tone += 0.25 * math.sin(2 * math.pi * frequency * 2.01 * elapsed)
            sample += 0.22 * envelope * tone
    sample = max(-1.0, min(1.0, sample))
    frames.extend(struct.pack("<h", round(sample * 32767)))

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
with wave.open(str(OUTPUT), "wb") as wav:
    wav.setnchannels(1)
    wav.setsampwidth(2)
    wav.setframerate(SAMPLE_RATE)
    wav.writeframes(frames)
print(f"Wrote {OUTPUT}")
