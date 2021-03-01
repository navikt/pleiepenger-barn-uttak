package no.nav.pleiepengerbarn.uttak.kontrakter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


internal class ÅrsakTest {

    @Test
    internal fun `Sjekk at ingen av årsakene er for lange til å persisteres`() {
        Årsak.values().map { it.name } .forEach {
            assertThat(it).hasSizeLessThanOrEqualTo(40)
        }
    }

}