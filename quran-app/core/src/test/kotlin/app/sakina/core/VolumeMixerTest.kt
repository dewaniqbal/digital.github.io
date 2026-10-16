package app.sakina.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VolumeMixerTest {
    @Test fun acceptanceTest_independentSliders() {
        val m = VolumeMixer()
        m.setAmbientEnabled(true)
        m.setQuranVolume(0.70f); m.setAmbientVolume(0.20f)
        assertEquals(0.70f, m.settings.value.quranVolume)
        assertEquals(0.20f, m.settings.value.ambientVolume)

        val quranGainBefore = m.quranGain
        m.setAmbientVolume(0.50f)
        assertEquals(0.70f, m.settings.value.quranVolume, "Quran must stay 70%")
        assertEquals(quranGainBefore, m.quranGain, "Quran gain must not move")

        val ambientGainBefore = m.ambientGain
        m.setQuranVolume(0.40f)
        assertEquals(0.50f, m.settings.value.ambientVolume, "background must stay 50%")
        assertEquals(ambientGainBefore, m.ambientGain, "background gain must not move")
    }

    @Test fun mutesAreSeparateAndPreserveLevels() {
        val m = VolumeMixer().apply { setAmbientEnabled(true); setQuranVolume(0.6f); setAmbientVolume(0.4f) }
        m.setQuranMuted(true)
        assertEquals(0f, m.quranGain)
        assertTrue(m.ambientGain > 0f, "background still audible")
        assertEquals(0.6f, m.settings.value.quranVolume, "level kept while muted")
        m.setQuranMuted(false); m.setAmbientMuted(true)
        assertTrue(m.quranGain > 0f)
        assertEquals(0f, m.ambientGain)
        assertEquals(0.4f, m.settings.value.ambientVolume)
    }

    @Test fun ambientOffByDefaultAndSilentUntilEnabled() {
        val m = VolumeMixer()
        assertFalse(m.settings.value.ambientEnabled)
        assertEquals(0f, m.ambientGain)
        assertFalse(m.ambientShouldPlayAfterQuranStops())
    }

    @Test fun headroomBackgroundNeverOverpowersFullRecitation() {
        val m = VolumeMixer().apply { setAmbientEnabled(true); setQuranVolume(1f); setAmbientVolume(1f) }
        assertEquals(1f, m.quranGain)
        assertEquals(VolumeMixer.AMBIENT_CEILING, m.ambientGain)
        assertTrue(m.quranGain + m.ambientGain <= 1.5f + 1e-6f)
        assertTrue(m.ambientGain < m.quranGain)
    }

    @Test fun gainsClampedAndMonotonic() {
        val m = VolumeMixer()
        m.setQuranVolume(5f); assertEquals(1f, m.settings.value.quranVolume)
        m.setQuranVolume(-1f); assertEquals(0f, m.settings.value.quranVolume)
        m.setQuranVolume(Float.NaN); assertEquals(0f, m.settings.value.quranVolume)
        var last = -1f
        for (i in 0..100) { m.setQuranVolume(i / 100f); assertTrue(m.quranGain >= last); last = m.quranGain }
    }

    @Test fun stopPolicyFollowsUserSetting() {
        val m = VolumeMixer().apply { setAmbientEnabled(true) }
        assertFalse(m.ambientShouldPlayAfterQuranStops()) // default STOP
        m.setAmbientOnQuranStop(AmbientOnQuranStop.CONTINUE)
        assertTrue(m.ambientShouldPlayAfterQuranStops())
    }

    @Test fun rampEndsExactlyAtTargetWithoutOvershoot() {
        val r = VolumeMixer.ramp(0.5f, 0f, 10)
        assertEquals(10, r.size); assertEquals(0f, r.last())
        assertTrue(r.all { it in 0f..0.5f })
        assertTrue(r.zipWithNext().all { (a, b) -> b <= a })
    }

    @Test fun restoredSettingsAreSanitized() {
        val m = VolumeMixer(VolumeSettings(quranVolume = 9f, ambientVolume = -3f))
        assertEquals(1f, m.settings.value.quranVolume); assertEquals(0f, m.settings.value.ambientVolume)
    }
}
