package app.sakina.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Fixtures use the reserved domain example.org: synthetic, NOT real providers or audio. */
class CatalogTest {
    private val goodJson = """
    {
      "schemaVersion": 1,
      "sources": [{
        "id": "test-src", "name": "Test Source", "attribution": "Test attribution",
        "licenseSummary": "Synthetic test licence", "streamingAllowed": true, "offlineAllowed": false,
        "allowedHosts": ["audio.example.org", "*.cdn.example.org"]
      }],
      "reciters": [{
        "id": "r1", "nameAr": "مشاري راشد العفاسي", "nameEn": "Test Reciter One",
        "alternateNames": ["Testy Reciter"],
        "recitations": [{
          "id": "r1-murattal", "sourceId": "test-src", "style": "Murattal",
          "urlTemplate": "https://audio.example.org/r1/{surah3}.mp3", "surahs": [1, 2, 112]
        }]
      }, {
        "id": "r2", "nameEn": "Explicit Reciter",
        "recitations": [{
          "id": "r2-x", "sourceId": "test-src",
          "urls": {"1": "https://a.cdn.example.org/1.mp3", "2": "http://a.cdn.example.org/2.mp3", "3": "https://evil.test/3.mp3"}
        }]
      }]
    }""".trimIndent()

    @Test fun parsesValidCatalogAndDropsBadUrls() {
        val res = CatalogParser.parse(goodJson)
        assertEquals(2, res.catalog.reciters.size)
        val r2 = res.catalog.reciters[1].recitations.single()
        assertEquals(setOf(1), r2.urls.keys)
        assertEquals(2, res.issues.size, res.issues.toString())
    }

    @Test fun indexResolvesTemplateUrls() {
        val idx = CatalogIndex(CatalogParser.parse(goodJson).catalog)
        val rec = idx.reciter("r1")!!.recitations.single()
        assertEquals("https://audio.example.org/r1/112.mp3", idx.audioUrl(rec, 112))
        assertNull(idx.audioUrl(rec, 3), "surah outside declared list")
        assertEquals(listOf(1, 2, 112), idx.availableSurahs(rec))
    }

    @Test fun nonStreamingSourceYieldsNoUrls() {
        val c = CatalogParser.parse(goodJson).catalog
        val locked = c.copy(sources = c.sources.map { it.copy(streamingAllowed = false) })
        val idx = CatalogIndex(locked)
        val rec = idx.reciter("r1")!!.recitations.single()
        assertNull(idx.audioUrl(rec, 1))
        assertTrue(idx.availableSurahs(rec).isEmpty())
    }

    @Test fun brokenRecitationIsNotPlayable() {
        val c = CatalogParser.parse(goodJson).catalog
        val rec = c.reciters[0].recitations[0].copy(status = SourceStatus.BROKEN)
        assertNull(CatalogIndex(c).audioUrl(rec, 1))
    }

    @Test fun offlineRequiresExplicitPermission() {
        val idx = CatalogIndex(CatalogParser.parse(goodJson).catalog)
        assertTrue(!idx.canDownload(idx.reciter("r1")!!.recitations.single()))
    }

    @Test fun rejectsUnsupportedSchemaAndGarbage() {
        assertFailsWith<IllegalArgumentException> { CatalogParser.parse("""{"schemaVersion": 2}""") }
        assertFailsWith<IllegalArgumentException> { CatalogParser.parse("not json") }
    }

    @Test fun rejectsTemplateOnDisallowedHost() {
        val bad = goodJson.replace("https://audio.example.org/r1", "https://other.example.net/r1")
        val res = CatalogParser.parse(bad)
        assertTrue(res.catalog.reciters[0].recitations.isEmpty())
        assertTrue(res.issues.any { "not allowed" in it })
    }

    @Test fun rejectsUnknownSourceAndDuplicates() {
        val bad = goodJson.replace("\"sourceId\": \"test-src\", \"style\"", "\"sourceId\": \"nope\", \"style\"")
        assertTrue(CatalogParser.parse(bad).issues.any { "unknown source" in it })
        val dup = CatalogParser.parse(goodJson).catalog.let { it.copy(reciters = it.reciters + it.reciters[0]) }
        assertTrue(CatalogParser.validate(dup).issues.any { "duplicate" in it })
    }

    @Test fun searchByArabicEnglishAndAlternate() {
        val idx = CatalogIndex(CatalogParser.parse(goodJson).catalog)
        assertEquals("r1", idx.search("العفاسي").single().id)
        assertEquals("r1", idx.search("test reciter one").single().id)
        assertEquals("r1", idx.search("testy").single().id)
        assertEquals(2, idx.search("").size)
        assertEquals(listOf("r1"), idx.search("", style = "murattal").map { it.id })
        assertEquals(listOf("Murattal"), idx.styles())
    }

    @Test fun bundledCatalogIfPresentParses() {
        val text = javaClass.getResource("/catalog.json")?.readText() ?: return
        assertNotNull(CatalogParser.parse(text))
    }
}
