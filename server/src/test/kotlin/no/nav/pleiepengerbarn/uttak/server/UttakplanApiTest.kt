package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
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
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration
import kotlin.test.fail


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres")
@Tag("integration")
internal class UttakplanApiTest(@Autowired val restTemplate: TestRestTemplate) {

    private val testClient by lazy { PleiepengerBarnUttakTestClient(restTemplate) }

    @Test
    internal fun `Enkelt uttak på et arbeidsforhold`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")
        val grunnlag = lagGrunnlag(
                søknadsperiode = søknadsperiode,
                arbeid = listOf(
                        Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
                ),
                tilsynsbehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)),
        )

        val postResponse = testClient.opprettUttaksplan(grunnlag)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val uttaksplan = postResponse.body ?: fail("Mangler uttaksplan")

        uttaksplan.assertInnvilget(
                periode = LukketPeriode("2020-01-01/2020-01-08"),
                grad = HUNDREPROSENT,
                gradPerArbeidsforhold = mapOf(
                    ARBEIDSFORHOLD1 to HUNDREPROSENT
                ),
                innvilgetÅrsak = Årsak.FULL_DEKNING
        )
        uttaksplan.assertAvslått(
                periode = LukketPeriode("2020-01-09/2020-01-10"),
                avslåttÅrsaker = setOf(Årsak.UTENOM_TILSYNSBEHOV),
                knekkpunktTyper = setOf(KnekkpunktType.TILSYNSBEHOV)
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
                tilsynsbehov = mapOf(HELE_2020 to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)),
        )

        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertInnvilget(
                periode = LukketPeriode("2020-10-12/2020-10-16"),
                grad = Prosent(61),
                gradPerArbeidsforhold = mapOf(
                    ARBEIDSFORHOLD2 to Prosent(50),
                    ARBEIDSFORHOLD3 to Prosent(100)
                ),
                innvilgetÅrsak = Årsak.AVKORTET_MOT_INNTEKT
        )
    }

    @Test
    internal fun `Flere behandlinger med uttak etter hverandre`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-01/2020-01-10").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-11/2020-01-20").opprettUttaksplan()

        uttaksplan.assertInnvilget(periode = LukketPeriode("2020-01-01/2020-01-10"))
        uttaksplan.assertInnvilget(periode = LukketPeriode("2020-01-11/2020-01-20"))
    }

    @Test
    internal fun `Flere behandlinger med uttak med overlapp`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-01/2020-01-12").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-07/2020-01-20").opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(2)
        uttaksplan.assertInnvilget(periode = LukketPeriode("2020-01-01/2020-01-06"))
        uttaksplan.assertInnvilget(periode = LukketPeriode("2020-01-07/2020-01-20"))
    }

    @Test
    internal fun `Flere behandlinger med uttak med overlapp midt i perioden`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-01/2020-01-20").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-07/2020-01-12").opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(3)
        uttaksplan.assertInnvilget(periode = LukketPeriode("2020-01-01/2020-01-06"))
        uttaksplan.assertInnvilget(periode = LukketPeriode("2020-01-07/2020-01-12"))
        uttaksplan.assertInnvilget(periode = LukketPeriode("2020-01-13/2020-01-20"))
    }


    @Test
    internal fun `Flere behandlinger med uttak hvor siste behandling overskygger først behandling`() {
        val saksnummer = nesteSaksnummer()

        lagGrunnlag(saksnummer, "2020-01-07/2020-01-12").opprettUttaksplan()
        val uttaksplan = lagGrunnlag(saksnummer, "2020-01-01/2020-01-20").opprettUttaksplan()

        assertThat(uttaksplan.perioder).hasSize(1)
        uttaksplan.assertInnvilget(periode = LukketPeriode("2020-01-01/2020-01-20"))
    }

    private fun Uttaksgrunnlag.opprettUttaksplan(): Uttaksplan {
        val postResponse = testClient.opprettUttaksplan(this)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Thread.sleep(25) //Vent 25 ms for å sikre at uttaksplaner ikke havner på samme timestamp
        return postResponse.body ?: fail("Mangler uttaksplan")
    }

    private fun Uttaksplan.assertInnvilget(periode: LukketPeriode, grad: Prosent = HUNDREPROSENT, gradPerArbeidsforhold: Map<Arbeidsforhold, Prosent> = mapOf(ARBEIDSFORHOLD1 to HUNDREPROSENT), innvilgetÅrsak: Årsak = Årsak.FULL_DEKNING) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        when (periodeInfo.utfall) {
            Utfall.INNVILGET -> {
                assertThat(periodeInfo.årsaker).isEqualTo(setOf(innvilgetÅrsak))
                assertThat(periodeInfo.uttaksgrad).isEqualByComparingTo(grad)
                gradPerArbeidsforhold.forEach { (arbeidsforhold, prosent) ->
                    val utbetalingsgrad = periodeInfo.utbetalingsgrader.first { it.arbeidsforhold == arbeidsforhold } .utbetalingsgrad
                    assertThat(utbetalingsgrad).isEqualByComparingTo(prosent)
                }
            }
            else -> fail("Perioden $periode er ikke innvilget")
        }
    }

    private fun Uttaksplan.assertAvslått(periode: LukketPeriode, avslåttÅrsaker: Set<Årsak> = setOf(), knekkpunktTyper: Set<KnekkpunktType> = setOf()) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        when (periodeInfo.utfall) {
            Utfall.AVSLÅTT -> {
                assertThat(periodeInfo.årsaker).isEqualTo(avslåttÅrsaker)
                assertThat(periodeInfo.knekkpunktTyper).isEqualTo(knekkpunktTyper)
            }
            else -> fail("Perioden $periode er ikke avslått")
        }
    }

    private fun Duration.prosent(prosent: Long):Duration {
        return this.multipliedBy(prosent).dividedBy(100)
    }

}