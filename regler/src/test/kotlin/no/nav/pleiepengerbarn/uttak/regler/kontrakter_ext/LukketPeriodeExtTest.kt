package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LukketPeriodeExtTest {

    @Test
    fun `Test av overlapperHelt`() {
        //Caser som overlapper helt
        assertThat(LukketPeriode("2021-01-10/2021-01-20").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-09/2021-01-21").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-09/2021-01-20").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-10/2021-01-21").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isTrue

        //Caser som ikke overlapper helt
        assertThat(LukketPeriode("2021-01-01/2021-01-09").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isFalse
        assertThat(LukketPeriode("2021-01-01/2021-01-10").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isFalse
        assertThat(LukketPeriode("2021-01-01/2021-01-15").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isFalse
        assertThat(LukketPeriode("2021-01-01/2021-01-19").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isFalse
        assertThat(LukketPeriode("2021-01-11/2021-01-20").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isFalse
        assertThat(LukketPeriode("2021-01-15/2021-01-25").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isFalse
        assertThat(LukketPeriode("2021-01-20/2021-01-25").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isFalse
        assertThat(LukketPeriode("2021-01-21/2021-01-25").overlapperHelt(LukketPeriode("2021-01-10/2021-01-20"))).isFalse
    }

    @Test
    fun `Test av overlapperDelvis`() {
        //Caser som overlapper
        assertThat(LukketPeriode("2021-01-10/2021-01-20").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-09/2021-01-21").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-09/2021-01-20").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-10/2021-01-21").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-01/2021-01-10").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-01/2021-01-15").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-01/2021-01-19").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-11/2021-01-20").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-15/2021-01-25").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-20/2021-01-25").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isTrue
        assertThat(LukketPeriode("2021-01-12/2021-01-18").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isTrue

        //Caser som ikke overlapper
        assertThat(LukketPeriode("2021-01-01/2021-01-09").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isFalse
        assertThat(LukketPeriode("2021-01-21/2021-01-25").overlapperDelvis(LukketPeriode("2021-01-10/2021-01-20"))).isFalse
    }

}