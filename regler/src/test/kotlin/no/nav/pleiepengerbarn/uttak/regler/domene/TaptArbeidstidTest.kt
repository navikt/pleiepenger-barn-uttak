package no.nav.pleiepengerbarn.uttak.regler.domene

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

internal class TaptArbeidstidTest {

    @BeforeEach
    internal fun setUp() {
        System.setProperty("JUSTER_NORMALTID_ANDRE_PARTERS_TILSYN", "true")
    }

    @AfterEach
    internal fun tearDown() {
        System.clearProperty("JUSTER_NORMALTID_ANDRE_PARTERS_TILSYN")
    }

    @Test
    fun `Skal justere tilsynsgraden bruker legger beslag på ved mindre enn 7,5`() {
        val taptArbeidstid = TaptArbeidstid(Duration.ofHours(4), Duration.ofHours(6))

        val beregnTaptArbeidIProsentJustertHvisNormaltErUnderFullStilling =
            taptArbeidstid.beregnTaptArbeidIProsentJustertMotNormalArbeidstid()
        assertThat(beregnTaptArbeidIProsentJustertHvisNormaltErUnderFullStilling).isEqualTo(BigDecimal("26.66666640").setScale(8, RoundingMode.HALF_UP))
    }

    @Test
    fun `Skal justere tilsynsgraden bruker legger beslag på ved mindre enn 7,5 2`() {
        val taptArbeidstid = TaptArbeidstid(Duration.ofHours(5), Duration.ofHours(10))

        val beregnTaptArbeidIProsentJustertHvisNormaltErUnderFullStilling =
            taptArbeidstid.beregnTaptArbeidIProsentJustertMotNormalArbeidstid()
        assertThat(beregnTaptArbeidIProsentJustertHvisNormaltErUnderFullStilling).isEqualTo(BigDecimal("66.66666650").setScale(8, RoundingMode.HALF_UP))
    }


    @Test
    fun `Skal justere tilsynsgraden bruker legger beslag på ved mindre enn 7,5 3`() {
        val taptArbeidstid = TaptArbeidstid(Duration.ofHours(1), Duration.ofHours(5))

        val beregnTaptArbeidIProsentJustertHvisNormaltErUnderFullStilling =
            taptArbeidstid.beregnTaptArbeidIProsentJustertMotNormalArbeidstid()
        assertThat(beregnTaptArbeidIProsentJustertHvisNormaltErUnderFullStilling).isEqualTo(BigDecimal("53.33333360").setScale(8, RoundingMode.HALF_UP))
    }

    @Test
    fun `Skal justere tilsynsgraden bruker legger beslag på ved mer enn 7,5`() {
        val taptArbeidstid = TaptArbeidstid(Duration.ofHours(10), Duration.ofHours(20))

        val beregnTaptArbeidIProsentJustertHvisNormaltErUnderFullStilling =
            taptArbeidstid.beregnTaptArbeidIProsentJustertMotNormalArbeidstid()
        assertThat(beregnTaptArbeidIProsentJustertHvisNormaltErUnderFullStilling).isEqualTo(BigDecimal("100"))
    }
}


