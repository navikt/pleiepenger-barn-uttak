package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkAvslått
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
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

        val uttaksplan = kjørRegler(grunnlag)

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
                ferier = listOf(
                        LukketPeriode(LocalDate.of(2020, Month.JANUARY, 15), LocalDate.of(2020, Month.FEBRUARY, 15))
                )
        )

        val uttaksplan = kjørRegler(grunnlag)

        assertTrue(uttaksplan.perioder.size == 2)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 14)), Prosent(100))
        sjekkAvslått(uttaksplan.perioder[1], helePerioden.copy(fom = LocalDate.of(2020, Month.JANUARY, 15)), setOf(AvslåttPeriodeÅrsak.OVERLAPPER_MED_FERIE))
    }

    @Test
    fun `En uttaksperioder som fortsetter etter slutt på tilsynsbehov perioden, skal avslås fra slutt på tilsynsbehov perioden`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = listOf(
                        Tilsynsbehov(helePerioden, TilsynsbehovStørrelse.PROSENT_200)
                ),
                søktePerioder = listOf(
                        LukketPeriode(helePerioden.fom, helePerioden.tom.plusDays(7))
                )
        )

        val uttaksplan = kjørRegler(grunnlag)

        assertTrue(uttaksplan.perioder.size == 2)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden, Prosent(100))
        sjekkAvslått(uttaksplan.perioder[1], LukketPeriode(helePerioden.tom.plusDays(1), helePerioden.tom.plusDays(7)), setOf(AvslåttPeriodeÅrsak.PERIODE_ETTER_TILSYNSBEHOV))
    }

    private fun kjørRegler(grunnlag: RegelGrunnlag):Uttaksplan {
        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)
        PrintGrunnlagOgUttaksplan(grunnlag, uttaksplan).print()
        return uttaksplan
    }


}