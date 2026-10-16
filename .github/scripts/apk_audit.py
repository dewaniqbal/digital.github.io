"""Summarise REA JSON output for the Quran Audio release APK (prints a compact report)."""
import json, os, re, sys, zipfile, hashlib

out, apk = sys.argv[1], sys.argv[2]
print("APK:", os.path.basename(apk), os.path.getsize(apk), "bytes, sha256", hashlib.sha256(open(apk, "rb").read()).hexdigest())

def load(name):
    p = os.path.join(out, name)
    try:
        return json.load(open(p))
    except Exception as e:
        print(f"[{name}] unreadable: {e}; first bytes: {open(p).read(300) if os.path.exists(p) else 'missing'}")
        return None

def walk(o, path=""):
    if isinstance(o, dict):
        for k, v in o.items():
            yield from walk(v, f"{path}.{k}")
    elif isinstance(o, list):
        for i, v in enumerate(o):
            yield from walk(v, f"{path}[{i}]")
    else:
        yield path, o

pkg = load("package.json")
if pkg is not None:
    print("\n== REA package result: top-level keys", list(pkg)[:20] if isinstance(pkg, dict) else type(pkg))
    leaves = list(walk(pkg))
    interesting = re.compile(r"(package|version|sdk|permission|exported|activity|service|receiver|provider|class_count|resource_count|coverage|limitation|unknown|engine)", re.I)
    shown = 0
    for path, val in leaves:
        if interesting.search(path) and not isinstance(val, (dict, list)) and len(str(val)) < 300:
            print(f"{path} = {val}")
            shown += 1
            if shown > 120:
                print("... (truncated)"); break
    manifest = next((v for p, v in leaves if isinstance(v, str) and "<manifest" in v), None)
    if manifest:
        print("\n== Decoded manifest (from REA) ==")
        print(manifest[:6000])

print("\n== R8 keep check: class searches ==")
for f in sorted(os.listdir(out)):
    if f.startswith("search_") and f.endswith(".json"):
        d = load(f)
        if d is None:
            continue
        names = sorted({v for p, v in walk(d) if isinstance(v, str) and re.fullmatch(r"[A-Za-z0-9_$.]+", v) and "." in v and ("quranaudio" in v or "app." in v)})
        print(f"{f[7:-5]}: {len(names)} matching names -> {names[:12]}")

with zipfile.ZipFile(apk) as z:
    dex = [n for n in z.namelist() if n.endswith(".dex")]
    raw = [n for n in z.namelist() if n.startswith("res/raw/")]
    print("\n== Archive ==", len(z.namelist()), "entries;", "dex:", dex, "; raw:", len(raw))
