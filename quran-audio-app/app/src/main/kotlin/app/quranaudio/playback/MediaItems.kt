package app.quranaudio.playback

import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import app.quranaudio.domain.Track
import app.quranaudio.shared.quran.SurahCatalog

/** Conversion between domain [Track]s and Media3 [MediaItem]s. */
object MediaItems {
    private const val K_RECITER_ID = "reciterId"
    private const val K_RECITATION_ID = "recitationId"
    private const val K_SURAH = "surah"
    private const val K_RECITER_NAME = "reciterName"
    private const val K_RECITER_AR = "reciterNameAr"
    private const val K_RECITATION_TITLE = "recitationTitle"
    private const val K_URL = "url"

    fun toMediaItem(track: Track, artwork: ByteArray? = null): MediaItem {
        val surah = track.surah
        val extras = Bundle().apply {
            putString(K_RECITER_ID, track.reciterId)
            putString(K_RECITATION_ID, track.recitationId)
            putInt(K_SURAH, track.surahNumber)
            putString(K_RECITER_NAME, track.reciterName)
            putString(K_RECITER_AR, track.reciterNameArabic)
            putString(K_RECITATION_TITLE, track.recitationTitle)
            putString(K_URL, track.audioUrl)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle("${surah.number}. ${surah.nameTransliterated}")
            .setSubtitle(surah.nameArabic)
            .setArtist(track.reciterName)
            .setAlbumTitle(track.recitationTitle)
            .setTrackNumber(surah.number)
            .setTotalTrackCount(SurahCatalog.SURAH_COUNT)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setExtras(extras)
            .apply { if (artwork != null) setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER) }
            .build()
        val uri = track.audioUrl.toUri()
        return MediaItem.Builder()
            .setMediaId(track.key)
            .setUri(uri)
            .setMimeType(guessMime(uri))
            .setMediaMetadata(metadata)
            // The URI travels to the session in requestMetadata (controllers strip localConfiguration).
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .build()
    }

    fun toTrack(item: MediaItem?): Track? {
        val extras = item?.mediaMetadata?.extras ?: return null
        val surah = extras.getInt(K_SURAH, 0).takeIf(SurahCatalog::isValid) ?: return null
        return Track(
            reciterId = extras.getString(K_RECITER_ID) ?: return null,
            recitationId = extras.getString(K_RECITATION_ID) ?: return null,
            surahNumber = surah,
            reciterName = extras.getString(K_RECITER_NAME).orEmpty(),
            reciterNameArabic = extras.getString(K_RECITER_AR),
            recitationTitle = extras.getString(K_RECITATION_TITLE).orEmpty(),
            audioUrl = extras.getString(K_URL) ?: item.requestMetadata.mediaUri?.toString() ?: return null,
        )
    }

    private fun guessMime(uri: Uri): String? = when (uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4"
        "ogg", "opus" -> "audio/ogg"
        else -> null
    }
}
