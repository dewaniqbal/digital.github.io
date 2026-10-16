#!/usr/bin/env python3
"""
Procedurally synthesises the optional background ("ambient") sound loops.

Every sound is generated from mathematical noise/oscillators by this script, so the
resulting audio is an original work of this project (released under CC0, see
docs/AUDIO_LICENSING.md). No third-party recordings are used.

Usage:  python3 tools/generate_ambient_sounds.py  [out_dir]
Requires: numpy, scipy, ffmpeg (with libvorbis) on PATH.
"""
import os, subprocess, sys, tempfile, wave
import numpy as np
from scipy import signal

SR = 32000            # mono 32 kHz is plenty for ambience and keeps the APK small
LOOP_S = 40           # loop length in seconds
XFADE_S = 3           # crossfade used to make the loop seamless
PEAK_DBFS = -6.0      # head-room so Quran + ambience never clip when mixed
rng = np.random.default_rng(20261005)   # fixed seed => reproducible output

N = int(SR * (LOOP_S + XFADE_S))
t = np.arange(N) / SR


def white(n=N): return rng.standard_normal(n)
def lp(x, fc, order=2): return signal.sosfilt(signal.butter(order, fc, 'low', fs=SR, output='sos'), x)
def hp(x, fc, order=2): return signal.sosfilt(signal.butter(order, fc, 'high', fs=SR, output='sos'), x)
def bp(x, lo, hi, order=2): return signal.sosfilt(signal.butter(order, [lo, hi], 'band', fs=SR, output='sos'), x)
def pink(n=N):
    f = np.fft.rfftfreq(n, 1 / SR); f[0] = 1
    s = np.fft.rfft(white(n)) / np.sqrt(f)
    return norm(np.fft.irfft(s, n))
def brown(n=N): return norm(hp(np.cumsum(white(n)), 15))
def norm(x): return x / (np.max(np.abs(x)) + 1e-9)
def smooth_noise(rate_hz, n=N):
    """slowly varying random curve in [0,1]"""
    k = max(4, int(n / SR * rate_hz) + 4)
    pts = rng.random(k)
    return np.interp(np.linspace(0, k - 3, n), np.arange(k), pts)
def env(n, a, d):
    """attack/exponential-decay envelope, lengths in seconds"""
    na = max(1, int(a * SR)); e = np.exp(-np.arange(n) / max(1, d * SR))
    e[:na] *= np.linspace(0, 1, na); return e
def add(dst, src, at):
    at = int(at); end = min(len(dst), at + len(src))
    if at < len(dst): dst[at:end] += src[:end - at]
def tone(freqs, amps=None):
    """sine with time-varying frequency curve (Hz array)"""
    ph = 2 * np.pi * np.cumsum(freqs) / SR
    return np.sin(ph) if amps is None else np.sin(ph) * amps


def rain():
    base = bp(pink(), 400, 9000) * 0.6 + hp(white(), 3000) * 0.15
    base *= 0.8 + 0.2 * smooth_noise(0.2)
    drops = np.zeros(N)
    for _ in range(int(LOOP_S * 25)):
        n = int(SR * rng.uniform(0.004, 0.02))
        d = bp(white(n), rng.uniform(1500, 3500), 8000) * env(n, 0.0005, 0.004) * rng.uniform(0.2, 1)
        add(drops, d, rng.integers(0, N))
    return base + 0.5 * drops

def thunder_layer(count, quiet=0.0):
    out = np.zeros(N) + quiet * lp(brown(), 120)
    times = np.sort(rng.uniform(1, LOOP_S - 8, count))
    for at in times:
        n = int(SR * rng.uniform(5, 8))
        roll = lp(brown(n), rng.uniform(150, 300)) * env(n, rng.uniform(0.05, 0.4), rng.uniform(1.2, 2.2))
        roll *= 0.6 + 0.4 * smooth_noise(3, n)
        crack = hp(white(n), 800) * env(n, 0.002, 0.08) * 0.25
        add(out, (roll + crack) * rng.uniform(0.6, 1.0), at * SR)
    return out

def thunder(): return thunder_layer(3, quiet=0.08)
def thunderstorm(): return 0.8 * norm(rain()) + 0.9 * norm(thunder_layer(2))

def wind():
    gust = smooth_noise(0.12) ** 1.5
    x = lp(0.7 * brown() + 0.3 * pink(), 900) * (0.25 + 0.75 * gust)
    whistle = bp(white(), 600, 1400, 4) * (gust ** 3) * 0.3
    return x + whistle

def waves():
    out = np.zeros(N); at = 0.0
    surf = bp(pink(), 150, 6000)
    while at < LOOP_S + XFADE_S:
        period = rng.uniform(7, 10)
        n = int(SR * period)
        ph = np.linspace(0, 1, n)
        swell = np.sin(np.pi * ph) ** 2 * (ph < 0.6) + (ph >= 0.6) * np.exp(-(ph - 0.6) * 6) * np.sin(np.pi * 0.6) ** 2
        add(out, swell * rng.uniform(0.7, 1.0), at * SR)
        at += period * rng.uniform(0.85, 1.0)
    return surf * (0.15 + out) + 0.3 * lp(brown(), 200)

def river():
    flow = bp(pink(), 250, 5000) * (0.85 + 0.15 * smooth_noise(0.5))
    bub = np.zeros(N)
    for _ in range(int(LOOP_S * 18)):
        n = int(SR * rng.uniform(0.02, 0.07))
        f0 = rng.uniform(500, 1600)
        f = f0 * (1 + np.linspace(0, rng.uniform(0.3, 1.0), n))
        add(bub, tone(f, env(n, 0.003, n / SR / 3)) * rng.uniform(0.05, 0.25), rng.integers(0, N))
    return flow + bub

def fire():
    bed = lp(brown(), 400) * 0.6 + bp(pink(), 300, 2500) * 0.15 * (0.6 + 0.4 * smooth_noise(1))
    cr = np.zeros(N)
    for _ in range(int(LOOP_S * 30)):
        n = int(SR * rng.uniform(0.002, 0.012))
        c = hp(white(n), rng.uniform(1500, 4000)) * env(n, 0.0002, 0.002) * rng.uniform(0.1, 1) ** 2
        add(cr, c, rng.integers(0, N))
    for _ in range(int(LOOP_S * 0.6)):   # occasional louder pops
        n = int(SR * 0.04)
        add(cr, bp(white(n), 800, 5000) * env(n, 0.0002, 0.006), rng.integers(0, N))
    return bed + 0.8 * cr

def chirp(n, f_start, f_end, harmonic=0.25):
    f = np.geomspace(f_start, f_end, n)
    e = np.sin(np.linspace(0, np.pi, n)) ** 1.5
    return (tone(f) + harmonic * tone(2 * f)) * e

def birds():
    out = 0.05 * bp(pink(), 300, 3000)
    species = [(2500, 4500, 0.05, 0.10), (3000, 6000, 0.03, 0.06), (1800, 3000, 0.12, 0.25)]
    for lo, hi, dmin, dmax in species:
        at = rng.uniform(0, 3)
        while at < LOOP_S + XFADE_S - 2:
            amp = rng.uniform(0.2, 0.5)
            for _ in range(rng.integers(2, 7)):
                n = int(SR * rng.uniform(dmin, dmax))
                a, b = rng.uniform(lo, hi), rng.uniform(lo, hi)
                add(out, chirp(n, a, b) * amp, at * SR)
                at += n / SR + rng.uniform(0.03, 0.15)
            at += rng.uniform(1.5, 5)
    return out

def crickets():
    out = 0.04 * lp(brown(), 300)
    for k in range(4):
        fc = rng.uniform(4200, 5200); amp = rng.uniform(0.15, 0.35)
        at = rng.uniform(0, 1)
        while at < LOOP_S + XFADE_S - 1:
            for p in range(rng.integers(3, 5)):  # one chirp = a few pulses
                n = int(SR * 0.018)
                add(out, np.sin(2 * np.pi * fc * np.arange(n) / SR) * np.hanning(n) * amp, (at + p * 0.03) * SR)
            at += rng.uniform(0.45, 0.8)
    return out

def owl():
    out = 0.06 * lp(brown(), 250) * (0.6 + 0.4 * smooth_noise(0.2))
    at = 2.0
    while at < LOOP_S + XFADE_S - 4:
        f0 = rng.uniform(340, 420)
        for dur, gap in [(0.35, 0.25), (0.25, 0.35), (0.9, 0.0)]:   # hoo, hoo, hooooo
            n = int(SR * dur)
            f = f0 * np.linspace(1.03, 0.95, n)
            e = np.sin(np.linspace(0, np.pi, n)) ** 0.8
            add(out, (tone(f) + 0.15 * tone(2 * f)) * e * 0.45 + lp(white(n), 900) * e * 0.03, at * SR)
            at += dur + gap
        at += rng.uniform(6, 10)
    return out

def cat():
    # purring: low-pass noise amplitude modulated at ~25 Hz with a ~2.6 s breathing cycle
    breath = 0.55 + 0.45 * np.sin(2 * np.pi * t / 2.6) ** 2
    pulses = (0.5 + 0.5 * np.sin(2 * np.pi * 25 * t)) ** 3
    body = lp(white(), 300, 4) * pulses * breath
    hum = tone(np.full(N, 50.0)) * 0.05 * breath
    return body + hum

def train():
    out = 0.5 * lp(brown(), 180) * (0.85 + 0.15 * smooth_noise(0.3))
    out += 0.08 * bp(pink(), 500, 2500)
    period = 1.35; at = 0.2
    while at < LOOP_S + XFADE_S:
        for off in (0.0, 0.18):          # clickety-clack: two wheel sets per rail joint
            n = int(SR * 0.12)
            hit = (bp(white(n), 300, 3000) * env(n, 0.0005, 0.02) + tone(np.full(n, 90.0)) * env(n, 0.001, 0.05)) * 0.6
            add(out, hit, (at + off) * SR)
        at += period
    return out

SOUNDS = dict(rain=rain, birds=birds, fire=fire, waves=waves, wind=wind, cat=cat, owl=owl,
              river=river, crickets=crickets, thunderstorm=thunderstorm, thunder=thunder, train=train)


def make_loop(x):
    """crossfade the tail onto the head so the loop is seamless"""
    L, X = int(SR * LOOP_S), int(SR * XFADE_S)
    x = x[:L + X].copy()
    fade = np.sqrt(np.linspace(0, 1, X))
    head = x[:X] * fade + x[L:L + X] * fade[::-1]
    y = x[:L].copy(); y[:X] = head
    y -= np.mean(y)
    return y / (np.max(np.abs(y)) + 1e-9) * 10 ** (PEAK_DBFS / 20)


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
    os.makedirs(out_dir, exist_ok=True)
    for name, fn in SOUNDS.items():
        y = make_loop(fn())
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
            with wave.open(tmp.name, "wb") as w:
                w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
                w.writeframes((y * 32767).astype("<i2").tobytes())
            dst = os.path.join(out_dir, f"ambient_{name}.ogg")
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp.name, "-c:a", "libvorbis", "-q:a", "2", dst], check=True)
            os.unlink(tmp.name)
        print(f"{dst}  {os.path.getsize(dst) // 1024} KB")

if __name__ == "__main__":
    main()
