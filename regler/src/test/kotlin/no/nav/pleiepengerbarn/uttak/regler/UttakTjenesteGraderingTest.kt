package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class UttakTjenesteGraderingTest {

    private val arbeidsforhold1 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "123456789")

    @Test
    fun `En uttaksperiode med overlappende tilsynsperiode skal føre til redusert grad på uttaksperiode`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = listOf(
                        Tilsynsbehov(helePerioden, TilsynsbehovStørrelse.PROSENT_100)
                ),
                søktePerioder = listOf(
                        helePerioden
                ),
                tilsynPerioder = listOf(
                        Tilsyn(periode = helePerioden, grad = Prosent(20))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(80))
    }

    @Test
    fun `En uttaksperiode med overlappende arbeidsperiode skal føre til redusert grad på uttaksperiode`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
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
                                        arbeidsforhold = arbeidsforhold1,
                                        periode = helePerioden,
                                        arbeidsprosent = Prosent(25),
                                        inntekt = Beløp(1000)
                                )
                        )
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(75))
    }

    @Test
    fun `En uttaksperiode med overlappende arbeidsperiode og overlappende tilsyn så skal tilsynsgrad overstyre arbeidsgradgrad dersom tilsynsgrad er større enn arbeidsgrad`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
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
                                        arbeidsforhold = arbeidsforhold1,
                                        periode = helePerioden,
                                        arbeidsprosent = Prosent(25),
                                        inntekt = Beløp(1000)
                                )
                        )
                ),
                tilsynPerioder = listOf(
                        Tilsyn(periode = helePerioden, grad = Prosent(30))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(70))
    }


    @Test
    fun `En uttaksperiode med gradering mot arbeids for overlappende arbeidsperiode og overlappende tilsyn så skal tilsynsgrad overstyre arbeidsgradgrad dersom tilsynsgrad er større enn arbeidsgrad`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
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
                                        arbeidsforhold = arbeidsforhold1,
                                        periode = helePerioden,
                                        arbeidsprosent = Prosent(25),
                                        inntekt = Beløp(1000)
                                )
                        )
                ),
                tilsynPerioder = listOf(
                        Tilsyn(periode = helePerioden, grad = Prosent(30))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder[0], helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(70))
    }




}