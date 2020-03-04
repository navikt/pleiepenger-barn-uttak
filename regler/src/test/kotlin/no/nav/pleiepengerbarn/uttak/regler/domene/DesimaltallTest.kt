package no.nav.pleiepengerbarn.uttak.regler.domene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

internal class DesimaltallTest {
    @Test
    internal fun `Dele to durations på hverandre`() {
        val durationA = Duration.parse("PT129H22M30S")
        val durationB = Duration.parse("PT172H30M")
        val forventet = Desimaltall.fraDouble(0.75)
        assertEquals(forventet, durationA / durationB)
    }

    @Test
    internal fun `Delte to durations og få en prosent ut fra det`() {
        val durationA = Duration.parse("PT120H45M")
        val durationB = Duration.parse("PT129H22M30S")
        val forventet = Desimaltall.fraDouble(93.33)
        assertEquals(forventet, durationA.div(durationB).fraFaktorTilProsent())
    }
}