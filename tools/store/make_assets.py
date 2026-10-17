"""Renders Play Store graphics into store/.

Usage: python tools/store/make_assets.py <raw_screens_dir>

- store/icon-512.png             launcher icon (redrawn from the adaptive icon vectors)
- store/feature-graphic.png      1024x500 feature graphic
- store/screenshots/NN-*.png     1080x1920 marketing frames around raw device screenshots,
                                 raw files named NN-<slug>.png with captions in CAPTIONS
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
STORE = os.path.join(ROOT, "store")
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font", "manrope.ttf")
INK = (7, 9, 13)
TEAL, BLUE = (46, 230, 197), (79, 140, 255)
ICON_A, ICON_B = (0, 194, 168), (21, 101, 192)

CAPTIONS = {
    "home": ("See what you can clear", "Screenshots, duplicates, blurry shots and big videos"),
    "dupes": ("Keep the best shot", "Smart Clean removes the copies in one tap"),
    "review": ("Swipe to decide", "Left to clean, right to keep. Undo anytime"),
    "videos": ("Find the space hogs", "Your largest videos, biggest first"),
    "celebrate": ("Safe by design", "Everything goes to Trash first, restorable for 30 days"),
    "onboarding": ("Private by design", "All analysis happens on your phone"),
}


def font(size, weight=800):
    f = ImageFont.truetype(FONT, size)
    try:
        f.set_variation_by_axes([weight])
    except Exception:
        pass
    return f


def gradient(size, a, b, diagonal=True):
    w, h = size
    xs = np.linspace(0, 1, w)[None, :]
    ys = np.linspace(0, 1, h)[:, None]
    t = (xs + ys) / 2 if diagonal else np.repeat(xs, h, axis=0)
    rgb = np.array(a)[None, None, :] + (np.array(b) - np.array(a))[None, None, :] * t[:, :, None]
    return Image.fromarray(rgb.astype(np.uint8))


def icon(size=512):
    s = size * 4 / 108  # supersample, then scale the 108-unit viewport
    big = gradient((size * 4, size * 4), ICON_A, ICON_B)
    d = ImageDraw.Draw(big)
    # The adaptive icon's safe zone is the centre 72 units; scale it up so the art fills the Play icon.
    z, o = 1.35, -54 * 0.35

    def p(x, y):
        return ((x * z + o) * s, (y * z + o) * s)

    d.rounded_rectangle([*p(32, 32), *p(76, 76)], radius=4 * z * s, fill="white")
    d.polygon([p(36, 70), p(47, 55), p(55, 65), p(61, 58), p(72, 70)], fill=ICON_B)
    cx, cy = p(60, 43)
    r = 5 * z * s
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=ICON_A)
    d.polygon([p(74, 26), p(76, 32), p(82, 34), p(76, 36), p(74, 42), p(72, 36), p(66, 34), p(72, 32)], fill=(255, 213, 79))
    return big.resize((size, size), Image.LANCZOS)


def text_gradient(canvas, xy, text, fnt):
    mask = Image.new("L", canvas.size, 0)
    ImageDraw.Draw(mask).text(xy, text, font=fnt, fill=255)
    canvas.paste(gradient(canvas.size, TEAL, BLUE, diagonal=False), (0, 0), mask)


def glow(canvas, center, radius, color, alpha=90):
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    x, y = center
    d.ellipse([x - radius, y - radius, x + radius, y + radius], fill=(*color, alpha))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(radius // 2)))


def feature_graphic():
    img = Image.new("RGBA", (1024, 500), (*INK, 255))
    glow(img, (820, 250), 260, TEAL, 70)
    glow(img, (940, 120), 180, BLUE, 60)
    ic = icon(220)
    m = Image.new("L", ic.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, *ic.size], radius=50, fill=255)
    img.paste(ic, (740, 140), m)
    d = ImageDraw.Draw(img)
    d.text((70, 120), "CleanShots", font=font(84), fill=(242, 244, 248))
    text_gradient(img, (70, 225), "Free up space,", font(52))
    text_gradient(img, (70, 285), "keep the best shots.", font(52))
    d.text((72, 372), "Private · On-device · Trash-first", font=font(26, 600), fill=(138, 147, 166))
    img.convert("RGB").save(os.path.join(STORE, "feature-graphic.png"))


def frame(raw_path, slug, out_path):
    title, sub = CAPTIONS[slug]
    W, H = 1080, 1920
    img = Image.new("RGBA", (W, H), (*INK, 255))
    glow(img, (W // 2, 1150), 520, TEAL, 55)
    glow(img, (W - 120, 420), 300, BLUE, 45)
    d = ImageDraw.Draw(img)
    tf = font(76)
    tw = d.textlength(title, font=tf)
    text_gradient(img, ((W - tw) / 2, 110), title, tf)
    sf = font(36, 500)
    sw = d.textlength(sub, font=sf)
    d.text(((W - sw) / 2, 215), sub, font=sf, fill=(170, 178, 194))
    shot = Image.open(raw_path).convert("RGB")
    target_h = 1560
    shot = shot.resize((int(shot.width * target_h / shot.height), target_h), Image.LANCZOS)
    radius = 56
    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, *shot.size], radius=radius, fill=255)
    x = (W - shot.width) // 2
    y = 330
    border = Image.new("RGBA", (shot.width + 16, shot.height + 16), (0, 0, 0, 0))
    ImageDraw.Draw(border).rounded_rectangle([0, 0, *border.size], radius=radius + 8, fill=(36, 42, 54, 255))
    img.alpha_composite(border, (x - 8, y - 8))
    img.paste(shot, (x, y), mask)
    img.convert("RGB").save(out_path)


def main(raw_dir):
    os.makedirs(os.path.join(STORE, "screenshots"), exist_ok=True)
    icon().save(os.path.join(STORE, "icon-512.png"))
    feature_graphic()
    for name in sorted(os.listdir(raw_dir)):
        if not name.endswith(".png"):
            continue
        slug = name[3:-4]
        if slug in CAPTIONS:
            frame(os.path.join(raw_dir, name), slug, os.path.join(STORE, "screenshots", name))
    print("store assets written to", STORE)


if __name__ == "__main__":
    main(sys.argv[1])
