package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.util.*

internal class UttakTjenesteGraderingTest {

    private val arbeidsforhold1 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID())
    private val arbeidsforhold2 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID())
    private val arbeidsforhold3 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "987654321", arbeidsforholdId = UUID.randomUUID())
    private val arbeidsforhold4 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "987654321", arbeidsforholdId = UUID.randomUUID())

    private val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))

    @Test
    fun `En uttaksperiode med overlappende tilsynsperiode skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = listOf(
                        Tilsynsbehov(helePerioden, TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                tilsynsperioder = listOf(
                        Tilsyn(periode = helePerioden, grad = Prosent(20))
                )
        )

        val uttaksplan = kjørRegler(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(80))
    }

    @Test
    fun `En uttaksperiode med overlappende arbeidsperiode skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = listOf(
                        Tilsynsbehov(helePerioden, TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeidsforhold = listOf(
                        arbeidsforhold1.copy(perioder = mapOf(helePerioden to ArbeidsforholdPeriodeInfo(inntekt = Beløp(1000), arbeidsprosent = Prosent(25))))
                )
        )

        val uttaksplan = kjørRegler(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(75))
    }

    @Test
    fun `En uttaksperiode med overlappende arbeidsperiode og uttak på annen part skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = listOf(
                        Tilsynsbehov(helePerioden, TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                andrePartersUttaksplan = listOf(Uttaksplan(perioder = listOf(
                        Uttaksperiode(periode = helePerioden, uttaksperiodeResultat = UttaksperiodeResultat(
                                grad = Prosent(40)
                        ))
                ))),
                arbeidsforhold = listOf(
                        arbeidsforhold1.copy(perioder = mapOf(helePerioden to ArbeidsforholdPeriodeInfo(inntekt = Beløp(1000), arbeidsprosent = Prosent(25))))
                )
        )

        val uttaksplan = kjørRegler(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(60))
    }


    @Test
    fun `En uttaksperiode med mer arbeid enn tilsyn, så skal perioden graderes mot arbeid`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = listOf(
                        Tilsynsbehov(helePerioden, TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeidsforhold = listOf(
                        arbeidsforhold1.copy(perioder = mapOf(helePerioden to ArbeidsforholdPeriodeInfo(inntekt = Beløp(1000), arbeidsprosent = Prosent(35))))
                ),
                tilsynsperioder = listOf(
                        Tilsyn(periode = helePerioden, grad = Prosent(30))
                )
        )

        val uttaksplan = kjørRegler(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(65))
    }


    @Test
    fun `En uttaksperiode med mer tilsyn enn arbeid, så skal perioden graderes mot arbeid`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = listOf(
                        Tilsynsbehov(helePerioden, TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeidsforhold = listOf(
                        arbeidsforhold1.copy(perioder = mapOf(helePerioden to ArbeidsforholdPeriodeInfo(inntekt = Beløp(1000), arbeidsprosent = Prosent(25))))
                ),
                tilsynsperioder = listOf(
                        Tilsyn(periode = helePerioden, grad = Prosent(30))
                )
        )

        val uttaksplan = kjørRegler(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(70))
    }

    @Test
    fun `En uttaksperioder med fire arbeidsforhold som skal vurderes til gradering mot tilsyn`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = listOf(
                        Tilsynsbehov(helePerioden, TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeidsforhold = listOf(
                        arbeidsforhold1.copy(perioder = mapOf(helePerioden to ArbeidsforholdPeriodeInfo(inntekt = Beløp(2000), arbeidsprosent = Prosent(40)))),
                        arbeidsforhold1.copy(perioder = mapOf(helePerioden to ArbeidsforholdPeriodeInfo(inntekt = Beløp(1500), arbeidsprosent = Prosent(20)))),
                        arbeidsforhold1.copy(perioder = mapOf(helePerioden to ArbeidsforholdPeriodeInfo(inntekt = Beløp(500), arbeidsprosent = Prosent(80)))),
                        arbeidsforhold1.copy(perioder = mapOf(helePerioden to ArbeidsforholdPeriodeInfo(inntekt = Beløp(1000), arbeidsprosent = Prosent(0))))
                ),
                tilsynsperioder = listOf(
                        Tilsyn(periode = helePerioden, grad = Prosent(40))
                )
        )

        val uttaksplan = kjørRegler(grunnlag)

        sjekkInnvilget(uttaksplan.perioder[0], helePerioden, Prosent(60))

    }

    private fun kjørRegler(grunnlag: RegelGrunnlag):Uttaksplan {
        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)
        PrintGrunnlagOgUttaksplan(grunnlag, uttaksplan).print()
        return uttaksplan
    }


}