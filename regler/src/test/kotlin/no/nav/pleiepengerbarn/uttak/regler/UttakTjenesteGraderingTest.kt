package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkAvslått
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.util.*

internal class UttakTjenesteGraderingTest {

    private companion object {val FULL_UKE: Duration = Duration.ofHours(37).plusMinutes(30)}

    private val arbeidsforhold1 = UUID.randomUUID().toString()
    private val arbeidsforhold2 = UUID.randomUUID().toString()
    private val arbeidsforhold3 = UUID.randomUUID().toString()
    private val arbeidsforhold4 = UUID.randomUUID().toString()

    private val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
    private val annenPartInnvilgetÅrsak = InnvilgetÅrsak(
            årsak = InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT,
            hjemler = setOf(
                    Hjemmel(
                            henvisning = "En lovtekst",
                            anvendelse = "Til testformål"
                    )
            )
    )

    @Test
    fun `En uttaksperiode med overlappende tilsynsperiode skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                tilsynsperioder = mapOf(
                        helePerioden to Tilsyn(Prosent(20))
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent.ZERO))
                ).somArbeid()

        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(80.00), mapOf(arbeidsforhold1 to Prosent(80.00)), InnvilgetÅrsaker.GRADERT_MOT_TILSYN)
    }

    @Test
    fun `En uttaksperiode med overlappende arbeidsperiode skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent(25)))
                ).somArbeid()
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(75), mapOf(arbeidsforhold1 to Prosent(75)), InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
    }

    @Test
    fun `En uttaksperiode med overlappende arbeidsperiode og uttak på annen part skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                andrePartersUttaksplan = listOf(
                        Uttaksplan(perioder = mapOf(helePerioden to InnvilgetPeriode(grad = Prosent(40), utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(40)).somUtbetalingsgrader(), årsak = annenPartInnvilgetÅrsak)))
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent(25)))
                ).somArbeid()
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(60), mapOf(arbeidsforhold1 to Prosent(60)), InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
    }

    @Test
    fun `En uttaksperiode med tilsyn og uttak på annen part skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                andrePartersUttaksplan = listOf(
                        Uttaksplan(perioder = mapOf(helePerioden to InnvilgetPeriode(grad = Prosent(40), utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(40)).somUtbetalingsgrader(), årsak = annenPartInnvilgetÅrsak)))
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent(25)))
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        helePerioden to Tilsyn(grad = Prosent(30))
                )

        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(30), mapOf(arbeidsforhold1 to Prosent(30)), InnvilgetÅrsaker.GRADERT_MOT_TILSYN)
    }

    @Test
    fun `En uttaksperiode med tilsyn og uttak på annen part som tilsammen er over 80% skal føre til avslag`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                andrePartersUttaksplan = listOf(
                        Uttaksplan(perioder = mapOf(helePerioden to InnvilgetPeriode(grad = Prosent(40), utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(40)).somUtbetalingsgrader(), årsak = annenPartInnvilgetÅrsak )))
                ),
                tilsynsperioder = mapOf(
                        helePerioden to Tilsyn(grad = Prosent(45))
                )

        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkAvslått(uttaksplan, helePerioden, setOf(AvslåttÅrsaker.FOR_LAV_GRAD))
    }

    @Test
    fun `En uttaksperiode med mer arbeid enn tilsyn, så skal perioden graderes mot arbeid`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent(35)))
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        helePerioden to Tilsyn(Prosent(30))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(65), mapOf(arbeidsforhold1 to Prosent(65)), InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
    }


    @Test
    fun `En uttaksperiode med mer tilsyn enn arbeid, så skal perioden graderes mot tilsyn`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent(25)))
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        helePerioden to Tilsyn(Prosent(30))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(70), mapOf(arbeidsforhold1 to Prosent(70)), InnvilgetÅrsaker.GRADERT_MOT_TILSYN)
    }


    @Test
    fun `En uttaksperiode med gradering i en deltidsjobb`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_UKE.dividedBy(2), Prosent(25)))
                ).somArbeid()
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent("37.5"), mapOf(arbeidsforhold1 to Prosent(75)), InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
    }


    @Test
    fun `En uttaksperioder med fire arbeidsforhold som skal vurderes til gradering mot arbeid`() {
        val enUke = LukketPeriode(LocalDate.of(2020,Month.JANUARY, 1), LocalDate.of(2020,Month.JANUARY, 7))
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        enUke to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        enUke
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(enUke to ArbeidsforholdPeriodeInfo(FULL_UKE.minusHours(30), Prosent(40))),
                        arbeidsforhold2 to mapOf(enUke to ArbeidsforholdPeriodeInfo(FULL_UKE.minusHours(30), Prosent(20))),
                        arbeidsforhold3 to mapOf(enUke to ArbeidsforholdPeriodeInfo(FULL_UKE.minusHours(30), Prosent(80))),
                        arbeidsforhold4 to mapOf(enUke to ArbeidsforholdPeriodeInfo(FULL_UKE.minusHours(30), Prosent(0)))
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        enUke to Tilsyn(Prosent(40))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, enUke, Prosent(52), mapOf(
                arbeidsforhold1 to Prosent(60),
                arbeidsforhold2 to Prosent(80),
                arbeidsforhold3 to Prosent(20),
                arbeidsforhold4 to Prosent(100)
        ), InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)

    }

    @Test
    fun `En søknadsperioder med forskjellige arbeidsprosenter skal graderes mot arbeid`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)) to ArbeidsforholdPeriodeInfo(jobberNormaltPerUke = FULL_UKE, skalJobbeProsent = Prosent(10)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)) to ArbeidsforholdPeriodeInfo(jobberNormaltPerUke = FULL_UKE, skalJobbeProsent = Prosent(20)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 31)) to ArbeidsforholdPeriodeInfo(jobberNormaltPerUke = FULL_UKE, skalJobbeProsent = Prosent(30))
                        )
                ).somArbeid()
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(3)
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)), Prosent(90), mapOf(), InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)), Prosent(80), mapOf(), InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 31)), Prosent(70), mapOf(), InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
    }

    @Test
    fun `En søknadsperioder med forskjellige arbeidsprosenter skal graderes mot arbeid og tilsyn`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)) to ArbeidsforholdPeriodeInfo(jobberNormaltPerUke = FULL_UKE, skalJobbeProsent = Prosent(10)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)) to ArbeidsforholdPeriodeInfo(jobberNormaltPerUke = FULL_UKE, skalJobbeProsent = Prosent(20)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 31)) to ArbeidsforholdPeriodeInfo(jobberNormaltPerUke = FULL_UKE, skalJobbeProsent = Prosent(30))
                        )
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        LukketPeriode(LocalDate.of(2020, Month.JANUARY, 25), LocalDate.of(2020, Month.JANUARY, 31)) to Tilsyn(Prosent(35))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(4)
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)), Prosent(90), mapOf(), InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)), Prosent(80), mapOf(), InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 24)), Prosent(70), mapOf(), InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 25), LocalDate.of(2020, Month.JANUARY, 31)), Prosent(65), mapOf(), InnvilgetÅrsaker.GRADERT_MOT_TILSYN)
    }

}