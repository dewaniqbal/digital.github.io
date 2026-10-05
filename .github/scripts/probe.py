import json, re, urllib.request
def get(url, n=None):
    req = urllib.request.Request(url, headers={"User-Agent": "probe/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            b = r.read(); print(f"[{r.status}] {url} final={r.url} type={r.headers.get('content-type')} len={len(b)}")
            return b
    except Exception as e:
        print("ERR", url, e); return b""
G = "https://dl.google.com/dl/android/maven2/"
want = {"com/android/tools/build":["gradle","aapt2"],"androidx/compose":["compose-bom"],"androidx/media3":["media3-exoplayer"],
 "androidx/room":["room-runtime"],"androidx/datastore":["datastore-preferences"],"androidx/navigation":["navigation-compose"],
 "androidx/hilt":["hilt-navigation-compose","hilt-lifecycle-viewmodel-compose"],"androidx/activity":["activity-compose"],"androidx/lifecycle":["lifecycle-runtime-compose","lifecycle-viewmodel-compose"],
 "androidx/core":["core-ktx","core-splashscreen"],"com/google/android/gms":["play-services-ads"],"com/google/android/ump":["user-messaging-platform"],
 "androidx/work":["work-runtime-ktx"],"androidx/test/ext":["junit"],"androidx/test":["runner","core-ktx"],"androidx/navigation3":["navigation3-runtime","navigation3-ui"],
 "androidx/room3":["room3-runtime"],"androidx/media":["media"],"androidx/compose/material3":["material3"],"androidx/compose/material":["material-icons-extended"]}
for g, arts in want.items():
    x = get(G + g + "/group-index.xml").decode(errors="ignore")
    for a in arts:
        m = re.search(r'<' + re.escape(a) + r' versions="([^"]*)"', x)
        if m:
            vs = m.group(1).split(",")
            print("VER", g, a, "stable:", [v for v in vs if not re.search("alpha|beta|rc|dev", v)][-4:], "latest:", vs[-1])
x = get("https://dl.google.com/android/repository/repository2-3.xml").decode(errors="ignore")
print("SDK", sorted(set(re.findall(r'build-tools;[0-9.]+', x)))[-4:], sorted(set(re.findall(r'platforms;android-[0-9]+', x)))[-4:])

d = json.loads(get("https://www.mp3quran.net/api/v3/reciters?language=eng") or b"{}")
r = d.get("reciters", [])
print("MP3Q keys", list(d.keys()), "reciters", len(r))
for i in range(2): print(json.dumps(r[i], ensure_ascii=False)[:1400])
comp = [x for x in r if any(int(m.get("surah_total", 0)) == 114 for m in x["moshaf"])]
print("MP3Q complete114 reciters", len(comp), "moshaf total", sum(len(x["moshaf"]) for x in r))
mt = {}
for x in r:
    for m in x["moshaf"]: mt[m["name"]] = mt.get(m["name"], 0) + 1
print("MP3Q moshaf names", sorted(mt.items(), key=lambda kv: -kv[1])[:30])
print("MP3Q hosts", sorted(set(m["server"].split("/")[2] for x in r for m in x["moshaf"])))
print("MP3Q letters", sorted(set(x.get("letter","") for x in r))[:40])
print("MP3Q sample names", [x["name"] for x in r][:80])
for path in ["suwar?language=eng", "riwayat?language=eng", "moshaf?language=eng", "languages", "recent_reads?language=eng"]:
    print(path, get("https://www.mp3quran.net/api/v3/" + path)[:900].decode(errors="ignore"))
ar = json.loads(get("https://www.mp3quran.net/api/v3/reciters?language=ar") or b"{}").get("reciters", [])
print("MP3Q ar sample", [(x["id"], x["name"]) for x in ar[:10]])
if r:
    m = r[0]["moshaf"][0]; u = m["server"] + "001.mp3"
    req = urllib.request.Request(u, method="HEAD", headers={"User-Agent":"probe"})
    try:
        with urllib.request.urlopen(req, timeout=30) as h: print("HEAD", u, h.status, dict(h.headers))
    except Exception as e: print("HEAD ERR", u, e)
for u in ["https://api.quran.com/api/v4/resources/chapter_reciters?language=en", "https://api.quran.com/api/v4/chapter_recitations/7/1",
          "https://api.quran.com/api/v4/chapters?language=en", "https://quranicaudio.com/api/qaris", "https://www.mp3quran.net/eng/terms", "https://mp3quran.net/api"]:
    print(get(u)[:2500].decode(errors="ignore"))
ch = json.loads(get("https://api.quran.com/api/v4/chapters?language=en") or b"{}").get("chapters", [])
print("CHAPTERS", json.dumps([[c["id"], c["name_simple"], c["name_arabic"], c["verses_count"], c["revelation_place"]] for c in ch], ensure_ascii=False))
