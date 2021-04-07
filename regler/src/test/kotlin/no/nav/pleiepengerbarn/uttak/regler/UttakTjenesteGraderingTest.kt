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
        private val INGENTING = Duration.ZERO

        private val arbeidsforhold1 = UUID.randomUUID().toString()
        private val arbeidsforhold2 = UUID.randomUUID().toString()
        private val arbeidsforhold3 = UUID.randomUUID().toString()

        private const val aktørIdSøker = "123"
        private const val aktørIdBarn = "456"
    }


    private val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
    private val helePeriodenSøktUttak = SøktUttak(helePerioden)

    @Test
    internal fun `En uttaksperiode med overlappende tilsynsperiode skal føre til redusert grad på uttaksperiode`() {
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
                behandlingUUID = nesteBehandlingUUID()

        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(80.00), mapOf(arbeidsforhold1 to Prosent(80.00)), Årsak.GRADERT_MOT_TILSYN)
    }

    @Test
    internal fun `En uttaksperiode med overlappende tilsynsperiode som er under 10 prosent skal ikke føre til redusert grad`() {
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
                helePerioden to Prosent(9)
            ).somTilsynperioder(),
            arbeid = mapOf(
                arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
            ).somArbeid(),
            behandlingUUID = nesteBehandlingUUID()

        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), Årsak.FULL_DEKNING, OverseEtablertTilsynÅrsak.FOR_LAVT)
    }

    @Test
    internal fun `En uttaksperiode med overlappende tilsynsperiode så skal periodene som overlapper med beredskap og nattevåk ikke få redusert grad`() {
        val perioden = LukketPeriode("2020-01-01/2020-01-20")
        val grunnlag = RegelGrunnlag(
            søker = Søker(
                aktørId = aktørIdSøker
            ),
            barn = Barn(
                aktørId = aktørIdBarn
            ),
            pleiebehov = mapOf(
                perioden to Pleiebehov.PROSENT_100
            ),
            søktUttak = listOf(
                SøktUttak(perioden)
            ),
            tilsynsperioder = mapOf(
                perioden to Prosent(60)
            ).somTilsynperioder(),
            arbeid = mapOf(
                arbeidsforhold1 to mapOf(perioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
            ).somArbeid(),
            behandlingUUID = nesteBehandlingUUID(),
            beredskapsperioder = setOf(LukketPeriode("2020-01-05/2020-01-12")),
            nattevåksperioder = setOf(LukketPeriode("2020-01-08/2020-01-15"))

        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(5)
        sjekkOppfylt(uttaksplan, LukketPeriode("2020-01-01/2020-01-04"), Prosent(40), mapOf(arbeidsforhold1 to Prosent(40)), Årsak.GRADERT_MOT_TILSYN)
        sjekkOppfylt(uttaksplan, LukketPeriode("2020-01-05/2020-01-07"), Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), Årsak.FULL_DEKNING, OverseEtablertTilsynÅrsak.BEREDSKAP)
        sjekkOppfylt(uttaksplan, LukketPeriode("2020-01-08/2020-01-12"), Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), Årsak.FULL_DEKNING, OverseEtablertTilsynÅrsak.NATTEVÅK_OG_BEREDSKAP)
        sjekkOppfylt(uttaksplan, LukketPeriode("2020-01-13/2020-01-15"), Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), Årsak.FULL_DEKNING, OverseEtablertTilsynÅrsak.NATTEVÅK)
        sjekkOppfylt(uttaksplan, LukketPeriode("2020-01-16/2020-01-20"), Prosent(40), mapOf(arbeidsforhold1 to Prosent(40)), Årsak.GRADERT_MOT_TILSYN)
    }


    @Test
    internal fun `En uttaksperiode med overlappende arbeidsperiode skal føre til redusert grad på uttaksperiode`() {
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
                behandlingUUID = nesteBehandlingUUID()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(25), mapOf(arbeidsforhold1 to Prosent(25)), Årsak.AVKORTET_MOT_INNTEKT)
    }

    @Test
    internal fun `En uttaksperiode med overlappende arbeidsperiode og uttak på annen part skal føre til redusert grad på uttaksperiode`() {
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
                                kildeBehandlingUUID = nesteBehandlingUUID(),
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
                behandlingUUID = nesteBehandlingUUID()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(25), mapOf(arbeidsforhold1 to Prosent(25)), Årsak.AVKORTET_MOT_INNTEKT)
    }

    @Test
    internal fun `En uttaksperiode med tilsyn og uttak på annen part skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                behandlingUUID = nesteBehandlingUUID(),
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
                        "999" to Uttaksplan(perioder = mapOf(helePerioden to UttaksperiodeInfo.oppfylt(
                            uttaksgrad = Prosent(40),
                            utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(40)).somUtbetalingsgrader(),
                            årsak = Årsak.AVKORTET_MOT_INNTEKT,
                            knekkpunktTyper = setOf(),
                            annenPart = AnnenPart.ALENE,
                            kildeBehandlingUUID = UUID.randomUUID().toString(),
                            pleiebehov = Prosent(100),
                            søkersTapteArbeidstid = Prosent(40)
                        )))
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.prosent(40)))
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        helePerioden to Prosent(30)
                ).somTilsynperioder()

        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(30), mapOf(arbeidsforhold1 to Prosent(30)), Årsak.GRADERT_MOT_TILSYN)
    }

    @Test
    internal fun `En uttaksperiode med tilsyn og uttak på annen part som tilsammen er over 80 prosent skal føre til avslag`() {
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
                                kildeBehandlingUUID = nesteBehandlingUUID(),
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
                behandlingUUID = nesteBehandlingUUID(),
                arbeid = listOf(Arbeid(Arbeidsforhold(type="frilans"), mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))))

        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkIkkeOppfylt(uttaksplan, helePerioden, setOf(Årsak.FOR_LAV_REST_PGA_ETABLERT_TILSYN_OG_ANDRE_SØKERE))
    }

    @Test
    internal fun `En uttaksperiode med mer arbeid enn tilsyn, så skal perioden graderes mot arbeid`() {
        val grunnlag = RegelGrunnlag(
                behandlingUUID = nesteBehandlingUUID(),
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
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.prosent(65)))
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
    internal fun `En uttaksperiode med mer tilsyn enn arbeid, så skal perioden graderes mot tilsyn`() {
        val grunnlag = RegelGrunnlag(
                behandlingUUID = nesteBehandlingUUID(),
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
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.prosent(60)))
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        helePerioden to Prosent(70)
                ).somTilsynperioder()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(30), mapOf(arbeidsforhold1 to Prosent(30)), Årsak.GRADERT_MOT_TILSYN)
    }

    @Test
    internal fun `En uttaksperiode med gradering i en deltidsjobb`() {
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
                behandlingUUID = nesteBehandlingUUID()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(50), mapOf(arbeidsforhold1 to Prosent(50)), Årsak.AVKORTET_MOT_INNTEKT)
    }

    @Test
    internal fun `En uttaksperioder med tre arbeidsforhold som skal vurderes til gradering mot arbeid`() {
        val enUke = LukketPeriode(LocalDate.of(2020,Month.JANUARY, 1), LocalDate.of(2020,Month.JANUARY, 7))
        val grunnlag = RegelGrunnlag(
                behandlingUUID = nesteBehandlingUUID(),
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        enUke to Pleiebehov.PROSENT_100
                ),
                søktUttak = listOf(
                        SøktUttak(enUke)
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(enUke to ArbeidsforholdPeriodeInfo(Duration.ofHours(3), Duration.ZERO)), // 0 % arbeid
                        arbeidsforhold2 to mapOf(enUke to ArbeidsforholdPeriodeInfo(Duration.ofHours(2), Duration.ofHours(1))), // 50 % arbeid
                        arbeidsforhold3 to mapOf(enUke to ArbeidsforholdPeriodeInfo(Duration.ofHours(4), Duration.ofHours(3))), // 75 % arbeid
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        enUke to Prosent(40) // 40 % etabelert tilsyn
                ).somTilsynperioder()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, enUke, Prosent(56), mapOf(
                arbeidsforhold1 to Prosent(100),
                arbeidsforhold2 to Prosent(50),
                arbeidsforhold3 to Prosent(25),
        ), Årsak.AVKORTET_MOT_INNTEKT)

    }



    @Test
    internal fun `En søknadsperioder med forskjellige arbeidsprosenter skal graderes mot arbeid`() {
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
                behandlingUUID = nesteBehandlingUUID()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(3)
        sjekkIkkeOppfylt(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)), setOf(Årsak.FOR_LAV_TAPT_ARBEIDSTID))
        sjekkOppfylt(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)), Prosent(20), mapOf(), Årsak.AVKORTET_MOT_INNTEKT)
        sjekkOppfylt(uttaksplan, LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 31)), Prosent(30), mapOf(), Årsak.AVKORTET_MOT_INNTEKT)
    }

    @Test
    internal fun `En søknadsperioder med forskjellige arbeidsprosenter skal graderes mot arbeid og tilsyn`() {
        val grunnlag = RegelGrunnlag(
                behandlingUUID =nesteBehandlingUUID(),
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
                        SøktUttak(helePerioden)
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 9)) to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(10)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 10), LocalDate.of(2020, Month.JANUARY, 19)) to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(20)),
                                LukketPeriode(LocalDate.of(2020, Month.JANUARY, 20), LocalDate.of(2020, Month.JANUARY, 31)) to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(30))
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

    private fun nesteBehandlingUUID(): BehandlingUUID = UUID.randomUUID().toString()


}


