package app.quranaudio.playback

import app.quranaudio.domain.Track
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaItemsTest {
    private val track = Track("mp3quran:123", "mp3quran:123", 36, "Mishary Alafasi", "مشاري العفاسي", "Hafs · Murattal", "https://cdn.mp3quran.net/audio/afs/036.mp3")

    @Test fun `round trips through a media item`() {
        val item = MediaItems.toMediaItem(track)
        assertThat(item.mediaId).isEqualTo("mp3quran:123#36")
        assertThat(item.requestMetadata.mediaUri.toString()).isEqualTo(track.audioUrl)
        assertThat(item.mediaMetadata.title.toString()).isEqualTo("36. Ya-Sin")
        assertThat(item.mediaMetadata.artist.toString()).isEqualTo("Mishary Alafasi")
        assertThat(MediaItems.toTrack(item)).isEqualTo(track)
    }

    @Test fun `rejects items without our metadata`() {
        assertThat(MediaItems.toTrack(null)).isNull()
        assertThat(MediaItems.toTrack(androidx.media3.common.MediaItem.fromUri("https://x.test/a.mp3"))).isNull()
    }

    @Test fun `track keys parse back`() {
        assertThat(Track.parseKey(track.key)).isEqualTo("mp3quran:123" to 36)
        assertThat(Track.parseKey("bad")).isNull()
        assertThat(Track.parseKey("x#999")).isNull()
    }
}
