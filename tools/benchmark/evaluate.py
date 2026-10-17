"""Scores the app's scan results against the labelled library from make_library.py.

Usage: python tools/benchmark/evaluate.py <cleanshots.db> [blur_threshold=30] [hamming_threshold=4]

Mirrors the app: duplicates use leader clustering over dHash in getAllHashed() order (BK-tree
query, closest leader wins) with screenshots excluded; blur uses BlurUtils.DEFAULT_THRESHOLD and
skips screenshots. Defaults match HashUtils.DUPLICATE_DISTANCE and BlurUtils.DEFAULT_THRESHOLD.
"""
import re
import sqlite3
import sys
from itertools import combinations

SCREENSHOT_HINTS = ["screenshot", "screen_shot", "截屏", "bildschirmfoto", "captura de pantalla", "capture d'écran"]


def is_screenshot(rp, bucket, name):
    s = f"{rp}|{bucket}|{name}".lower()
    return any(h in s for h in SCREENSHOT_HINTS)


def informative(h, min_bits=6):
    return min_bits <= bin(h & 0xFFFFFFFFFFFFFFFF).count("1") <= 64 - min_bits


def hamming(a, b):
    return bin((a ^ b) & 0xFFFFFFFFFFFFFFFF).count("1")


def group(rows, threshold):
    leaders, members = [], {}
    for i, r in enumerate(rows):
        h = r["dhash"]
        if not informative(h):
            continue
        near = [j for j in leaders if hamming(h, rows[j]["dhash"]) <= threshold]
        if not near:
            leaders.append(i)
            members[i] = [i]
        else:
            members[min(near, key=lambda j: hamming(h, rows[j]["dhash"]))].append(i)
    return [m for m in members.values() if len(m) > 1]


def truth_key(name):
    m = re.match(r"([udnbk])_(\w+?)(?:_(\w))?\.jpg$", name)
    if not m:
        return ("other", name)
    kind, pid, var = m.groups()
    if kind in ("d", "n"):
        return (kind, pid)
    return (kind, f"{pid}_{var}")


def main(db, blur_threshold=30.0, ham=4):
    con = sqlite3.connect(db)
    con.row_factory = sqlite3.Row
    rows = [dict(r) for r in con.execute(
        "SELECT * FROM media_scan WHERE dhash IS NOT NULL ORDER BY dateModified DESC")]
    print(f"rows hashed: {len(rows)}")

    # Blur
    photos = [r for r in rows if not is_screenshot(r["relativePath"], r["bucket"], r["displayName"])]
    flagged = {r["displayName"] for r in photos if (r["blurScore"] if r["blurScore"] is not None else 1e6) < blur_threshold}
    blur_truth = {r["displayName"] for r in photos if r["displayName"].startswith("b_")}
    dark = {r["displayName"] for r in photos if r["displayName"].startswith("k_")}
    sharp = {r["displayName"] for r in photos} - blur_truth
    # Upscaled crops (u_*_2..4) are genuinely soft; "natural" excludes them.
    natural = {n for n in sharp if not re.match(r"u_\w+_[234]\.jpg$", n)}
    tp = len(flagged & blur_truth)
    print(f"blur: caught {tp}/{len(blur_truth)} blurred ({100 * tp / max(1, len(blur_truth)):.0f}%), "
          f"false alarms {len(flagged & sharp)}/{len(sharp)} sharp photos "
          f"({100 * len(flagged & sharp) / max(1, len(sharp)):.1f}%), of which dark {len(flagged & dark)}/{len(dark)}")
    print(f"  on natural (uncropped) photos: false alarms {len(flagged & natural)}/{len(natural)} "
          f"({100 * len(flagged & natural) / max(1, len(natural)):.1f}%)")

    # Duplicates (screenshots are excluded in the app)
    dup_rows = [r for r in rows if not is_screenshot(r["relativePath"], r["bucket"], r["displayName"])]
    groups = [[rows.index(dup_rows[i]) for i in g] for g in group(dup_rows, ham)]
    pred_pairs, wrong_suggestions, suggested = set(), 0, 0
    for g in groups:
        names = [rows[i]["displayName"] for i in g]
        pred_pairs |= {tuple(sorted(p)) for p in combinations(names, 2)}
        keeper = max(g, key=lambda i: (rows[i]["width"] * rows[i]["height"], rows[i]["size"], rows[i]["blurScore"] or 0))
        for i in g:
            if i == keeper:
                continue
            suggested += 1
            if truth_key(rows[i]["displayName"]) != truth_key(rows[keeper]["displayName"]):
                wrong_suggestions += 1
    by_truth = {}
    for r in rows:
        k = truth_key(r["displayName"])
        if k[0] in ("d", "n"):
            by_truth.setdefault(k, []).append(r["displayName"])
    true_pairs = {tuple(sorted(p)) for names in by_truth.values() for p in combinations(names, 2)}
    tp_pairs = len(pred_pairs & true_pairs)
    exact = [k for k in by_truth if k[0] == "d"]
    near = [k for k in by_truth if k[0] == "n"]

    def found(keys):
        ok = 0
        for k in keys:
            names = set(by_truth[k])
            if any(names <= {rows[i]["displayName"] for i in g} for g in groups):
                ok += 1
        return ok

    print(f"duplicates: {len(groups)} groups; exact sets fully found {found(exact)}/{len(exact)}, "
          f"near sets found {found(near)}/{len(near)}")
    print(f"  pair precision {100 * tp_pairs / max(1, len(pred_pairs)):.1f}%  recall {100 * tp_pairs / max(1, len(true_pairs)):.1f}%")
    print(f"  suggested for deletion {suggested}, of which NOT a copy of the kept photo: {wrong_suggestions}")
    shots = sum(is_screenshot(r["relativePath"], r["bucket"], r["displayName"]) for r in rows)
    print(f"screenshots detected: {shots}")


if __name__ == "__main__":
    args = sys.argv[1:]
    main(args[0], float(args[1]) if len(args) > 1 else 30.0, int(args[2]) if len(args) > 2 else 4)
