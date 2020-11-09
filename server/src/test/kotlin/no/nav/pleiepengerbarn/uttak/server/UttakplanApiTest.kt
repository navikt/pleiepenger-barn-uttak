package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
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
import java.time.LocalDate
import java.util.*
import kotlin.test.fail


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres")
@Tag("integration")
internal class UttakplanApiTest(@Autowired val restTemplate: TestRestTemplate) {

    private val testClient = PleiepengerBarnUttakTestClient(restTemplate)

    private val FULL_DAG: Duration = Duration.ofHours(7).plusMinutes(30)
    private val FULL_UKE: Duration = FULL_DAG.multipliedBy(5)

    private val HUNDREPROSENT = Prosent(100)

    private val HELE_2020 = LukketPeriode("2020-01-01/2020-12-31")

    private val ARBEIDSFORHOLD1 = ArbeidsforholdReferanse(type="arbeidsgiver", organisasjonsnummer = "123456789")
    private val ARBEIDSFORHOLD2 = ArbeidsforholdReferanse(type="arbeidsgiver", organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID().toString())
    private val ARBEIDSFORHOLD3 = ArbeidsforholdReferanse(type="arbeidsgiver", organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID().toString())
    private val ARBEIDSFORHOLD4 = ArbeidsforholdReferanse(type="arbeidsgiver", organisasjonsnummer = "987654321", arbeidsforholdId = UUID.randomUUID().toString())
    private val ARBEIDSFORHOLD5 = ArbeidsforholdReferanse(type="arbeidsgiver", organisasjonsnummer = "987654321", arbeidsforholdId = UUID.randomUUID().toString())

    @Test
    internal fun `Enkelt uttak på et arbeidsforhold`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")
        val grunnlag = lagGrunnlag(
                søknadsperioder = listOf(søknadsperiode),
                arbeid = listOf(
                        Arbeidsforhold(ARBEIDSFORHOLD1, mapOf(HELE_2020 to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent(0))))
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
                innvilgetÅrsak = InnvilgetÅrsaker.FULL_DEKNING
        )
        uttaksplan.assertAvslått(
                periode = LukketPeriode("2020-01-09/2020-01-10"),
                avslåttÅrsaker = setOf(AvslåttÅrsaker.UTENOM_TILSYNSBEHOV),
                knekkpunktTyper = setOf(KnekkpunktType.TILSYNSBEHOV)
        )
    }


    @Test
    internal fun `Enkelt uttak på flere arbeidsforhold`() {
        val søknadsperiode = LukketPeriode("2020-10-12/2020-10-16")
        val grunnlag = lagGrunnlag(
                søknadsperioder = listOf(søknadsperiode),
                arbeid = listOf(
                        Arbeidsforhold(ARBEIDSFORHOLD2, mapOf(HELE_2020 to ArbeidsforholdPeriodeInfo(jobberNormaltPerUke = FULL_UKE.prosent(70), skalJobbeProsent = Prosent(50)))),
                        Arbeidsforhold(ARBEIDSFORHOLD3, mapOf(HELE_2020 to ArbeidsforholdPeriodeInfo(jobberNormaltPerUke = FULL_UKE.prosent(20), skalJobbeProsent = Prosent(0)))),
                ),
                tilsynsbehov = mapOf(HELE_2020 to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)),
        )

        val uttaksplan = grunnlag.opprettUttaksplan()

        uttaksplan.assertInnvilget(
                periode = LukketPeriode("2020-10-12/2020-10-16"),
                grad = Prosent(55),
                gradPerArbeidsforhold = mapOf(
                    ARBEIDSFORHOLD2 to Prosent(50),
                    ARBEIDSFORHOLD3 to Prosent(100)
                ),
                innvilgetÅrsak = InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT
        )
    }

    private fun Uttaksgrunnlag.opprettUttaksplan(): Uttaksplan {
        val postResponse = testClient.opprettUttaksplan(this)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        return postResponse.body ?: fail("Mangler uttaksplan")
    }

    private fun Uttaksplan.assertInnvilget(periode: LukketPeriode, grad: Prosent = HUNDREPROSENT, gradPerArbeidsforhold: Map<ArbeidsforholdReferanse, Prosent> = mapOf(), innvilgetÅrsak: InnvilgetÅrsaker, knekkpunktTyper: Set<KnekkpunktType> = setOf()) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        when (periodeInfo) {
            is InnvilgetPeriode -> {
                assertThat(periodeInfo.årsak).isEqualTo(innvilgetÅrsak)
                assertThat(periodeInfo.grad).isEqualByComparingTo(grad)
                gradPerArbeidsforhold.forEach { (arbeidsforhold, prosent) ->
                    val utbetalingsgrad = periodeInfo.utbetalingsgrader.first { it.arbeidsforhold == arbeidsforhold } .utbetalingsgrad
                    assertThat(utbetalingsgrad).isEqualByComparingTo(prosent)
                }
            }
            else -> fail("Perioden $periode er ikke innvilget")
        }
    }

    private fun Uttaksplan.assertAvslått(periode: LukketPeriode, avslåttÅrsaker: Set<AvslåttÅrsaker> = setOf(), knekkpunktTyper: Set<KnekkpunktType> = setOf()) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        when (periodeInfo) {
            is AvslåttPeriode -> {
                val årsaker = periodeInfo.årsaker.map { it.årsak } .toSet()
                assertThat(årsaker).isEqualTo(avslåttÅrsaker)
                assertThat(periodeInfo.knekkpunktTyper()).isEqualTo(knekkpunktTyper)
            }
            else -> fail("Perioden $periode er ikke avslått")
        }
    }

    private fun Duration.prosent(prosent: Long):Duration {
        return this.multipliedBy(prosent).dividedBy(100)
    }

    private fun lagGrunnlag(
            søknadsperioder: List<LukketPeriode>,
            arbeid: Arbeid,
            tilsynsbehov: Map<LukketPeriode, Tilsynsbehov>,
            tilsynsperioder: Map<LukketPeriode, TilsynPeriodeInfo> = mapOf(),
            søker: Søker = Søker(LocalDate.parse("2000-01-01")),
            saksnummer: Saksnummer = nesteSaksnummer(),
            behandlingId: BehandlingId = nesteBehandlingId()
    ): Uttaksgrunnlag {
        return Uttaksgrunnlag(
                søker = søker,
                saksnummer = saksnummer,
                behandlingId = behandlingId,
                søknadsperioder = søknadsperioder,
                arbeid = arbeid,
                tilsynsbehov = tilsynsbehov,
                tilsynsperioder = tilsynsperioder,
                medlemskap = mapOf(HELE_2020 to Medlemskap()) //TODO: endret når medlemskap er implementert
        )
    }


    private fun nesteSaksnummer(): Saksnummer = UUID.randomUUID().toString().takeLast(19)
    private fun nesteBehandlingId(): BehandlingId = UUID.randomUUID().toString()
}