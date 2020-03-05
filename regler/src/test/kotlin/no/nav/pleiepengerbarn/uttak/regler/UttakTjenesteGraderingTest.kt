package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.util.*

internal class UttakTjenesteGraderingTest {

    private companion object {val FULL_UKE: Duration = Duration.ofHours(37).plusMinutes(30)}

    private val arbeidsforhold1 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID())
    private val arbeidsforhold2 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID())
    private val arbeidsforhold3 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "987654321", arbeidsforholdId = UUID.randomUUID())
    private val arbeidsforhold4 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "987654321", arbeidsforholdId = UUID.randomUUID())

    private val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))

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
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(FULL_UKE, Prosent.ZERO)))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(80), mapOf(arbeidsforhold1 to Prosent(80)))
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
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(25))))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(75), mapOf(arbeidsforhold1 to Prosent(75)))
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
                        Uttaksplan(perioder = mapOf(helePerioden to InnvilgetPeriode(grad = Prosent(40), utbetalingsgrader = listOf(ArbeidsforholdOgUtbetalingsgrad(arbeidsforhold1, Prosent(40))))
                ))),
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(25))))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(60), mapOf(arbeidsforhold1 to Prosent(60)))
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
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(35))))
                ),
                tilsynsperioder = mapOf(
                        helePerioden to Tilsyn(Prosent(30))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(65), mapOf(arbeidsforhold1 to Prosent(65)))
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
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(25))))
                ),
                tilsynsperioder = mapOf(
                        helePerioden to Tilsyn(Prosent(30))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(70), mapOf(arbeidsforhold1 to Prosent(70)))
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
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE.dividedBy(2), skalJobbe = Prosent(25))))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent("37.5"), mapOf(arbeidsforhold1 to Prosent(75)))
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
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(enUke to ArbeidInfo(jobberNormalt = FULL_UKE.minusHours(30), skalJobbe = Prosent(40)))),
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold2, mapOf(enUke to ArbeidInfo(jobberNormalt = FULL_UKE.minusHours(30), skalJobbe = Prosent(20)))),
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold3, mapOf(enUke to ArbeidInfo(jobberNormalt = FULL_UKE.minusHours(30), skalJobbe = Prosent(80)))),
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold4, mapOf(enUke to ArbeidInfo(jobberNormalt = FULL_UKE.minusHours(30), skalJobbe = Prosent(0))))

                ),
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
        ))

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
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)) to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(10)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)) to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(20)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 31)) to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(30))
                        ))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(3)
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)), Prosent(90))
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)), Prosent(80))
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 31)), Prosent(70))
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
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)) to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(10)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)) to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(20)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 31)) to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(30))
                        ))
                ),
                tilsynsperioder = mapOf(
                        LukketPeriode(LocalDate.of(2020, Month.JANUARY, 25), LocalDate.of(2020, Month.JANUARY, 31)) to Tilsyn(Prosent(35))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(4)
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)), Prosent(90))
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)), Prosent(80))
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 24)), Prosent(70))
        sjekkInnvilget(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 25), LocalDate.of(2020, Month.JANUARY, 31)), Prosent(65))
    }

}