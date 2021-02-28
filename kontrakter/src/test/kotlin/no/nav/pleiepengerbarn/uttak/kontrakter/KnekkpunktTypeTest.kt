package no.nav.pleiepengerbarn.uttak.kontrakter

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


internal class KnekkpunktTypeTest {

    @Test
    internal fun `Sjekk at ingen av årsakene er for lange til å persisteres`() {
        KnekkpunktType.values().map { it.name } .forEach {
            Assertions.assertThat(it).hasSizeLessThanOrEqualTo(40)
        }
    }

}