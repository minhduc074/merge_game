"""Synthesize SFX wavs (16-bit mono 22050Hz, stdlib only).

Currently regenerates the merge sounds: a bubble "pop" followed by a
short glockenspiel pentatonic arpeggio. Three transposed variants
(merge1/2/3) give variety so repeated merges don't sound identical.
"""

import math
import random
import struct
import wave
from pathlib import Path

SR = 22050
OUT = Path(__file__).resolve().parent.parent / "app/src/main/res/raw"


def write_wav(name: str, samples: list[float]) -> None:
    path = OUT / name
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(
            struct.pack("<h", max(-32767, min(32767, int(s * 32767))))
            for s in samples
        ))
    print(f"{name}: {len(samples)/SR:.2f}s")


def pop(t: float) -> float:
    """Bubble pop: sine sweeping 200->540Hz, ~75ms, with a click of noise."""
    d = 0.075
    if t >= d:
        return 0.0
    phase = 2 * math.pi * (200 * t + (540 - 200) / 2 / d * t * t)
    env = math.exp(-t * 45)
    noise = random.uniform(-1, 1) * 0.15 if t < 0.006 else 0.0
    return (math.sin(phase) * 0.9 + noise) * env


def chime(t: float, freq: float, dur: float = 0.42) -> float:
    """Glockenspiel note: fundamental + harmonics, exponential decay."""
    if t >= dur:
        return 0.0
    env = math.exp(-t * 8.5)
    attack = min(1.0, t / 0.005)
    return (
        math.sin(2 * math.pi * freq * t) * 0.55
        + math.sin(2 * math.pi * freq * 2 * t) * 0.28
        + math.sin(2 * math.pi * freq * 3 * t) * 0.12
        + math.sin(2 * math.pi * freq * 4.2 * t) * 0.05
    ) * env * attack


def merge(root_hz: float) -> list[float]:
    """Pop + pentatonic arpeggio (1 - 3 - 5) starting ~60ms in."""
    ratios = [1.0, 1.25, 1.5]  # major triad, bright and happy
    starts = [0.06, 0.14, 0.22]
    total = int(SR * 0.62)
    out = []
    for i in range(total):
        t = i / SR
        s = pop(t) * 0.9
        for r, st in zip(ratios, starts):
            if t >= st:
                s += chime(t - st, root_hz * r) * 0.55
        out.append(s)
    peak = max(abs(s) for s in out) or 1.0
    return [s / peak * 0.85 for s in out]


if __name__ == "__main__":
    # C6, D6, A5 roots -> cute, slightly different flavors
    for name, root in [("merge1.wav", 1046.5), ("merge2.wav", 1174.7), ("merge3.wav", 880.0)]:
        write_wav(name, merge(root))
