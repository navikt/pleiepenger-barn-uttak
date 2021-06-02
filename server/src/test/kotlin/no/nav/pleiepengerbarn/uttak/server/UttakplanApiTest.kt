package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
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
import java.time.Duration
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
                oppfyltÅrsak = Årsak.FULL_DEKNING
        )
        uttaksplan.assertIkkeOppfylt(
                periode = LukketPeriode("2020-01-09/2020-01-10"),
                ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
                knekkpunktTyper = setOf(KnekkpunktType.PLEIEBEHOV)
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
                oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT
        )
    }

    @Test
    internal fun `Flere behandlinger med uttak etter hverandre`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-01/2020-01-10").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-11/2020-01-20").opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(4)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-06/2020-01-10"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-13/2020-01-17"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-20/2020-01-20"))
    }

    @Test
    internal fun `Flere behandlinger med uttak med overlapp`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-01/2020-01-12").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-07/2020-01-20").opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(5)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-06/2020-01-06"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-07/2020-01-10"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-13/2020-01-17"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-20/2020-01-20"))
    }

    @Test
    internal fun `Flere behandlinger med uttak med overlapp midt i perioden`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-01/2020-01-20").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-07/2020-01-12").opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(5)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-06/2020-01-06"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-07/2020-01-10"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-13/2020-01-17"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-20/2020-01-20"))
    }


    @Test
    internal fun `Flere behandlinger med uttak hvor siste behandling overskygger først behandling`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-07/2020-01-12").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-01/2020-01-20").opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(4)
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-06/2020-01-10"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-13/2020-01-17"))
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-20/2020-01-20"))
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

        val postResponse = testClient.opprettUttaksplan(grunnlag)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val uttaksplan = postResponse.body ?: fail("Mangler uttaksplan")
        assertThat(uttaksplan.perioder).hasSize(3)

        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-01/2020-01-03"))
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT)
        )
        uttaksplan.assertOppfylt(periode = LukketPeriode("2020-01-09/2020-01-10"))
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
            oppfyltÅrsak = Årsak.FULL_DEKNING
        )
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT)
        )
        uttaksplan.assertOppfylt(
            perioder = listOf(LukketPeriode("2020-01-09/2020-01-10"), LukketPeriode("2020-01-13/2020-01-17"), LukketPeriode("2020-01-20/2020-01-20")),
            grad = HUNDRE_PROSENT,
            gradPerArbeidsforhold = mapOf(
                ARBEIDSFORHOLD1 to HUNDRE_PROSENT
            ),
            oppfyltÅrsak = Årsak.FULL_DEKNING
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
            oppfyltÅrsak = Årsak.FULL_DEKNING
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
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT
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
        ).copy(tilsynsperioder = mapOf(periode to FULL_DAG.prosent(40)), saksnummer = "2", andrePartersSaksnummer = listOf("1"))

        val uttaksplanSøker2 = grunnlagSøker2.opprettUttaksplan()

        assertThat(uttaksplanSøker2.perioder.keys).hasSize(1)
        assertThat(uttaksplanSøker2.perioder.keys.first()).isEqualTo(periode)
        assertThat(uttaksplanSøker2.perioder.values.first().uttaksgrad).isEqualTo(Prosent(60))
    }

    private fun Uttaksgrunnlag.opprettUttaksplan(): Uttaksplan {
        val postResponse = testClient.opprettUttaksplan(this)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Thread.sleep(25) //Vent 25 ms for å sikre at uttaksplaner ikke havner på samme timestamp
        return postResponse.body ?: fail("Mangler uttaksplan")
    }

    private fun Uttaksplan.assertOppfylt(perioder: List<LukketPeriode>, grad: Prosent = HUNDRE_PROSENT, gradPerArbeidsforhold: Map<Arbeidsforhold, Prosent> = mapOf(ARBEIDSFORHOLD1 to HUNDRE_PROSENT), oppfyltÅrsak: Årsak = Årsak.FULL_DEKNING) {
        perioder.forEach { assertOppfylt(it, grad, gradPerArbeidsforhold, oppfyltÅrsak) }
    }

    private fun Uttaksplan.assertOppfylt(periode: LukketPeriode, grad: Prosent = HUNDRE_PROSENT, gradPerArbeidsforhold: Map<Arbeidsforhold, Prosent> = mapOf(ARBEIDSFORHOLD1 to HUNDRE_PROSENT), oppfyltÅrsak: Årsak = Årsak.FULL_DEKNING) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        when (periodeInfo.utfall) {
            Utfall.OPPFYLT -> {
                assertThat(periodeInfo.årsaker).isEqualTo(setOf(oppfyltÅrsak))
                assertThat(periodeInfo.uttaksgrad).isEqualByComparingTo(grad)
                gradPerArbeidsforhold.forEach { (arbeidsforhold, prosent) ->
                    val utbetalingsgrad = periodeInfo.utbetalingsgrader.first { it.arbeidsforhold == arbeidsforhold } .utbetalingsgrad
                    assertThat(utbetalingsgrad).isEqualByComparingTo(prosent)
                }
            }
            else -> fail("Perioden $periode er ikke oppfylt")
        }
    }

    private fun Uttaksplan.assertSøkersTaptArbeid(periode: LukketPeriode, søkersTapteArbeidstid: Prosent, søkersTapteTimer: Duration) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        assertThat(periodeInfo.søkersTapteArbeidstid).isEqualByComparingTo(søkersTapteArbeidstid)
        assertThat(periodeInfo.getSøkersTapteTimer()).isEqualTo(søkersTapteTimer)
    }

    private fun Uttaksplan.assertIkkeOppfylt(periode: LukketPeriode, ikkeOppfyltÅrsaker: Set<Årsak> = setOf(), knekkpunktTyper: Set<KnekkpunktType> = setOf()) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        when (periodeInfo.utfall) {
            Utfall.IKKE_OPPFYLT -> {
                assertThat(periodeInfo.årsaker).isEqualTo(ikkeOppfyltÅrsaker)
                assertThat(periodeInfo.knekkpunktTyper).isEqualTo(knekkpunktTyper)
            }
            else -> fail("Perioden $periode er oppfylt")
        }
    }

    private fun Duration.prosent(prosent: Long):Duration {
        return this.multipliedBy(prosent).dividedBy(100)
    }

}
