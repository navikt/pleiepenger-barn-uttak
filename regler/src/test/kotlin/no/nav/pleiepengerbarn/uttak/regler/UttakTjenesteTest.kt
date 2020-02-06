package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class UttakTjenesteTest {

    private val arbeidsforhold1 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "123456789")

    @Test
    fun `enkel uttaksperiode uten annen informasjon`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val avklarteFakta = RegelGrunnlag(
                tilsynsbehov = listOf(
                        Tilsynsbehov(helePerioden, TilsynsbehovStørrelse.PROSENT_200)
                ),
                søktePerioder = listOf(
                        helePerioden
                ),
                arbeidsforhold = mapOf(
                    arbeidsforhold1 to listOf()
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplan(avklarteFakta)

        assertTrue(uttaksplan.perioder.size == 1)
        val innvilgetPeriode = uttaksplan.perioder[0]
        assertEquals(helePerioden.fom, innvilgetPeriode.fom)
        assertEquals(helePerioden.tom, innvilgetPeriode.tom)
        assertNotNull(innvilgetPeriode.uttaksperiodeResultat)
        assertEquals(BigDecimal(100), innvilgetPeriode.uttaksperiodeResultat?.utbetalingsgrad)
        assertNull(innvilgetPeriode.uttaksperiodeResultat?.avslåttPeriodeÅrsak)

    }

}