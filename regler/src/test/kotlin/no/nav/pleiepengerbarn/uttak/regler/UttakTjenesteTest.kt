package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class UttakTjenesteTest {

    private val arbeidsforhold1 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "123456789")

    @Test
    fun `enkel uttaksperiode uten annen informasjon`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
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

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden, Prosent(100))
    }


    fun sjekkInnvilget(uttaksperiode: Uttaksperiode, forventetPeriode:LukketPeriode, utbetalingsgrad:Prosent) {
        assertEquals(forventetPeriode.fom, uttaksperiode.periode.fom)
        assertEquals(forventetPeriode.tom, uttaksperiode.periode.tom)
        assertNotNull(uttaksperiode.uttaksperiodeResultat)
        assertEquals(utbetalingsgrad, uttaksperiode.uttaksperiodeResultat?.utbetalingsgrad)
        assertNull(uttaksperiode.uttaksperiodeResultat?.avslåttPeriodeÅrsak)

    }

}