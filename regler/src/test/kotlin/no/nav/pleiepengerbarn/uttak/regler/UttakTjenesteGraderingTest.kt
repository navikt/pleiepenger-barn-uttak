package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkIkkeOppfylt
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkOppfylt
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.util.*

internal class UttakTjenesteGraderingTest {

    private companion object {
        private val FULL_DAG = Duration.ofHours(7).plusMinutes(30)
        private val INGENTING = Duration.ZERO

        private val arbeidsforhold1 = UUID.randomUUID().toString()
        private val arbeidsforhold2 = UUID.randomUUID().toString()
        private val arbeidsforhold3 = UUID.randomUUID().toString()
        private val arbeidsforhold4 = UUID.randomUUID().toString()

        private val aktørIdSøker = "123"
        private val aktørIdBarn = "456"
    }


    private val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
    private val helePeriodenSøktUttak = SøktUttak(helePerioden)

    @Test
    fun `En uttaksperiode med overlappende tilsynsperiode skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_100
                ),
                søktUttak = listOf(
                    helePeriodenSøktUttak
                ),
                tilsynsperioder = mapOf(
                        helePerioden to Prosent(20)
                ).somTilsynperioder(),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
                ).somArbeid(),
                behandlingUUID = nesteBehandlingId()

        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(80.00), mapOf(arbeidsforhold1 to Prosent(80.00)), Årsak.GRADERT_MOT_TILSYN)
    }

    @Test
    fun `En uttaksperiode med overlappende arbeidsperiode skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_100
                ),
                søktUttak = listOf(
                    helePeriodenSøktUttak
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.prosent(75)))
                ).somArbeid(),
                behandlingUUID = nesteBehandlingId()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(25), mapOf(arbeidsforhold1 to Prosent(25)), Årsak.AVKORTET_MOT_INNTEKT)
    }

    @Test
    fun `En uttaksperiode med overlappende arbeidsperiode og uttak på annen part skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_100
                ),
                søktUttak = listOf(
                    helePeriodenSøktUttak
                ),
                andrePartersUttaksplan = mapOf(
                        "999" to Uttaksplan(perioder = mapOf(
                            helePerioden to UttaksperiodeInfo.oppfylt(
                                kildeBehandlingUUID = nesteBehandlingId(),
                                uttaksgrad = Prosent(40),
                                utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(40)).somUtbetalingsgrader(),
                                søkersTapteArbeidstid = Prosent(40),
                                årsak = Årsak.AVKORTET_MOT_INNTEKT, knekkpunktTyper = setOf(),
                                pleiebehov = Pleiebehov.PROSENT_100.prosent,
                                annenPart = AnnenPart.ALENE
                            )
                        ))
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.prosent(75)))
                ).somArbeid(),
                behandlingUUID = nesteBehandlingId()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(25), mapOf(arbeidsforhold1 to Prosent(25)), Årsak.AVKORTET_MOT_INNTEKT)
    }

    /*
    TODO: utkommentert til tilsyn er implementert
    @Test
    fun `En uttaksperiode med tilsyn og uttak på annen part skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker,
                        fødselsdato = LocalDate.now().minusYears(20)
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePeriodenSøktUttak
                ),
                andrePartersUttaksplan = mapOf(
                        "999" to Uttaksplan(perioder = mapOf(helePerioden to OppfyltPeriode(grad = Prosent(40), utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(40)).somUtbetalingsgrader(), årsak = annenPartOppfyltÅrsak)))
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.dividedBy(4L), FULL_DAG.dividedBy(4L)))
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        helePerioden to Prosent(30)
                ).somTilsynperioder()

        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(30), mapOf(arbeidsforhold1 to Prosent(30)), Årsak.GRADERT_MOT_TILSYN)
    }

     */

    @Test
    fun `En uttaksperiode med tilsyn og uttak på annen part som tilsammen er over 80 prosent skal føre til avslag`() {
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_100
                ),
                søktUttak = listOf(
                    helePeriodenSøktUttak
                ),
                andrePartersUttaksplan = mapOf(
                        "999" to Uttaksplan(perioder = mapOf(
                            helePerioden to UttaksperiodeInfo.oppfylt(
                                kildeBehandlingUUID = nesteBehandlingId(),
                                uttaksgrad = Prosent(40),
                                utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(40)).somUtbetalingsgrader(),
                                søkersTapteArbeidstid = Prosent(40),
                                årsak = Årsak.AVKORTET_MOT_INNTEKT,
                                pleiebehov = Pleiebehov.PROSENT_100.prosent,
                                knekkpunktTyper = setOf(),
                                annenPart = AnnenPart.ALENE
                            )
                        ))
                ),
                tilsynsperioder = mapOf(
                        helePerioden to Prosent(45)
                ).somTilsynperioder(),
                behandlingUUID = nesteBehandlingId(),
                arbeid = listOf(Arbeid(Arbeidsforhold(type="frilans"), mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))))

        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkIkkeOppfylt(uttaksplan, helePerioden, setOf(Årsak.FOR_LAV_GRAD))
    }

    /*

     TODO: legg inn igjen når tilsyn er implementert
    @Test
    fun `En uttaksperiode med mer arbeid enn tilsyn, så skal perioden graderes mot arbeid`() {
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker,
                        fødselsdato = LocalDate.now().minusYears(20)
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePeriodenSøktUttak
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.prosent(35), FULL_DAG.prosent(35)))
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        helePerioden to Prosent(30)
                ).somTilsynperioder()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(35), mapOf(arbeidsforhold1 to Prosent(35)), Årsak.AVKORTET_MOT_INNTEKT)
    }

    @Test
    fun `En uttaksperiode med mer tilsyn enn arbeid, så skal perioden graderes mot tilsyn`() {
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker,
                        fødselsdato = LocalDate.now().minusYears(20)
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePeriodenSøktUttak
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.dividedBy(4), FULL_DAG.dividedBy(4)))
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        helePerioden to Prosent(30)
                ).somTilsynperioder()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(30), mapOf(arbeidsforhold1 to Prosent(30)), Årsak.GRADERT_MOT_TILSYN)
    }
*/


    @Test
    fun `En uttaksperiode med gradering i en deltidsjobb`() {
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_100
                ),
                søktUttak = listOf(
                    helePeriodenSøktUttak
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG.dividedBy(2), FULL_DAG.prosent(25)))
                ).somArbeid(),
                behandlingUUID = nesteBehandlingId()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(50), mapOf(arbeidsforhold1 to Prosent(50)), Årsak.AVKORTET_MOT_INNTEKT)
    }

/*

TODO: fiks til realistiske arbeidsforhold
    @Test
    fun `En uttaksperioder med fire arbeidsforhold som skal vurderes til gradering mot arbeid`() {
        val enUke = LukketPeriode(LocalDate.of(2020,Month.JANUARY, 1), LocalDate.of(2020,Month.JANUARY, 7))
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker,
                        fødselsdato = LocalDate.now().minusYears(20)
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                tilsynsbehov = mapOf(
                        enUke to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        enUke
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(enUke to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.prosent(40), FULL_DAG.prosent(40))),
                        arbeidsforhold2 to mapOf(enUke to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.prosent(20), FULL_DAG.prosent(20))),
                        arbeidsforhold3 to mapOf(enUke to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.prosent(80), FULL_DAG.prosent(80))),
                        arbeidsforhold4 to mapOf(enUke to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.prosent(0), FULL_DAG.prosent(0)))
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        enUke to Prosent(40)
                ).somTilsynperioder()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, enUke, Prosent(52), mapOf(
                arbeidsforhold1 to Prosent(60),
                arbeidsforhold2 to Prosent(80),
                arbeidsforhold3 to Prosent(20),
                arbeidsforhold4 to Prosent(100)
        ), Årsak.AVKORTET_MOT_INNTEKT)

    }


 */

    @Test
    fun `En søknadsperioder med forskjellige arbeidsprosenter skal graderes mot arbeid`() {
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_100
                ),
                søktUttak = listOf(
                    helePeriodenSøktUttak
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)) to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(90)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)) to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(80)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 31)) to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(70))
                        )
                ).somArbeid(),
                behandlingUUID = nesteBehandlingId()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(3)
        sjekkIkkeOppfylt(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)), setOf(Årsak.FOR_LAV_GRAD))
        sjekkOppfylt(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)), Prosent(20), mapOf(), Årsak.AVKORTET_MOT_INNTEKT)
        sjekkOppfylt(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 31)), Prosent(30), mapOf(), Årsak.AVKORTET_MOT_INNTEKT)
    }

    /*
    TODO: utkommentert til tilsyn er implementert
    @Test
    fun `En søknadsperioder med forskjellige arbeidsprosenter skal graderes mot arbeid og tilsyn`() {
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker,
                        fødselsdato = LocalDate.now().minusYears(20)
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)) to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG.prosent(10), søkersTilsyn = FULL_DAG.prosent(10)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)) to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG.prosent(20), søkersTilsyn = FULL_DAG.prosent(20)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 31)) to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG.prosent(30), søkersTilsyn = FULL_DAG.prosent(30))
                        )
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        LukketPeriode(LocalDate.of(2020, Month.JANUARY, 25), LocalDate.of(2020, Month.JANUARY, 31)) to Prosent(35)
                ).somTilsynperioder()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(4)
        sjekkOppfylt(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)), Prosent(90), mapOf(), Årsak.AVKORTET_MOT_INNTEKT)
        sjekkOppfylt(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)), Prosent(80), mapOf(), Årsak.AVKORTET_MOT_INNTEKT)
        sjekkOppfylt(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 24)), Prosent(70), mapOf(), Årsak.AVKORTET_MOT_INNTEKT)
        sjekkOppfylt(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 25), LocalDate.of(2020, Month.JANUARY, 31)), Prosent(65), mapOf(), Årsak.GRADERT_MOT_TILSYN)
    }
     */

    private fun nesteBehandlingId(): BehandlingUUID = UUID.randomUUID().toString()


}


