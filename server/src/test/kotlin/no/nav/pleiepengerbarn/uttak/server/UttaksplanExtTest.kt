package no.nav.pleiepengerbarn.uttak.server

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UttaksplanExtTest {

    @Test
    internal fun `Bare helg mellom to datoer`() {
        assertThat(bareHelgMellom(LocalDate.parse("2021-08-06"), LocalDate.parse("2021-08-09"))).isTrue
        assertThat(bareHelgMellom(LocalDate.parse("2021-08-05"), LocalDate.parse("2021-08-09"))).isFalse
        assertThat(bareHelgMellom(LocalDate.parse("2021-08-06"), LocalDate.parse("2021-08-10"))).isFalse
        assertThat(bareHelgMellom(LocalDate.parse("2021-08-05"), LocalDate.parse("2021-08-10"))).isFalse
        assertThat(bareHelgMellom(LocalDate.parse("2021-08-07"), LocalDate.parse("2021-08-10"))).isFalse
    }

}