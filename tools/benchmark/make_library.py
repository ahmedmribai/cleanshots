"""Builds a labelled test photo library for measuring scan speed and detection accuracy.

Usage: python tools/benchmark/make_library.py <out_dir>

Real photos come from picsum.photos (Unsplash licence). File names encode the ground truth:
  u_<id>_<k>.jpg   unique photo (k = variant: original, mirror, crops)
  d_<id>_<k>.jpg   exact-duplicate set: a original, b re-encoded, c half-size
  n_<id>_<k>.jpg   near-duplicate set: a original, b cropped 3 % and brightened
  b_<id>.jpg       blurred (should be flagged blurry)
  k_<id>.jpg       dark but sharp (must NOT be flagged blurry)
  Screenshot_*.png synthetic screenshots (Pictures/Screenshots)
  big_*.mp4 / small_*.mp4 large and small videos (Movies)
"""
import io
import json
import os
import random
import subprocess
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageOps

W, H = 1200, 900
random.seed(7)


def fetch(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return r.read()


def photo_ids():
    ids = []
    for page in range(1, 12):
        data = json.loads(fetch(f"https://picsum.photos/v2/list?page={page}&limit=100"))
        ids += [d["id"] for d in data]
    return ids


def download(cache, pid):
    path = os.path.join(cache, f"{pid}.jpg")
    if not os.path.exists(path):
        try:
            data = fetch(f"https://picsum.photos/id/{pid}/{W}/{H}.jpg")
            with open(path, "wb") as f:
                f.write(data)
        except Exception as e:  # a few ids have been removed upstream
            print("skip", pid, e)
            return None
    return path


def save(img, path, quality=88):
    img.convert("RGB").save(path, "JPEG", quality=quality)


def crop_scaled(img, box):
    return img.crop(box).resize((W, H), Image.LANCZOS)


def screenshot(path, i):
    w, h = 1080, 2340
    bg = random.choice([(255, 255, 255), (18, 18, 18), (245, 246, 250)])
    fg = (30, 30, 30) if sum(bg) > 300 else (230, 230, 230)
    img = Image.new("RGB", (w, h), bg)
    d = ImageDraw.Draw(img)
    accent = tuple(random.randint(40, 220) for _ in range(3))
    d.rectangle([0, 0, w, 120], fill=accent)
    y = 180
    while y < h - 200:
        kind = random.random()
        if kind < 0.5:
            for _ in range(random.randint(1, 4)):
                d.rectangle([60, y, 60 + random.randint(300, 900), y + 28], fill=fg)
                y += 50
        elif kind < 0.8:
            d.rounded_rectangle([40, y, w - 40, y + 300], 30, fill=tuple(min(255, c + 25) for c in bg), outline=accent, width=3)
            y += 340
        else:
            d.ellipse([60, y, 180, y + 120], fill=accent)
            d.rectangle([220, y + 30, 800, y + 60], fill=fg)
            y += 160
        y += 30
    img.save(path, "PNG", optimize=True)


def video(path, seconds, bitrate):
    subprocess.run([
        "ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi", "-i", "testsrc2=size=1920x1080:rate=30",
        "-t", str(seconds), "-c:v", "libx264", "-b:v", bitrate, "-pix_fmt", "yuv420p", path
    ], check=True)


def main(out):
    cache = os.path.join(out, "_cache")
    cam = os.path.join(out, "DCIM", "Camera")
    shots = os.path.join(out, "Pictures", "Screenshots")
    movies = os.path.join(out, "Movies")
    for d in (cache, cam, shots, movies):
        os.makedirs(d, exist_ok=True)

    ids = photo_ids()
    with ThreadPoolExecutor(16) as pool:
        paths = [p for p in pool.map(lambda i: download(cache, i), ids) if p]
    random.shuffle(paths)
    n = len(paths)
    split = [int(n * f) for f in (0.70, 0.90, 0.96, 0.985)]
    groups = {
        "unique": paths[:split[0]], "dup": paths[split[0]:split[1]], "near": paths[split[1]:split[2]],
        "blur": paths[split[2]:split[3]], "dark": paths[split[3]:],
    }
    truth = {k: len(v) for k, v in groups.items()}

    for p in groups["unique"]:
        pid = os.path.basename(p)[:-4]
        img = Image.open(p)
        save(img, os.path.join(cam, f"u_{pid}_0.jpg"))
        save(ImageOps.mirror(img), os.path.join(cam, f"u_{pid}_1.jpg"))
        save(crop_scaled(img, (0, 0, W // 2, H // 2)), os.path.join(cam, f"u_{pid}_2.jpg"))
        save(crop_scaled(img, (W // 2, H // 2, W, H)), os.path.join(cam, f"u_{pid}_3.jpg"))
        save(crop_scaled(img, (W // 4, 0, 3 * W // 4, H // 2)), os.path.join(cam, f"u_{pid}_4.jpg"))
    for p in groups["dup"]:
        pid = os.path.basename(p)[:-4]
        img = Image.open(p)
        save(img, os.path.join(cam, f"d_{pid}_a.jpg"))
        save(img, os.path.join(cam, f"d_{pid}_b.jpg"), quality=70)
        save(img.resize((W // 2, H // 2), Image.LANCZOS), os.path.join(cam, f"d_{pid}_c.jpg"))
    for p in groups["near"]:
        pid = os.path.basename(p)[:-4]
        img = Image.open(p)
        save(img, os.path.join(cam, f"n_{pid}_a.jpg"))
        tweaked = ImageEnhance.Brightness(crop_scaled(img, (36, 27, W - 36, H - 27))).enhance(1.1)
        save(tweaked, os.path.join(cam, f"n_{pid}_b.jpg"))
    for p in groups["blur"]:
        pid = os.path.basename(p)[:-4]
        radius = random.uniform(4, 9)
        save(Image.open(p).filter(ImageFilter.GaussianBlur(radius)), os.path.join(cam, f"b_{pid}.jpg"))
    for p in groups["dark"]:
        pid = os.path.basename(p)[:-4]
        save(ImageEnhance.Brightness(Image.open(p)).enhance(0.18), os.path.join(cam, f"k_{pid}.jpg"))
    for i in range(300):
        screenshot(os.path.join(shots, f"Screenshot_2026{i:04d}.png"), i)
    for i in range(3):
        video(os.path.join(movies, f"big_{i}.mp4"), 40, "8M")
    for i in range(2):
        video(os.path.join(movies, f"small_{i}.mp4"), 5, "2M")

    with open(os.path.join(out, "truth.json"), "w") as f:
        json.dump(truth, f, indent=2)
    total = sum(len(fs) for _, _, fs in os.walk(cam)) + 300
    print("photo bases:", truth, "| image files:", total)


if __name__ == "__main__":
    main(sys.argv[1])
