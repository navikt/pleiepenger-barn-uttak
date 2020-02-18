package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
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
                søktePerioder = listOf(
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
                søktePerioder = listOf(
                        helePerioden
                ),
                arbeidsforhold = mapOf(
                        arbeidsforhold1 to listOf(
                                Arbeid(
                                        periode = helePerioden,
                                        arbeidsprosent = Prosent(25),
                                        inntekt = Beløp(1000)
                                )
                        )
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
                søktePerioder = listOf(
                        helePerioden
                ),
                andrePartersUttaksplan = listOf(Uttaksplan(perioder = listOf(
                    Uttaksperiode(periode = helePerioden, uttaksperiodeResultat = UttaksperiodeResultat(
                            grad = Prosent(40)
                    ))
                ))),
                arbeidsforhold = mapOf(
                        arbeidsforhold1 to listOf(
                                Arbeid(
                                        periode = helePerioden,
                                        arbeidsprosent = Prosent(25),
                                        inntekt = Beløp(1000)
                                )
                        )
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
                søktePerioder = listOf(
                        helePerioden
                ),
                arbeidsforhold = mapOf(
                        arbeidsforhold1 to listOf(
                                Arbeid(
                                        periode = helePerioden,
                                        arbeidsprosent = Prosent(35),
                                        inntekt = Beløp(1000)
                                )
                        )
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
                søktePerioder = listOf(
                        helePerioden
                ),
                arbeidsforhold = mapOf(
                        arbeidsforhold1 to listOf(
                                Arbeid(
                                        periode = helePerioden,
                                        arbeidsprosent = Prosent(25),
                                        inntekt = Beløp(1000)
                                )
                        )
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
                søktePerioder = listOf(
                        helePerioden
                ),
                arbeidsforhold = mapOf(
                        arbeidsforhold1 to listOf(
                                Arbeid(
                                        periode = helePerioden,
                                        arbeidsprosent = Prosent(40),
                                        inntekt = Beløp(2000)
                                )
                        ),
                        arbeidsforhold2 to listOf(
                                Arbeid(
                                        periode = helePerioden,
                                        arbeidsprosent = Prosent(20),
                                        inntekt = Beløp(1500)
                                )
                        ),
                        arbeidsforhold3 to listOf(
                                Arbeid(
                                        periode = helePerioden,
                                        arbeidsprosent = Prosent(80),
                                        inntekt = Beløp(500)
                                )
                        ),
                        arbeidsforhold4 to listOf(
                                Arbeid(
                                        periode = helePerioden,
                                        arbeidsprosent = Prosent(0),
                                        inntekt = Beløp(1000)
                                )
                        )
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