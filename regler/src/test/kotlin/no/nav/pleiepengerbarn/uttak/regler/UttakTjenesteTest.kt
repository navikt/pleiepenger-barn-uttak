package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkIkkeOppfylt
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkOppfylt
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.util.*

internal class UttakTjenesteTest {

    private companion object {
        private val INGENTING = Duration.ZERO

        private val arbeidsforhold1 = UUID.randomUUID().toString()

        private const val aktørIdSøker = "123"
        private const val aktørIdBarn = "456"
    }

    @BeforeEach
    internal fun setUp() {
        System.setProperty("SPESIALHANDTERING_SKAL_GI_HUNDREPROSENT_DATO", "2023-06-01")
        System.setProperty("SPESIALHANDTERING_SKAL_GI_HUNDREPROSENT", "false")
    }

    @AfterEach
    internal fun tearDown() {
        System.clearProperty("SPESIALHANDTERING_SKAL_GI_HUNDREPROSENT_DATO")
        System.clearProperty("SPESIALHANDTERING_SKAL_GI_HUNDREPROSENT")
    }

    @Test
    fun `Enkel uttaksperiode uten annen informasjon`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                saksnummer = nesteSaksnummer(),
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

        assertThat(uttaksplan.perioder).hasSize(5)
        sjekkOppfylt(
            uttaksplan,
            listOf(
                LukketPeriode("2020-01-01/2020-01-03"),
                LukketPeriode("2020-01-06/2020-01-10"),
                LukketPeriode("2020-01-13/2020-01-17"),
                LukketPeriode("2020-01-20/2020-01-24"),
                LukketPeriode("2020-01-27/2020-01-31")
            ),
            HUNDRE_PROSENT,
            mapOf(arbeidsforhold1 to HUNDRE_PROSENT),
            Årsak.FULL_DEKNING)
    }


    @Test
    fun `En uttaksperiode som delvis overlapper med ferie`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                saksnummer = nesteSaksnummer(),
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

        assertThat(uttaksplan.perioder).hasSize(6)
        sjekkOppfylt(
            uttaksplan,
            listOf(
                LukketPeriode("2020-01-01/2020-01-03"),
                LukketPeriode("2020-01-06/2020-01-10"),
                LukketPeriode("2020-01-13/2020-01-14")
            ),
            HUNDRE_PROSENT,
            mapOf(arbeidsforhold1 to HUNDRE_PROSENT),
            Årsak.FULL_DEKNING
        )
        sjekkIkkeOppfylt(
            uttaksplan,
            listOf(
                LukketPeriode("2020-01-15/2020-01-17"),
                LukketPeriode("2020-01-20/2020-01-24"),
                LukketPeriode("2020-01-27/2020-01-31")
            ),
            setOf(Årsak.LOVBESTEMT_FERIE)
        )
    }

    @Test
    fun `En uttaksperiode som fortsetter etter slutt på pleiebehov perioden, skal avslås fra slutt på pleiebehov perioden`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                saksnummer = nesteSaksnummer(),
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

        assertThat(uttaksplan.perioder).hasSize(6)
        sjekkOppfylt(
            uttaksplan,
            listOf(
                LukketPeriode("2020-01-01/2020-01-03"),
                LukketPeriode("2020-01-06/2020-01-10"),
                LukketPeriode("2020-01-13/2020-01-17"),
                LukketPeriode("2020-01-20/2020-01-24"),
                LukketPeriode("2020-01-27/2020-01-31")
            ),
            HUNDRE_PROSENT,
            mapOf(arbeidsforhold1 to HUNDRE_PROSENT),
            Årsak.FULL_DEKNING
        )
        sjekkIkkeOppfylt(uttaksplan, LukketPeriode("2020-02-03/2020-02-07"), setOf(Årsak.UTENOM_PLEIEBEHOV))
    }

    @Test
    fun `En uttaksperiode som overlapper med tilsyn slik at uttaksgraden blir under 20 prosent, skal avslås pga for høy tilsynsgrad`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                saksnummer = nesteSaksnummer(),
                behandlingUUID = UUID.randomUUID(),
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

        assertThat(uttaksplan.perioder).hasSize(6)
        sjekkOppfylt(
            uttaksplan,
            listOf(
                LukketPeriode("2020-01-01/2020-01-03"),
                LukketPeriode("2020-01-06/2020-01-10"),
                LukketPeriode("2020-01-13/2020-01-15")
            ),
            HUNDRE_PROSENT,
            mapOf(arbeidsforhold1 to HUNDRE_PROSENT),
            Årsak.FULL_DEKNING)
        sjekkIkkeOppfylt(
            uttaksplan,
            listOf(
                LukketPeriode("2020-01-16/2020-01-17"),
                LukketPeriode("2020-01-20/2020-01-24"),
                LukketPeriode("2020-01-27/2020-01-31")
            ),
            setOf(Årsak.FOR_LAV_REST_PGA_ETABLERT_TILSYN)
        )
    }

    @Test
    fun `Får jobbet siste halvdel av en perioder`() {
        val søknadsperiode = LukketPeriode("2020-03-09/2020-03-22")
        val periode1 = LukketPeriode("2020-03-09/2020-03-13")
        val periode2 = LukketPeriode("2020-03-16/2020-03-20")

        val grunnlag = RegelGrunnlag(
                saksnummer = nesteSaksnummer(),
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
                forventetGrad = HUNDRE_PROSENT,
                forventedeUtbetalingsgrader = mapOf(arbeidsforhold1 to HUNDRE_PROSENT),
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
                saksnummer = nesteSaksnummer(),
                behandlingUUID = UUID.randomUUID(),
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
                forventetPeriode = LukketPeriode("2020-03-09/2020-03-13"),
                forventetGrad = HUNDRE_PROSENT,
                forventedeUtbetalingsgrader = mapOf(arbeidsforhold1 to HUNDRE_PROSENT),
                forventedeOppfyltÅrsak = Årsak.FULL_DEKNING
        )

    }

    @Test
    fun `Livets sluttfase, En enkel uttaksperiode skal få 100 prosent utbetaling`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 3))
        val grunnlag = RegelGrunnlag(
                saksnummer = nesteSaksnummer(),
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
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
                ).somArbeid(),
                behandlingUUID = nesteBehandlingId()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)

        sjekkOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-03"),
                forventetGrad = HUNDRE_PROSENT,
                forventedeUtbetalingsgrader = mapOf(arbeidsforhold1 to HUNDRE_PROSENT),
                forventedeOppfyltÅrsak = Årsak.FULL_DEKNING
        )
    }

}

private fun nesteSaksnummer(): Saksnummer = UUID.randomUUID().toString().takeLast(19)
private fun nesteBehandlingId() = UUID.randomUUID()
