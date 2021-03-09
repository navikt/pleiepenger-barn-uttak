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

internal class UttakTjenesteTest {

    private companion object {
        private val FULL_DAG = Duration.ofHours(7).plusMinutes(30)
        private val INGENTING = Duration.ZERO

        private val arbeidsforhold1 = UUID.randomUUID().toString()

        private const val aktørIdSøker = "123"
        private const val aktørIdBarn = "456"
    }


    @Test
    fun `Enkel uttaksperiode uten annen informasjon`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_200
                ),
                søktUttak = listOf(
                        SøktUttak(helePerioden)
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
                ).somArbeid(),
                behandlingUUID = nesteBehandlingId()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), Årsak.FULL_DEKNING)
    }


    @Test
    fun `En uttaksperiode som delvis overlapper med ferie`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_200
                ),
                søktUttak = listOf(
                    SøktUttak(helePerioden)
                ),
                lovbestemtFerie = listOf(
                        LukketPeriode(LocalDate.of(2020, Month.JANUARY, 15), LocalDate.of(2020, Month.FEBRUARY, 15))
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
                ).somArbeid(),
                behandlingUUID = nesteBehandlingId()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkOppfylt(uttaksplan, helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 14)), Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), Årsak.FULL_DEKNING)
        sjekkIkkeOppfylt(uttaksplan, helePerioden.copy(fom = LocalDate.of(2020, Month.JANUARY, 15)), setOf(Årsak.LOVBESTEMT_FERIE))
    }

    @Test
    fun `En uttaksperiode som fortsetter etter slutt på pleiebehov perioden, skal avslås fra slutt på pleiebehov perioden`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_200
                ),
                søktUttak = listOf(
                    SøktUttak(LukketPeriode(helePerioden.fom, helePerioden.tom.plusDays(7)))
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
                ).somArbeid(),
                behandlingUUID = nesteBehandlingId()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkOppfylt(uttaksplan, helePerioden, Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), Årsak.FULL_DEKNING)
        sjekkIkkeOppfylt(uttaksplan, LukketPeriode(helePerioden.tom.plusDays(1), helePerioden.tom.plusDays(7)), setOf(Årsak.UTENOM_PLEIEBEHOV))
    }

    @Test
    fun `En uttaksperiode som overlapper med tilsyn slik at uttaksgraden blir under 20 prosent, skal avslås pga for høy tilsynsgrad`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                behandlingUUID = UUID.randomUUID().toString(),
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_100
                ),
                tilsynsperioder = mapOf(
                        helePerioden.copy(fom = helePerioden.fom.plusDays(15)) to Prosent(85)
                ).somTilsynperioder(),
                søktUttak = listOf(
                        SøktUttak(helePerioden)
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, Duration.ZERO))
                ).somArbeid()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkOppfylt(uttaksplan, helePerioden.copy(tom = helePerioden.fom.plusDays(15).minusDays(1)), Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), Årsak.FULL_DEKNING)
        sjekkIkkeOppfylt(uttaksplan, helePerioden.copy(fom = helePerioden.fom.plusDays(15)), setOf(Årsak.FOR_HØY_TILSYNSGRAD))
    }

    @Test
    fun `Får jobbet siste halvdel av en perioder`() {
        val søknadsperiode = LukketPeriode("2020-03-09/2020-03-22")
        val periode1 = LukketPeriode("2020-03-09/2020-03-15")
        val periode2 = LukketPeriode("2020-03-16/2020-03-22")

        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        søknadsperiode to Pleiebehov.PROSENT_100
                ),
                søktUttak = listOf(SøktUttak(søknadsperiode)),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(
                                periode1 to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING),
                                periode2 to ArbeidsforholdPeriodeInfo(FULL_DAG, FULL_DAG.prosent(20))
                        )
                ).somArbeid(),
                behandlingUUID = nesteBehandlingId()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)
        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = periode1,
                forventetGrad = Prosent(100),
                forventedeUtbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(100)),
                forventedeOppfyltÅrsak = Årsak.FULL_DEKNING
        )
        sjekkOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = periode2,
                forventetGrad = Prosent(80),
                forventedeUtbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(80)),
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT
        )
    }

    @Test
    fun `Det skal ikke avkortes mot tilsyn under 10 prosent`() {
        val periode = LukketPeriode("2020-03-09/2020-03-15")

        val grunnlag = RegelGrunnlag(
                behandlingUUID = UUID.randomUUID().toString(),
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        periode to Pleiebehov.PROSENT_100
                ),
                søktUttak = listOf(SøktUttak(periode)),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(
                                periode to ArbeidsforholdPeriodeInfo(FULL_DAG, Duration.ZERO)
                        )
                ).somArbeid(),
                tilsynsperioder = mapOf(
                        periode to Prosent(9)
                ).somTilsynperioder()
        )
        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)

        sjekkOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = periode,
                forventetGrad = Prosent(100),
                forventedeUtbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(100)),
                forventedeOppfyltÅrsak = Årsak.FULL_DEKNING
        )

    }

    private fun nesteBehandlingId(): BehandlingUUID = UUID.randomUUID().toString()

}