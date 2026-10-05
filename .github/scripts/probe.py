import json, urllib.request
def get(u):
    with urllib.request.urlopen(urllib.request.Request(u, headers={"User-Agent":"probe"}), timeout=60) as r: return json.load(r)
en = get("https://www.mp3quran.net/api/v3/reciters?language=eng")["reciters"]
ar = {x["id"]: x["name"] for x in get("https://www.mp3quran.net/api/v3/reciters?language=ar")["reciters"]}
for x in sorted(en, key=lambda x: x["id"]):
    print("R|%d|%s|%s|%s|%s" % (x["id"], x["name"], ar.get(x["id"], ""), x["date"][:10],
          ";".join("%d:%s:%d:%d" % (m["id"], m["name"].split(" - ")[-1], m["surah_total"], m["rewaya_id"]) for m in x["moshaf"])))
