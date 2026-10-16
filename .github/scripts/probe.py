"""Prints live mp3quran.net catalogue statistics used in docs and the store listing."""
import json, urllib.request

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "quran-audio-catalogue-check"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)

reciters = get("https://www.mp3quran.net/api/v3/reciters?language=eng")["reciters"]
complete = [r for r in reciters if any(int(m.get("surah_total", 0)) == 114 for m in r["moshaf"])]
print(f"reciters: {len(reciters)}")
print(f"with complete 114-Surah recitation: {len(complete)}")
print(f"recitations (moshaf): {sum(len(r['moshaf']) for r in reciters)}")
print("audio hosts:", sorted({m['server'].split('/')[2] for r in reciters for m in r['moshaf']}))
sample = complete[0]["moshaf"][0]["server"] + "001.mp3"
head = urllib.request.Request(sample, method="HEAD", headers={"User-Agent": "quran-audio-catalogue-check"})
with urllib.request.urlopen(head, timeout=30) as h:
    print("sample", sample, h.status, h.headers.get("Content-Type"), "ranges:", h.headers.get("Accept-Ranges"))
