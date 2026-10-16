package app.quranaudio.backend

import app.quranaudio.shared.catalog.AudioSource
import app.quranaudio.shared.catalog.CatalogReciter

/** One upstream source of reciters and audio. */
interface CatalogProvider {
    val source: AudioSource
    /** Hosts (exact, or suffix starting with ".") that this provider's audio URLs may use. */
    val allowedAudioHosts: Set<String>
    suspend fun fetchReciters(): List<CatalogReciter>
}
