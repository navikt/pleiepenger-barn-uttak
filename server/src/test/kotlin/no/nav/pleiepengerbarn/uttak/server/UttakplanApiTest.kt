package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.NULL_PROSENT
import no.nav.pleiepengerbarn.uttak.testklient.*
import no.nav.pleiepengerbarn.uttak.testklient.ARBEIDSFORHOLD1
import no.nav.pleiepengerbarn.uttak.testklient.FULL_DAG
import no.nav.pleiepengerbarn.uttak.testklient.INGENTING
import no.nav.pleiepengerbarn.uttak.testklient.PleiepengerBarnUttakTestClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import kotlin.test.fail

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@ActiveProfiles("postgres")
@Tag("integration")
class UttakplanApiTest(@Autowired val restTemplate: TestRestTemplate) {

    private val testClient by lazy { PleiepengerBarnUttakTestClient(restTemplate) }

    @Test
    internal fun `Enkelt uttak på et arbeidsforhold`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")
        val grunnlag = lagGrunnlag(
                søknadsperiode = søknadsperiode,
                arbeid = listOf(
                        Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
                ),
                pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
        )

        val postResponse = testClient.opprettUttaksplan(grunnlag)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val hentResponse = testClient.hentUttaksplan(grunnlag.behandlingUUID)
        assertThat(hentResponse.statusCode).isEqualTo(HttpStatus.OK)
        val uttaksplan = hentResponse.body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(
                perioder = listOf(LukketPeriode("2020-01-01/2020-01-03"), LukketPeriode("2020-01-06/2020-01-08")),
                grad = HUNDRE_PROSENT,
                gradPerArbeidsforhold = mapOf(
                    ARBEIDSFORHOLD1 to HUNDRE_PROSENT
                ),
                oppfyltÅrsak = Årsak.FULL_DEKNING,
                endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertIkkeOppfylt(
                periode = LukketPeriode("2020-01-09/2020-01-10"),
                ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
                knekkpunktTyper = setOf(KnekkpunktType.PLEIEBEHOV),
                endringsstatus = Endringsstatus.NY
        )
    }

    @Test
    internal fun `Enkelt uttak over lengre perioder kan slås sammen`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-03-31")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-03-31") to Pleiebehov.PROSENT_100),
        )

        val postResponse = testClient.opprettUttaksplan(grunnlag)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val hentResponse = testClient.hentUttaksplan(grunnlag.behandlingUUID, true)
        assertThat(hentResponse.statusCode).isEqualTo(HttpStatus.OK)
        val uttaksplan = hentResponse.body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(
            perioder = listOf(LukketPeriode("2020-01-01/2020-03-31")),
            grad = HUNDRE_PROSENT,
            gradPerArbeidsforhold = mapOf(
                ARBEIDSFORHOLD1 to HUNDRE_PROSENT
            ),
            oppfyltÅrsak = Årsak.FULL_DEKNING,
            endringsstatus = Endringsstatus.NY
        )
    }

    @Test
    internal fun `Enkelt uttak på flere arbeidsforhold`() {
        val grunnlag = lagGrunnlag(
                søknadsperiode = LukketPeriode("2020-10-12/2020-10-16"),
                arbeid = listOf(
                        Arbeid(ARBEIDSFORHOLD2, mapOf(HELE_2020 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG.prosent(70), jobberNå = FULL_DAG.prosent(70).prosent(50)))),
                        Arbeid(ARBEIDSFORHOLD3, mapOf(HELE_2020 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG.prosent(20), jobberNå = INGENTING))),
                ),
                pleiebehov = mapOf(HELE_2020 to Pleiebehov.PROSENT_100),
        )

        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertOppfylt(
                periode = LukketPeriode("2020-10-12/2020-10-16"),
                grad = Prosent(61),
                gradPerArbeidsforhold = mapOf(
                    ARBEIDSFORHOLD2 to Prosent(50),
                    ARBEIDSFORHOLD3 to Prosent(100)
                ),
                oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                endringsstatus = Endringsstatus.NY
        )
    }

    @Test
    internal fun `Avslag pga ferie`() {
        val søknadsperiode = LukketPeriode("2021-06-01/2021-06-04")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD2, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100)
        ).copy(lovbestemtFerie = listOf(LukketPeriode("2021-06-01/2021-06-01"), LukketPeriode("2021-06-03/2021-06-03")))

        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertIkkeOppfylt(periode = LukketPeriode("2021-06-01/2021-06-01"), ikkeOppfyltÅrsaker = setOf(Årsak.LOVBESTEMT_FERIE), endringsstatus = Endringsstatus.NY)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2021-06-02/2021-06-02"), grad = HUNDRE_PROSENT, gradPerArbeidsforhold = mapOf(ARBEIDSFORHOLD2 to HUNDRE_PROSENT), oppfyltÅrsak = Årsak.FULL_DEKNING, endringsstatus = Endringsstatus.NY)
        uttaksplan.assertIkkeOppfylt(periode = LukketPeriode("2021-06-03/2021-06-03"), ikkeOppfyltÅrsaker = setOf(Årsak.LOVBESTEMT_FERIE), knekkpunktTyper = setOf(KnekkpunktType.LOVBESTEMT_FERIE), endringsstatus = Endringsstatus.NY)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2021-06-04/2021-06-04"), grad = HUNDRE_PROSENT, gradPerArbeidsforhold = mapOf(ARBEIDSFORHOLD2 to HUNDRE_PROSENT), oppfyltÅrsak = Årsak.FULL_DEKNING, endringsstatus = Endringsstatus.NY)
    }

    @Test
    internal fun `Flere behandlinger med uttak etter hverandre`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-01/2020-01-10").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-11/2020-01-20").opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(4)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-06/2020-01-10"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-13/2020-01-17"), endringsstatus = Endringsstatus.NY)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-20/2020-01-20"), endringsstatus = Endringsstatus.NY)
    }

    @Test
    internal fun `Flere behandlinger med uttak med overlapp`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-01/2020-01-12").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-07/2020-01-20").opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(5)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-06/2020-01-06"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-07/2020-01-10"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-13/2020-01-17"), endringsstatus = Endringsstatus.NY)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-20/2020-01-20"), endringsstatus = Endringsstatus.NY)
    }

    @Test
    internal fun `Flere behandlinger med uttak med overlapp midt i perioden`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-01/2020-01-20").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-07/2020-01-12").opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(5)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-06/2020-01-06"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-07/2020-01-10"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-13/2020-01-17"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-20/2020-01-20"), endringsstatus = Endringsstatus.UENDRET)
    }


    @Test
    internal fun `Flere behandlinger med uttak hvor siste behandling overskygger først behandling`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-07/2020-01-12").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-01/2020-01-20").opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(5)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"), endringsstatus = Endringsstatus.NY)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-06/2020-01-06"), endringsstatus = Endringsstatus.NY)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-07/2020-01-10"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-13/2020-01-17"), endringsstatus = Endringsstatus.NY)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-20/2020-01-20"), endringsstatus = Endringsstatus.NY)
    }

    @Test
    internal fun `En del av uttaksplan blir ikke oppfylt pga ikke oppfylte inngangsvilkår`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-10") to Pleiebehov.PROSENT_100)
        ).copy(inngangsvilkår = mapOf("MEDLEMSKAPSVILKÅRET" to listOf(Vilkårsperiode(LukketPeriode("2020-01-05/2020-01-08"), Utfall.IKKE_OPPFYLT))))

        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"), endringsstatus = Endringsstatus.NY)
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT),
            endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-09/2020-01-10"), endringsstatus = Endringsstatus.NY)
    }


    @Test
    internal fun `Periode med avslått inngangsvilkår skal ikke være oppfylt`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-20")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-20") to Pleiebehov.PROSENT_100),
        ).copy(inngangsvilkår = mapOf(
            "FP_VK_3" to listOf(
                Vilkårsperiode(LukketPeriode("2020-01-01/2020-01-20"), Utfall.OPPFYLT)
            ),
            "FP_VK_2" to listOf(
                Vilkårsperiode(LukketPeriode("2020-01-01/2020-01-03"), Utfall.OPPFYLT),
                Vilkårsperiode(LukketPeriode("2020-01-04/2020-01-08"), Utfall.IKKE_OPPFYLT),
                Vilkårsperiode(LukketPeriode("2020-01-09/2020-01-20"), Utfall.OPPFYLT)
            ),
            "K9_VK_1" to listOf(
                Vilkårsperiode(LukketPeriode("2020-01-01/2020-01-20"), Utfall.OPPFYLT)
            )

        ))


        val postResponse = testClient.opprettUttaksplan(grunnlag)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val hentResponse = testClient.hentUttaksplan(grunnlag.behandlingUUID)
        assertThat(hentResponse.statusCode).isEqualTo(HttpStatus.OK)
        val uttaksplan = hentResponse.body ?: fail("Mangler uttaksplan")

        assertThat(uttaksplan.perioder).hasSize(5)

        uttaksplan.assertOppfylt(
            periode = LukketPeriode("2020-01-01/2020-01-03"),
            grad = HUNDRE_PROSENT,
            gradPerArbeidsforhold = mapOf(
                ARBEIDSFORHOLD1 to HUNDRE_PROSENT
            ),
            oppfyltÅrsak = Årsak.FULL_DEKNING,
            endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT),
            endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertOppfylt(
            perioder = listOf(LukketPeriode("2020-01-09/2020-01-10"), LukketPeriode("2020-01-13/2020-01-17"), LukketPeriode("2020-01-20/2020-01-20")),
            grad = HUNDRE_PROSENT,
            gradPerArbeidsforhold = mapOf(
                ARBEIDSFORHOLD1 to HUNDRE_PROSENT
            ),
            oppfyltÅrsak = Årsak.FULL_DEKNING,
            endringsstatus = Endringsstatus.NY
        )
    }


    @Test
    internal fun `Feil i input til uttaksplan skal gi bad request`() {
        val periode = LukketPeriode("2020-10-12/2020-10-16")
        val grunnlag = lagGrunnlag(
            søknadsperiode = periode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD2, mapOf(periode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = Duration.ZERO)))
            ),
            pleiebehov = mapOf(periode to Pleiebehov.PROSENT_100),
        ).copy(behandlingUUID = "tull og tøys")


        val postResponse = testClient.opprettUttaksplan(grunnlag)

        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    internal fun `Uttaksplan kan ikke hentes opp etter at den er slettet`() {
        val periode = LukketPeriode("2020-10-12/2020-10-16")
        val grunnlag = lagGrunnlag(
            søknadsperiode = periode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(periode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG.prosent(70), jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(periode to Pleiebehov.PROSENT_100),
        )

        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertOppfylt(
            periode = periode,
            grad = HUNDRE_PROSENT,
            gradPerArbeidsforhold = mapOf(
                ARBEIDSFORHOLD1 to HUNDRE_PROSENT
            ),
            oppfyltÅrsak = Årsak.FULL_DEKNING,
            endringsstatus = Endringsstatus.NY
        )

        testClient.slettUttaksplan(grunnlag.behandlingUUID)

        val uttaksplanResponse = testClient.hentUttaksplan(grunnlag.behandlingUUID)
        assertThat(uttaksplanResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    internal fun `En uttaksperiode med avkorting mot inntekt`() {
        val periode = LukketPeriode("2020-10-12/2020-10-16")
        val grunnlag = lagGrunnlag(
            søknadsperiode = periode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD2, mapOf(periode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), jobberNå = Duration.ofHours(1)))),
                Arbeid(ARBEIDSFORHOLD3, mapOf(periode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), jobberNå = Duration.ofHours(2)))),
            ),
            pleiebehov = mapOf(periode to Pleiebehov.PROSENT_100),
        )

        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertOppfylt(
            periode = periode,
            grad = Prosent(50),
            gradPerArbeidsforhold = mapOf(
                ARBEIDSFORHOLD2 to Prosent(67),
                ARBEIDSFORHOLD3 to Prosent(33)
            ),
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertSøkersTaptArbeid(periode, Prosent(50), Duration.ofHours(3))
    }

    @Test
    internal fun `2 søkere med etablert tilsyn hvor søker 2 skal få redusert uttaksgrad pga tilsyn`() {
        val periode = LukketPeriode("2020-10-12/2020-10-16")
        val grunnlagSøker1 = lagGrunnlag(
            søknadsperiode = periode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(periode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.dividedBy(2))))
            ),
            pleiebehov = mapOf(periode to Pleiebehov.PROSENT_100)
        ).copy(tilsynsperioder = mapOf(periode to FULL_DAG.prosent(40)), saksnummer = "1")

        grunnlagSøker1.opprettUttaksplan()


        val grunnlagSøker2 = lagGrunnlag(
            søknadsperiode = periode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(periode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(periode to Pleiebehov.PROSENT_200)
        ).copy(tilsynsperioder = mapOf(periode to FULL_DAG.prosent(40)), saksnummer = "2")

        val uttaksplanSøker2 = grunnlagSøker2.opprettUttaksplan()

        assertThat(uttaksplanSøker2.perioder.keys).hasSize(1)
        assertThat(uttaksplanSøker2.perioder.keys.first()).isEqualTo(periode)
        assertThat(uttaksplanSøker2.perioder.values.first().uttaksgrad).isEqualByComparingTo(Prosent(60))
    }

    @Test
    internal fun `2 søkere med forskjellig etablert tilsyn`() {
        val søknadsperiode = LukketPeriode("2020-10-12/2020-10-13")

        // Opprett uttaksplan 1 for søker 1
        val saksnummerSøker1 = nesteSaksnummer()
        val grunnlag1Søker1 = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100)
        ).copy(tilsynsperioder = mapOf(søknadsperiode to Duration.ofHours(4)), saksnummer = saksnummerSøker1)
        grunnlag1Søker1.opprettUttaksplan()

        // Opprett Uttaksplan 1 for søker 2
        val saksnummerSøker2 = nesteSaksnummer()
        val behandlingUUIDSøker2 = nesteBehandlingId()
        val grunnlagSøker2 = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100)
        ).copy(
            tilsynsperioder = mapOf(søknadsperiode to Duration.ofHours(2)),
            saksnummer = saksnummerSøker2,
            behandlingUUID = behandlingUUIDSøker2,
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(behandlingUUIDSøker2, grunnlag1Søker1.behandlingUUID))
        )
        val uttaksplan1Søker2 = grunnlagSøker2.opprettUttaksplan()

        assertThat(uttaksplan1Søker2.perioder.keys).hasSize(1)
        assertThat(uttaksplan1Søker2.perioder.keys.first()).isEqualTo(søknadsperiode)
        assertThat(uttaksplan1Søker2.perioder.values.first().uttaksgrad).isEqualByComparingTo(Prosent(73))
        assertThat(uttaksplan1Søker2.perioder.values.first().graderingMotTilsyn?.andreSøkeresTilsynReberegnet).isTrue
    }

    @Test
    internal fun `Endringsøknad som inkluderer at en periode er trukket`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-01/2020-01-20").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-15/2020-01-30").copy(
            trukketUttak = listOf(LukketPeriode("2020-01-06/2020-01-08")),
            tilsynsperioder = mapOf(LukketPeriode("2020-01-15/2020-01-30") to Duration.ofHours(4))
        ).opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(7)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-09/2020-01-10"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-13/2020-01-14"), endringsstatus = Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-15/2020-01-17"), endringsstatus = Endringsstatus.ENDRET,
            oppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN, grad = Prosent(47), gradPerArbeidsforhold = mapOf(ARBEIDSFORHOLD1 to Prosent(47)))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-20/2020-01-20"), endringsstatus = Endringsstatus.ENDRET,
            oppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN, grad = Prosent(47), gradPerArbeidsforhold = mapOf(ARBEIDSFORHOLD1 to Prosent(47)))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-21/2020-01-24"), endringsstatus = Endringsstatus.NY,
            oppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN, grad = Prosent(47), gradPerArbeidsforhold = mapOf(ARBEIDSFORHOLD1 to Prosent(47)))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-27/2020-01-30"), endringsstatus = Endringsstatus.NY,
            oppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN, grad = Prosent(47), gradPerArbeidsforhold = mapOf(ARBEIDSFORHOLD1 to Prosent(47)))
        assertThat(uttaksplan.trukketUttak).hasSize(1)
        assertThat(uttaksplan.trukketUttak[0].fom).isEqualTo(LocalDate.parse("2020-01-06"))
        assertThat(uttaksplan.trukketUttak[0].tom).isEqualTo(LocalDate.parse("2020-01-08"))
    }

    @Test
    internal fun `Søker med AT og SN og det er bare nok timer igjen til AT`() {
        val søknadsperiode = LukketPeriode("2021-01-04/2021-01-08")

        //Lager uttak for søker 1 som bruker opp 50%
        val grunnlagSøker1 = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD4, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(50))))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = "1001"
        )
        grunnlagSøker1.opprettUttaksplan()

        //Lager uttak for søker 2 som har både AT og SN. AT skal prioriteres foran SN.
        val grunnlagSøker2 = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(SELVSTENDIG1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofMinutes(12), jobberNå = INGENTING))),
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = "1002",
        ).copy(kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(grunnlagSøker1.behandlingUUID)))
        val uttaksplanSøker2 = grunnlagSøker2.opprettUttaksplan()

        uttaksplanSøker2.assertOppfylt(
            perioder = listOf(søknadsperiode),
            grad = Prosent(50),
            gradPerArbeidsforhold = mapOf(
                SELVSTENDIG1 to Prosent.ZERO,
                ARBEIDSFORHOLD1 to Prosent(51)
            ),
            oppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN,
            endringsstatus = Endringsstatus.NY
        )
    }

    @Test
    internal fun `Søker med AT og SN og det er nok timer igjen til begge`() {
        val søknadsperiode = LukketPeriode("2021-01-04/2021-01-08")

        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(SELVSTENDIG1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), jobberNå = INGENTING))),
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(8), jobberNå = Duration.ofHours(6))))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = nesteSaksnummer()
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertOppfylt(
            perioder = listOf(søknadsperiode),
            grad = Prosent(45),
            gradPerArbeidsforhold = mapOf(
                SELVSTENDIG1 to HUNDRE_PROSENT,
                ARBEIDSFORHOLD1 to Prosent(25)
            ),
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            endringsstatus = Endringsstatus.NY
        )
    }


    @Test
    internal fun `Søker med AT, FL og SN og kun AT skal få utbetalingsgrad`() {
        val søknadsperiode = LukketPeriode("2021-01-04/2021-01-04")

        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(SELVSTENDIG1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(2), jobberNå = Duration.ofHours(5)))),
                Arbeid(FRILANS1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(2), jobberNå = Duration.ofHours(2)))),
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(2), jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = nesteSaksnummer()
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertOppfylt(
            perioder = listOf(søknadsperiode),
            grad = Prosent(33),
            gradPerArbeidsforhold = mapOf(
                SELVSTENDIG1 to Prosent(0),
                FRILANS1 to Prosent(0),
                ARBEIDSFORHOLD1 to HUNDRE_PROSENT
            ),
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            endringsstatus = Endringsstatus.NY
        )
    }


    @Test
    internal fun `Endringssøknad som overlapper og forlengder`() {
        val etablertTilsyn = mapOf(
            LukketPeriode("2021-08-02/2021-08-05") to Duration.parse("PT3H"),
            LukketPeriode("2021-08-06/2021-08-06") to Duration.parse("PT5H"),
            LukketPeriode("2021-08-09/2021-08-12") to Duration.parse("PT3H"),
            LukketPeriode("2021-08-13/2021-08-13") to Duration.parse("PT5H"),
            LukketPeriode("2021-08-16/2021-08-19") to Duration.parse("PT3H"),
            LukketPeriode("2021-08-20/2021-08-20") to Duration.parse("PT5H"),
            LukketPeriode("2021-08-23/2021-08-26") to Duration.parse("PT3H"),
            LukketPeriode("2021-08-27/2021-08-27") to Duration.parse("PT5H"),
            LukketPeriode("2021-08-30/2021-08-31") to Duration.parse("PT3H")
        )

        val søknadsperiode1 = LukketPeriode("2021-08-02/2021-08-31")
        val søknadsperiode2 = LukketPeriode("2021-08-02/2021-09-03")
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(
            søknadsperiode = søknadsperiode1,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(8), jobberNå = Duration.ofHours(3).plusMinutes(36))))
            ),
            pleiebehov = mapOf(søknadsperiode1 to Pleiebehov.PROSENT_100),
            tilsynsperioder = etablertTilsyn,
            nattevåk = mapOf(LukketPeriode("2021-08-02/2021-08-31") to Utfall.OPPFYLT),
            saksnummer = saksnummer
        ).opprettUttaksplan()


        val uttaksplan = lagGrunnlag(
            søknadsperiode = søknadsperiode2,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(
                    LukketPeriode(søknadsperiode1.fom, søknadsperiode2.tom) to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(8), jobberNå = Duration.ofHours(3).plusMinutes(36)))
                )
            ),
            pleiebehov = mapOf(
                LukketPeriode("2021-08-02/2021-08-29") to Pleiebehov.PROSENT_100,
                LukketPeriode("2021-08-30/2021-09-03") to Pleiebehov.PROSENT_0
            ),
            tilsynsperioder = etablertTilsyn,
            nattevåk = mapOf(LukketPeriode("2021-08-02/2021-08-31") to Utfall.OPPFYLT),
            saksnummer = saksnummer
        ).opprettUttaksplan()


        assertThat(uttaksplan.perioder).hasSize(10)

        uttaksplan.assertOppfylt(
            perioder = listOf(
                LukketPeriode("2021-08-02/2021-08-05"),
                LukketPeriode("2021-08-06/2021-08-06"),
                LukketPeriode("2021-08-09/2021-08-12"),
                LukketPeriode("2021-08-13/2021-08-13"),
                LukketPeriode("2021-08-16/2021-08-19"),
                LukketPeriode("2021-08-20/2021-08-20"),
                LukketPeriode("2021-08-23/2021-08-26"),
                LukketPeriode("2021-08-27/2021-08-27")
            ),
            grad = Prosent(55),
            gradPerArbeidsforhold = mapOf(ARBEIDSFORHOLD1 to Prosent(55)),
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            endringsstatus = Endringsstatus.UENDRET
        )
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2021-08-30/2021-08-31"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(),
            endringsstatus = Endringsstatus.ENDRET
        )
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2021-09-01/2021-09-03"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(KnekkpunktType.FORRIGE_UTTAKPLAN, KnekkpunktType.TILSYNSPERIODE, KnekkpunktType.NATTEVÅKSPERIODE),
            endringsstatus = Endringsstatus.NY
        )
    }

    @Test
    internal fun `To parallelle behandling og så revurdering av den første med kravprioritet`() {
        val søknadsperiode = LukketPeriode("2021-09-20/2021-09-24")

        val arbeidSøker1 = Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(8), jobberNå = Duration.ofHours(2))))
        val grunnlag1Søker1 = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid =  listOf(arbeidSøker1),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            behandlingUUID = nesteBehandlingId(),
            saksnummer = nesteSaksnummer())


        grunnlag1Søker1.opprettUttaksplan()


        val arbeidSøker2 = Arbeid(ARBEIDSFORHOLD4, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(8), jobberNå = Duration.ofHours(4))))
        val grunnlagSøker2 = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid =  listOf(arbeidSøker2),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            behandlingUUID = nesteBehandlingId(),
            saksnummer = nesteSaksnummer())


        grunnlagSøker2.opprettUttaksplan()

        val grunnlag2Søker1BehandlingId = nesteBehandlingId()
        val grunnlag2Søker1 = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid =  listOf(arbeidSøker1),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            behandlingUUID = grunnlag2Søker1BehandlingId,
            saksnummer = grunnlag1Søker1.saksnummer
        ).copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(grunnlag2Søker1BehandlingId, grunnlagSøker2.behandlingUUID))
        )

        val uttakplan2søker1 = grunnlag2Søker1.opprettUttaksplan()

        uttakplan2søker1.assertOppfylt(søknadsperiode, Prosent(75), mapOf(ARBEIDSFORHOLD1 to Prosent(75)), Årsak.AVKORTET_MOT_INNTEKT, Endringsstatus.UENDRET)
    }


    @Test
    internal fun `Simulering av samme grunnlag skal gi at uttaksplanen ikke er endret`() {
        val grunnlag = lagGrunnlag(periode = "2021-09-20/2021-09-24")
        grunnlag.opprettUttaksplan()

        val simuleringsresultat = grunnlag.simulering()

        assertThat(simuleringsresultat.uttakplanEndret).isFalse
    }

    @Test
    internal fun `Simulering ved gjensidige krav`() {
        var grunnlagSøker1 = lagGrunnlag(periode = "2021-09-20/2021-09-24").copy(søker = Søker("søker1"))
        grunnlagSøker1 = grunnlagSøker1.copy(kravprioritetForBehandlinger = mapOf(
            LukketPeriode("2021-09-20/2021-09-24") to listOf(grunnlagSøker1.behandlingUUID)
        ))
        grunnlagSøker1.opprettUttaksplan()


        var grunnlagSøker2 = lagGrunnlag(periode = "2021-09-20/2021-09-24").copy(søker = Søker("søker2"))
        grunnlagSøker2 = grunnlagSøker2.copy(kravprioritetForBehandlinger = mapOf(
            LukketPeriode("2021-09-20/2021-09-24") to listOf(grunnlagSøker2.behandlingUUID)
        ))
        grunnlagSøker2.opprettUttaksplan()


        val grunnlagSøker1MedKravPrio = grunnlagSøker1.copy(kravprioritetForBehandlinger = mapOf(
            LukketPeriode("2021-09-20/2021-09-22") to listOf(grunnlagSøker1.behandlingUUID),
            LukketPeriode("2021-09-23/2021-09-24") to listOf(grunnlagSøker2.behandlingUUID, grunnlagSøker1.behandlingUUID)
        ))
        grunnlagSøker1MedKravPrio.opprettUttaksplan()

        val grunnlagSøker2MedKravPrio = grunnlagSøker2.copy(kravprioritetForBehandlinger = mapOf(
            LukketPeriode("2021-09-20/2021-09-22") to listOf(grunnlagSøker1.behandlingUUID, grunnlagSøker2.behandlingUUID),
            LukketPeriode("2021-09-23/2021-09-24") to listOf(grunnlagSøker2.behandlingUUID, grunnlagSøker1.behandlingUUID)
        ))
        val simuleringsresultat = grunnlagSøker2MedKravPrio.simulering()

        assertThat(simuleringsresultat.uttakplanEndret).isTrue
    }

    @Test
    internal fun `Simulering skal gi at uttaksplanen er endret når annen parts uttak er vedtatt`() {
        val grunnlagSøker1 = lagGrunnlag(periode = "2021-09-20/2021-09-24")
        grunnlagSøker1.opprettUttaksplan()

        val grunnlagSøker2 = lagGrunnlag(periode = "2021-09-20/2021-09-21").copy(
            arbeid = listOf(Arbeid(ARBEIDSFORHOLD4, mapOf(LukketPeriode("2021-09-20/2021-09-21") to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(50))))),
            søker = Søker("124")
        )
        grunnlagSøker2.opprettUttaksplan()

        val simuleringsresultat = grunnlagSøker1.copy(
            kravprioritetForBehandlinger = mapOf(
                LukketPeriode("2021-09-20/2021-09-21") to listOf(grunnlagSøker2.behandlingUUID, grunnlagSøker1.behandlingUUID)
            )
        ).simulering()
        assertThat(simuleringsresultat.uttakplanEndret).isTrue
    }

    @Test
    internal fun `Jobber nå er større enn normalt jobber skal føre til 0 utbetalingsgrad`() {
        val grunnlag = lagGrunnlag(periode = "2021-09-20/2021-09-24").copy(
            arbeid = listOf(Arbeid(ARBEIDSFORHOLD1, mapOf(LukketPeriode("2021-09-20/2021-09-24") to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(110)))))
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertIkkeOppfylt(LukketPeriode("2021-09-20/2021-09-24"), setOf(Årsak.FOR_LAV_TAPT_ARBEIDSTID), setOf(), Endringsstatus.NY)
        assertThat(uttaksplan.perioder).hasSize(1)
        val periode = uttaksplan.perioder[LukketPeriode("2021-09-20/2021-09-24")]!!
        assertThat(periode.uttaksgrad).isEqualByComparingTo(Prosent(0))
        assertThat(periode.utbetalingsgrader).hasSize(1)
        assertThat(periode.utbetalingsgrader[0].utbetalingsgrad).isEqualByComparingTo(Prosent(0))
    }


    @Test
    internal fun `Jobber nå er større enn normalt jobber hos en arbeidsgiver mens den jobber ikke hos den andre`() {
        val grunnlag = lagGrunnlag(periode = "2021-09-20/2021-09-24").copy(
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(LukketPeriode("2021-09-20/2021-09-24") to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4), jobberNå = Duration.ofHours(4)))),
                Arbeid(ARBEIDSFORHOLD4, mapOf(LukketPeriode("2021-09-20/2021-09-24") to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4), jobberNå = Duration.ZERO)))
            )
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertOppfylt(
            LukketPeriode("2021-09-20/2021-09-24"),
            Prosent(50),
            mapOf(
                ARBEIDSFORHOLD1 to Prosent(0),
                ARBEIDSFORHOLD4 to Prosent(100)
            ),
            Årsak.AVKORTET_MOT_INNTEKT,
            Endringsstatus.NY
        )
    }

    @Test
    internal fun `Skal slå sammen perioder selv om de har forskjellige knekkpunktTyper`() {
        var grunnlag = lagGrunnlag(periode = "2021-09-20/2021-09-24")
        grunnlag = grunnlag.copy(
            kravprioritetForBehandlinger = mapOf(LukketPeriode("2021-09-22/2021-09-24") to listOf(grunnlag.behandlingUUID))
        )

        val uttaksplan = grunnlag.opprettUttaksplan(slåSammenPerioder = true)

        assertThat(uttaksplan.perioder).hasSize(1)
        uttaksplan.assertOppfylt(
            LukketPeriode("2021-09-20/2021-09-24"),
            HUNDRE_PROSENT,
            mapOf(ARBEIDSFORHOLD1 to HUNDRE_PROSENT),
            Årsak.FULL_DEKNING,
            Endringsstatus.NY
        )
    }

    @Test
    internal fun `Avslag på inngangsvilkår og pleiebehov skal føre til at annen part kan overta andelen av pleiebehov fra motpart`() {
        val periode = "2021-09-20/2021-09-24"
        val barn = Barn("12345")
        val grunnlag1søker1 = lagGrunnlag(periode = periode).copy(
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(LukketPeriode(periode) to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = Duration.ofHours(3))))
            ),
            barn = barn
        )
        val uttakplan1søker1 = grunnlag1søker1.opprettUttaksplan()
        assertThat(uttakplan1søker1.perioder).hasSize(1)
        assertThat(uttakplan1søker1.perioder[LukketPeriode(periode)]!!.uttaksgrad).isEqualByComparingTo(Prosent(60))

        val grunnlag1søker2 = lagGrunnlag(periode = periode).copy(
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD4, mapOf(LukketPeriode(periode) to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            barn = barn,
            kravprioritetForBehandlinger = mapOf(LukketPeriode(periode) to listOf(grunnlag1søker1.behandlingUUID))
        )
        val uttaksplan1søker2 = grunnlag1søker2.opprettUttaksplan()
        assertThat(uttaksplan1søker2.perioder).hasSize(1)
        assertThat(uttaksplan1søker2.perioder[LukketPeriode(periode)]!!.uttaksgrad).isEqualByComparingTo(Prosent(40))


        val grunnlag2søker1 = grunnlag1søker1.copy(
            inngangsvilkår = mapOf("K9_VK_1" to listOf(Vilkårsperiode(LukketPeriode(periode), Utfall.IKKE_OPPFYLT))),
            pleiebehov = mapOf(LukketPeriode(periode) to Pleiebehov.PROSENT_0)

        )
        val uttaksplan2søker1 = grunnlag2søker1.opprettUttaksplan()
        assertThat(uttaksplan2søker1.perioder).hasSize(1)
        assertThat(uttaksplan2søker1.perioder[LukketPeriode(periode)]!!.uttaksgrad).isEqualByComparingTo(NULL_PROSENT)


        val grunnlag2søker2 = grunnlag1søker2.copy(
            kravprioritetForBehandlinger = mapOf(LukketPeriode(periode) to listOf(grunnlag1søker1.behandlingUUID, grunnlag1søker2.behandlingUUID))
        )
        val uttaksplan2søker2 = grunnlag2søker2.opprettUttaksplan()


        assertThat(uttaksplan2søker2.perioder).hasSize(1)
        assertThat(uttaksplan2søker2.perioder[LukketPeriode(periode)]!!.uttaksgrad).isEqualByComparingTo(HUNDRE_PROSENT)
    }

    @Test
    internal fun `Endre utfall og årsak på eksisterende uttaksplan`() {
        val søknadsperiode = LukketPeriode("2021-01-04/2021-01-08")

        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = nesteSaksnummer()
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertOppfylt(
            perioder = listOf(søknadsperiode),
            grad = HUNDRE_PROSENT,
            gradPerArbeidsforhold = mapOf(
                ARBEIDSFORHOLD1 to HUNDRE_PROSENT
            ),
            oppfyltÅrsak = Årsak.FULL_DEKNING,
            endringsstatus = Endringsstatus.NY
        )

        testClient.endreUttaksplan(EndrePerioderGrunnlag(grunnlag.saksnummer, grunnlag.behandlingUUID, mapOf(LukketPeriode("2021-01-04/2021-01-05") to Årsak.FOR_LAV_INNTEKT)))
        val endretUttaksplanResponse = testClient.hentUttaksplan(grunnlag.behandlingUUID)
        assertThat(endretUttaksplanResponse.statusCode).isEqualTo(HttpStatus.OK)
        val endretUttaksplan = endretUttaksplanResponse.body!!

        endretUttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2021-01-04/2021-01-05"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_INNTEKT),
            endringsstatus = Endringsstatus.NY
        )
        endretUttaksplan.assertOppfylt(
            perioder = listOf(LukketPeriode("2021-01-06/2021-01-08")),
            grad = HUNDRE_PROSENT,
            gradPerArbeidsforhold = mapOf(
                ARBEIDSFORHOLD1 to HUNDRE_PROSENT
            ),
            oppfyltÅrsak = Årsak.FULL_DEKNING,
            endringsstatus = Endringsstatus.NY
        )
    }

    @Test
    internal fun `Simulering av livets sluttfase med samme grunnlag skal gi at uttaksplanen ikke er endret og at det er brukt 5 dager av kvoten`() {
        val grunnlag = lagGrunnlag(ytelseType = YtelseType.PLS, periode = "2021-09-20/2021-09-24")
        grunnlag.opprettUttaksplan()

        val simuleringsresultat = grunnlag.simuleringSluttfase()

        assertThat(simuleringsresultat.uttakplanEndret).isFalse
        assertThat(simuleringsresultat.overstigerKvoten).isFalse
        assertThat(simuleringsresultat.antallBrukteDager).isEqualTo(BigDecimal.valueOf(5).setScale(2))
    }

    @Test
    internal fun `Enkelt uttak på grunnlaget til livets sluttfase med et arbeidsforhold`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")
        val grunnlag = lagGrunnlag(
                ytelseType = YtelseType.PLS,
                søknadsperiode = søknadsperiode,
                arbeid = listOf(
                        Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
                ),
                pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-10") to Pleiebehov.PROSENT_6000),
        )

        val postResponse = testClient.opprettUttaksplan(grunnlag)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val hentResponse = testClient.hentUttaksplan(grunnlag.behandlingUUID)
        assertThat(hentResponse.statusCode).isEqualTo(HttpStatus.OK)
        val uttaksplan = hentResponse.body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(
                perioder = listOf(LukketPeriode("2020-01-01/2020-01-03"), LukketPeriode("2020-01-06/2020-01-10")),
                grad = HUNDRE_PROSENT,
                gradPerArbeidsforhold = mapOf(
                        ARBEIDSFORHOLD1 to HUNDRE_PROSENT
                ),
                oppfyltÅrsak = Årsak.FULL_DEKNING,
                endringsstatus = Endringsstatus.NY
        )
    }

    @Test
    internal fun `En del av uttaksplanen til livets sluttfase blir ikke oppfylt pga ikke oppfylte inngangsvilkår`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")
        val grunnlag = lagGrunnlag(
                ytelseType = YtelseType.PLS,
                søknadsperiode = søknadsperiode,
                arbeid = listOf(
                        Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
                ),
                pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-10") to Pleiebehov.PROSENT_6000)
        ).copy(inngangsvilkår = mapOf("MEDLEMSKAPSVILKÅRET" to listOf(Vilkårsperiode(LukketPeriode("2020-01-05/2020-01-08"), Utfall.IKKE_OPPFYLT))))

        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"), endringsstatus = Endringsstatus.NY)
        uttaksplan.assertIkkeOppfylt(
                periode = LukketPeriode("2020-01-06/2020-01-08"),
                ikkeOppfyltÅrsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT),
                endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-09/2020-01-10"), endringsstatus = Endringsstatus.NY)
    }

    @Test
    internal fun `Tre parallelle behandlinger på livets sluttfase med 100 prosent krav skal alle bli innvilget med fullstendig dekning`() {
        val søknadsperiode = LukketPeriode("2021-09-20/2021-09-24")

        val arbeidSøker1 = Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(8), jobberNå = INGENTING)))
        val grunnlag1Søker1 = lagGrunnlag(
                ytelseType = YtelseType.PLS,
                søknadsperiode = søknadsperiode,
                arbeid =  listOf(arbeidSøker1),
                pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_6000),
                behandlingUUID = nesteBehandlingId(),
                saksnummer = nesteSaksnummer())


        grunnlag1Søker1.opprettUttaksplan()

        val søker2BehandlingId = nesteBehandlingId()
        val arbeidSøker2 = Arbeid(ARBEIDSFORHOLD4, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(8), jobberNå = INGENTING)))
        val grunnlagSøker2 = lagGrunnlag(
                ytelseType = YtelseType.PLS,
                søknadsperiode = søknadsperiode,
                arbeid =  listOf(arbeidSøker2),
                pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_6000),
                behandlingUUID = søker2BehandlingId,
                saksnummer = nesteSaksnummer()).copy(kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(søker2BehandlingId, grunnlag1Søker1.behandlingUUID)))

        grunnlagSøker2.opprettUttaksplan()

        val søker3BehandlingId = nesteBehandlingId()

        val arbeidSøker3 = Arbeid(ARBEIDSFORHOLD4, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(8), jobberNå = INGENTING)))
        val grunnlagSøker3 = lagGrunnlag(
                ytelseType = YtelseType.PLS,
                søknadsperiode = søknadsperiode,
                arbeid =  listOf(arbeidSøker3),
                pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_6000),
                behandlingUUID = søker3BehandlingId,
                saksnummer = nesteSaksnummer()).copy(kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(søker3BehandlingId, grunnlagSøker2.behandlingUUID, grunnlag1Søker1.behandlingUUID)))


        val uttakplanSøker3 = grunnlagSøker3.opprettUttaksplan()

        uttakplanSøker3.assertOppfylt(søknadsperiode, Prosent(100), mapOf(ARBEIDSFORHOLD4 to Prosent(100)), Årsak.FULL_DEKNING, Endringsstatus.NY)
    }

    @Test
    internal fun `Perioder med utenlandsopphold skal godkjennes dersom der er under 80 dager per 12 måneder`() {
        val søknadsperiode = LukketPeriode("2021-01-04/2021-01-08")

        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
        ).copy(
            utenlandsoppholdperioder = mapOf(LukketPeriode("2021-01-05/2021-01-07") to UtenlandsoppholdInfo(UtenlandsoppholdÅrsak.INGEN, "USA"))
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertOppfylt(periode = LukketPeriode("2021-01-04/2021-01-04"), utenlandsoppholdUtenÅrsak = false)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2021-01-05/2021-01-07"), utenlandsoppholdUtenÅrsak = true)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2021-01-08/2021-01-08"), utenlandsoppholdUtenÅrsak = false)
    }

    @Test
    internal fun `Perioder med utenlandsopphold med gyldig grunn skal ikke telles`() {
        val saksnummer = opprettUttakUtenlandsopphold(LukketPeriode("2021-01-04/2021-02-28"))

        val søknadsperiode = LukketPeriode("2021-12-13/2021-12-17")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = saksnummer
        ).copy(
            utenlandsoppholdperioder = mapOf(søknadsperiode to UtenlandsoppholdInfo(UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING, "USA"))
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(9)
        uttaksplan.assertOppfylt(søknadsperiode)
    }

    @Test
    internal fun `Perioder med utenlandsopphold uten årsak innenfor EØS som overstiger 8 ukers grensen skal innfris`() {
        val saksnummer = opprettUttakUtenlandsopphold(LukketPeriode("2021-01-04/2021-02-28"))

        val søknadsperiode = LukketPeriode("2021-12-13/2021-12-17")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = saksnummer
        ).copy(
            utenlandsoppholdperioder = mapOf(søknadsperiode to UtenlandsoppholdInfo(UtenlandsoppholdÅrsak.INGEN, "DNK"))
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(9)
        uttaksplan.assertOppfylt(periode = søknadsperiode, endringsstatus = Endringsstatus.NY)
    }

    @Test
    internal fun `Perioder med utenlandsopphold uten årsak som overstiger 8 ukers grensen skal avslås`() {
        val saksnummer = opprettUttakUtenlandsopphold(LukketPeriode("2021-01-04/2021-02-28"))

        val søknadsperiode = LukketPeriode("2021-12-13/2021-12-17")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = saksnummer
        ).copy(
            utenlandsoppholdperioder = mapOf(søknadsperiode to UtenlandsoppholdInfo(UtenlandsoppholdÅrsak.INGEN, "USA"))
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(9)
        uttaksplan.assertIkkeOppfylt(periode = søknadsperiode, ikkeOppfyltÅrsaker = setOf(Årsak.FOR_MANGE_DAGER_UTENLANDSOPPHOLD), endringsstatus = Endringsstatus.NY)
    }

    @Test
    internal fun `Perioder med utenlandsopphold uten årsak som overstiger 8 ukers grensen men hvor utenlandsoppholdet endres i ny behandling`() {
        val saksnummer = opprettUttakUtenlandsopphold(LukketPeriode("2021-01-04/2021-03-07"))

        val søknadsperiode = LukketPeriode("2021-02-22/2021-03-07")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = saksnummer
        ).copy(
            utenlandsoppholdperioder = mapOf(
                LukketPeriode("2021-02-22/2021-02-28") to UtenlandsoppholdInfo(UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING, "USA"),
                LukketPeriode("2021-03-01/2021-03-07") to UtenlandsoppholdInfo(UtenlandsoppholdÅrsak.INGEN, "USA")
            )
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(9)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2021-02-22/2021-02-26"), utenlandsoppholdUtenÅrsak = false, endringsstatus = Endringsstatus.ENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2021-03-01/2021-03-05"), utenlandsoppholdUtenÅrsak = true, endringsstatus = Endringsstatus.ENDRET)
    }

    @Test
    internal fun `Perioder med utenlandsopphold uten årsak som overstiger 8 ukers grensen men hvor en del av uttak avslås pga arbeid`() {
        val saksnummer = opprettUttakUtenlandsopphold(LukketPeriode("2021-01-04/2021-03-07"))

        val søknadsperiode = LukketPeriode("2021-02-22/2021-03-07")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(
                    LukketPeriode("2021-02-22/2021-02-28") to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG),
                    LukketPeriode("2021-03-01/2021-03-07") to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
                ))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = saksnummer
        ).copy(
            utenlandsoppholdperioder = mapOf(
                søknadsperiode to UtenlandsoppholdInfo(UtenlandsoppholdÅrsak.INGEN, "USA")
            )
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(9)
        uttaksplan.assertIkkeOppfylt(periode = LukketPeriode("2021-02-22/2021-02-26"), ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_TAPT_ARBEIDSTID),  endringsstatus = Endringsstatus.ENDRET)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2021-03-01/2021-03-05"), utenlandsoppholdUtenÅrsak = true, endringsstatus = Endringsstatus.ENDRET)
    }

    @Test
    internal fun `Perioder med utenlandsopphold uten årsak som overstiger 8 ukers grensen men over mer enn 12 måneder skal ikke avslås`() {
        val saksnummer = opprettUttakUtenlandsopphold(LukketPeriode("2021-01-04/2021-02-28"))

        val søknadsperiode = LukketPeriode("2022-03-07/2022-03-11")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = saksnummer
        ).copy(
            utenlandsoppholdperioder = mapOf(søknadsperiode to UtenlandsoppholdInfo(UtenlandsoppholdÅrsak.INGEN, "USA"))
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(9)
        uttaksplan.assertOppfylt(periode = søknadsperiode, utenlandsoppholdUtenÅrsak = true)
    }

    @Test
    internal fun `Perioder skal opprettes med korrekte default verdier for landkode og utenlandsoppholdÅrsak`() {
        val saksnummer = opprettUttakUtenlandsopphold(LukketPeriode("2021-01-04/2021-02-28"))

        val søknadsperiode = LukketPeriode("2021-12-13/2021-12-17")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
            saksnummer = saksnummer
        )
        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertOppfylt(søknadsperiode, landkode = null, utenlandsoppholdÅrsak = UtenlandsoppholdÅrsak.INGEN)
    }

    @Test
    internal fun `Simulering skal gi samme resultat med samme grunnlag som forrige uttaksplan`() {

        val søknadsperiode = LukketPeriode("2021-10-01/2021-12-31")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100)
        ).copy(
            utenlandsoppholdperioder = mapOf(søknadsperiode to UtenlandsoppholdInfo(UtenlandsoppholdÅrsak.INGEN, "USA"))
        )
        grunnlag.opprettUttaksplan()

        val resultatSimulering = testClient.simulerUttaksplan(grunnlag).body ?: throw IllegalStateException("Skal ikke være null")

        assertThat(resultatSimulering.uttakplanEndret).isFalse
    }

    @Test
    internal fun `Ikke yrkesaktiv i kombinasjon med arbeidsforhold uten normal arbeidstid skal gi utbetaling for ikke yrkesaktiv`() {
        val saksnummer = nesteSaksnummer()

        val uttaksplan = lagGrunnlag(saksnummer, "2022-01-01/2022-01-31")
            .copy(
                arbeid = listOf(
                    Arbeid(Arbeidsforhold(type = "AT", organisasjonsnummer = "987654321"), mapOf(LukketPeriode("2022-01-01/2022-01-31") to ArbeidsforholdPeriodeInfo(jobberNormalt = INGENTING, jobberNå = INGENTING))),
                    Arbeid(Arbeidsforhold(type = "AT", organisasjonsnummer = "123456789"), mapOf(LukketPeriode("2022-01-01/2022-01-10") to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))),
                    Arbeid(Arbeidsforhold(type = "IKKE_YRKESAKTIV", organisasjonsnummer = "123456789"), mapOf(LukketPeriode("2022-01-11/2022-01-31") to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))),
                )
            )
            .opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(6)
        val hundreProsentTilVanligArbeidsforhold = mapOf(Arbeidsforhold(type = "AT", organisasjonsnummer = "123456789") to HUNDRE_PROSENT)
        val hundreProsentTilIkkeYrkesaktiv = mapOf(Arbeidsforhold(type = "IKKE_YRKESAKTIV", organisasjonsnummer = "123456789") to HUNDRE_PROSENT)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2022-01-03/2022-01-07"), gradPerArbeidsforhold = hundreProsentTilVanligArbeidsforhold)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2022-01-10/2022-01-10"), gradPerArbeidsforhold = hundreProsentTilVanligArbeidsforhold)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2022-01-11/2022-01-14"), gradPerArbeidsforhold = hundreProsentTilIkkeYrkesaktiv)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2022-01-17/2022-01-21"), gradPerArbeidsforhold = hundreProsentTilIkkeYrkesaktiv)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2022-01-24/2022-01-28"), gradPerArbeidsforhold = hundreProsentTilIkkeYrkesaktiv)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2022-01-31/2022-01-31"), gradPerArbeidsforhold = hundreProsentTilIkkeYrkesaktiv)
    }

    private fun opprettUttakUtenlandsopphold(søknadsperiode: LukketPeriode, utenlandsoppholdÅrsak: UtenlandsoppholdÅrsak = UtenlandsoppholdÅrsak.INGEN): Saksnummer {
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
        ).copy(
            utenlandsoppholdperioder = mapOf(søknadsperiode to UtenlandsoppholdInfo(UtenlandsoppholdÅrsak.INGEN, "USA"))
        )
        grunnlag.opprettUttaksplan()

        return grunnlag.saksnummer
    }

    private fun Uttaksgrunnlag.opprettUttaksplan(slåSammenPerioder: Boolean = false): Uttaksplan {
        val postResponse = testClient.opprettUttaksplan(this)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val hentResponse = testClient.hentUttaksplan(this.behandlingUUID, slåSammenPerioder)
        assertThat(hentResponse.statusCode).isEqualTo(HttpStatus.OK)
        Thread.sleep(25) //Vent 25 ms for å sikre at uttaksplaner ikke havner på samme timestamp
        return hentResponse.body ?: fail("Mangler uttaksplan")
    }

    private fun Uttaksgrunnlag.simulering(): Simulering {
        val postResponse = testClient.simulerUttaksplan(this)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.OK)
        return postResponse.body ?: fail("Mangler simulering")
    }

    private fun Uttaksgrunnlag.simuleringSluttfase(): SimuleringLivetsSluttfase {
        val postResponse = testClient.simulerUttaksplanLivetsSluttfase(this)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.OK)
        return postResponse.body ?: fail("Mangler simulering")
    }

    private fun Uttaksplan.assertOppfylt(perioder: List<LukketPeriode>, grad: Prosent = HUNDRE_PROSENT, gradPerArbeidsforhold: Map<Arbeidsforhold, Prosent> = mapOf(ARBEIDSFORHOLD1 to HUNDRE_PROSENT), oppfyltÅrsak: Årsak = Årsak.FULL_DEKNING, endringsstatus: Endringsstatus) {
        perioder.forEach { assertOppfylt(it, grad, gradPerArbeidsforhold, oppfyltÅrsak, endringsstatus) }
    }

    private fun Uttaksplan.assertOppfylt(
        periode: LukketPeriode,
        grad: Prosent = HUNDRE_PROSENT,
        gradPerArbeidsforhold: Map<Arbeidsforhold, Prosent> = mapOf(ARBEIDSFORHOLD1 to HUNDRE_PROSENT),
        oppfyltÅrsak: Årsak = Årsak.FULL_DEKNING,
        endringsstatus: Endringsstatus = Endringsstatus.NY,
        utenlandsoppholdUtenÅrsak: Boolean = false
    ) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        when (periodeInfo.utfall) {
            Utfall.OPPFYLT -> {
                assertThat(periodeInfo.årsaker).isEqualTo(setOf(oppfyltÅrsak))
                assertThat(periodeInfo.uttaksgrad).isEqualByComparingTo(grad)
                assertThat(periodeInfo.endringsstatus).isEqualTo(endringsstatus)
                gradPerArbeidsforhold.forEach { (arbeidsforhold, prosent) ->
                    val utbetalingsgrad = periodeInfo.utbetalingsgrader.first { it.arbeidsforhold == arbeidsforhold } .utbetalingsgrad
                    assertThat(utbetalingsgrad).isEqualByComparingTo(prosent)
                }
                assertThat(periodeInfo.utenlandsoppholdUtenÅrsak).isEqualTo(utenlandsoppholdUtenÅrsak)
            }
            else -> fail("Perioden $periode er ikke oppfylt")
        }
    }

    /*
     * Lagt til en testmetode som tester informasjon om utenlandsperioder.
     * Valgte å lage en metode med ekstra parametre så slipper vi å skrive om ekisterende tester.
     */
    private fun Uttaksplan.assertOppfylt(
        periode: LukketPeriode,
        grad: Prosent = HUNDRE_PROSENT,
        gradPerArbeidsforhold: Map<Arbeidsforhold, Prosent> = mapOf(ARBEIDSFORHOLD1 to HUNDRE_PROSENT),
        oppfyltÅrsak: Årsak = Årsak.FULL_DEKNING,
        endringsstatus: Endringsstatus = Endringsstatus.NY,
        utenlandsoppholdUtenÅrsak: Boolean = false,
        landkode: String? = null,
        utenlandsoppholdÅrsak: UtenlandsoppholdÅrsak = UtenlandsoppholdÅrsak.INGEN
    ) {
        assertOppfylt(
            periode, grad, gradPerArbeidsforhold, oppfyltÅrsak,
            endringsstatus, utenlandsoppholdUtenÅrsak
        )
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        when (periodeInfo.utfall) {
            Utfall.OPPFYLT -> {
                assertThat(periodeInfo.landkode).isEqualTo(landkode)
                assertThat(periodeInfo.utenlandsoppholdÅrsak).isEqualTo(utenlandsoppholdÅrsak)
            }
        }
    }

    private fun Uttaksplan.assertSøkersTaptArbeid(periode: LukketPeriode, søkersTapteArbeidstid: Prosent, søkersTapteTimer: Duration) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        assertThat(periodeInfo.søkersTapteArbeidstid).isEqualByComparingTo(søkersTapteArbeidstid)
        assertThat(periodeInfo.getSøkersTapteTimer()).isEqualTo(søkersTapteTimer)
    }

    private fun Uttaksplan.assertIkkeOppfylt(periode: LukketPeriode, ikkeOppfyltÅrsaker: Set<Årsak> = setOf(), knekkpunktTyper: Set<KnekkpunktType> = setOf(), endringsstatus: Endringsstatus) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        when (periodeInfo.utfall) {
            Utfall.IKKE_OPPFYLT -> {
                assertThat(periodeInfo.årsaker).isEqualTo(ikkeOppfyltÅrsaker)
                assertThat(periodeInfo.knekkpunktTyper).isEqualTo(knekkpunktTyper)
                assertThat(periodeInfo.endringsstatus).isEqualTo(endringsstatus)
                assertThat(periodeInfo.uttaksgrad).isEqualByComparingTo(Prosent(0))
                periodeInfo.utbetalingsgrader.forEach {
                    assertThat(it.utbetalingsgrad).isEqualByComparingTo(Prosent(0))
                }
            }
            else -> fail("Perioden $periode er oppfylt")
        }
    }

    private fun Duration.prosent(prosent: Long):Duration {
        return this.multipliedBy(prosent).dividedBy(100)
    }

}
