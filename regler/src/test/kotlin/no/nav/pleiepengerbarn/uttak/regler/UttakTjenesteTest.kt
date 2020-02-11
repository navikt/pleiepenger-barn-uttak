package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

internal class UttakTjenesteTest {

    private val arbeidsforhold1 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "123456789")

    @Test
    fun `Enkel uttaksperiode uten annen informasjon`() {
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


    @Test
    fun `En uttaksperioder som delvis overlapper med ferie`() {
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
                ),
                ferier = listOf(
                        LukketPeriode(LocalDate.of(2020, Month.JANUARY, 15), LocalDate.of(2020, Month.FEBRUARY, 15))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertTrue(uttaksplan.perioder.size == 2)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 14)), Prosent(100))
        sjekkAvslått(uttaksplan.perioder[1], helePerioden.copy(fom = LocalDate.of(2020, Month.JANUARY, 15)), setOf(AvslåttPeriodeÅrsak.OVERLAPPER_MED_FERIE))
    }

    fun sjekkInnvilget(uttaksperiode: Uttaksperiode, forventetPeriode:LukketPeriode, utbetalingsgrad:Prosent) {
        assertEquals(forventetPeriode.fom, uttaksperiode.periode.fom)
        assertEquals(forventetPeriode.tom, uttaksperiode.periode.tom)
        assertNotNull(uttaksperiode.uttaksperiodeResultat)
        assertEquals(utbetalingsgrad, uttaksperiode.uttaksperiodeResultat?.grad)
        assertTrue(uttaksperiode.uttaksperiodeResultat?.avslåttPeriodeÅrsaker!!.isEmpty())

    }

    fun sjekkAvslått(uttaksperiode: Uttaksperiode, forventetPeriode: LukketPeriode, årsaker:Set<AvslåttPeriodeÅrsak>) {
        assertEquals(forventetPeriode.fom, uttaksperiode.periode.fom)
        assertEquals(forventetPeriode.tom, uttaksperiode.periode.tom)
        assertNotNull(uttaksperiode.uttaksperiodeResultat)
        assertEquals(BigDecimal.ZERO, uttaksperiode.uttaksperiodeResultat?.grad)
        assertEquals(årsaker, uttaksperiode.uttaksperiodeResultat?.avslåttPeriodeÅrsaker)
    }
}