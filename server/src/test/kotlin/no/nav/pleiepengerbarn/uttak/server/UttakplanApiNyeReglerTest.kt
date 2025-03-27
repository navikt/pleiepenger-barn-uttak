package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import no.nav.pleiepengerbarn.uttak.testklient.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
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
@EnableMockOAuth2Server
class UttakplanApiNyeReglerTest(@Autowired val restTemplate: TestRestTemplate) {

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    private val testClient by lazy {
        val token = mockOAuth2Server.issueToken(
            issuerId = "azure",
            audience = "pleiepenger-barn-uttak"
        ).serialize()
        PleiepengerBarnUttakTestClient(restTemplate, token)
    }

    @AfterEach
    internal fun tearDown() {
        System.clearProperty("INKLUDER_TILKOMMET_UTEN_ARBEIDSTID")
    }

    @Test
    internal fun `IKKE_YRKESAKTIV skal spesialhåndteres fra satt dato og settes til hundre prosent`() {
        val søknadsperiode = LukketPeriode("2023-05-29/2023-06-02")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    IKKE_YRKESAKTIV,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                ),
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(
                        søknadsperiode to ArbeidsforholdPeriodeInfo(
                            jobberNormalt = FULL_DAG,
                            jobberNå = FULL_DAG.prosent(50)
                        )
                    )
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2023-05-29/2023-06-02") to Pleiebehov.PROSENT_100),
            nyeReglerUtbetalingsgrad = LocalDate.parse("2023-06-01")
        )

        val postResponse = testClient.opprettUttaksplan(grunnlag)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val hentResponse = testClient.hentUttaksplan(grunnlag.behandlingUUID)
        assertThat(hentResponse.statusCode).isEqualTo(HttpStatus.OK)
        val uttaksplan = hentResponse.body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(
            perioder = listOf(LukketPeriode("2023-05-29/2023-05-31")),
            grad = Prosent(50),
            gradPerArbeidsforhold = mapOf(
                IKKE_YRKESAKTIV to Prosent(50),
                ARBEIDSFORHOLD1 to Prosent(50)
            ),
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            endringsstatus = Endringsstatus.NY
        )

        uttaksplan.assertOppfylt(
            perioder = listOf(LukketPeriode("2023-06-01/2023-06-02")),
            grad = Prosent(75),
            gradPerArbeidsforhold = mapOf(
                IKKE_YRKESAKTIV to HUNDRE_PROSENT,
                ARBEIDSFORHOLD1 to Prosent(50)
            ),
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            endringsstatus = Endringsstatus.NY
        )
    }


    @Test
    internal fun `Overstyrer kun utbetalingsgrad for ett arbeidsforhold`() {
        val søknadsperiode = LukketPeriode("2020-01-06/2020-01-10")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    IKKE_YRKESAKTIV,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                ),
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(
                        søknadsperiode to ArbeidsforholdPeriodeInfo(
                            jobberNormalt = FULL_DAG,
                            jobberNå = FULL_DAG.prosent(50)
                        )
                    )
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-06/2020-01-10") to Pleiebehov.PROSENT_100),
        ).copy(
            overstyrtInput = mapOf(
                LukketPeriode("2020-01-06/2020-01-08") to
                        OverstyrtInput(
                            overstyrtUttaksgrad = null,
                            skalUttaksgradOverstyreTimerDekket = null,
                            overstyrtUtbetalingsgradPerArbeidsforhold = listOf(
                                OverstyrtUtbetalingsgradPerArbeidsforhold(
                                    overstyrtUtbetalingsgrad = BigDecimal.ZERO,
                                    arbeidsforhold = IKKE_YRKESAKTIV
                                )
                            )
                        )
            )
        )

        val postResponse = testClient.opprettUttaksplan(grunnlag)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val hentResponse = testClient.hentUttaksplan(grunnlag.behandlingUUID)
        assertThat(hentResponse.statusCode).isEqualTo(HttpStatus.OK)
        val uttaksplan = hentResponse.body ?: fail("Mangler uttaksplan")

        // perioden hvor kun IKKE_YRKESAKTIV har blitt overstyrt
        uttaksplan.assertOppfylt(
            perioder = listOf(LukketPeriode("2020-01-06/2020-01-08")),
            grad = Prosent(50),
            gradPerArbeidsforhold = mapOf(
                IKKE_YRKESAKTIV to BigDecimal.ZERO,
                ARBEIDSFORHOLD1 to Prosent(50)
            ),
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertManueltOverstyrt(LukketPeriode("2020-01-06/2020-01-08"), true)

        // perioden hvor ingen aktiviteter har blitt overstyrt
        uttaksplan.assertOppfylt(
            perioder = listOf(LukketPeriode("2020-01-09/2020-01-10")),
            grad = Prosent(50),
            gradPerArbeidsforhold = mapOf(
                IKKE_YRKESAKTIV to Prosent(50),
                ARBEIDSFORHOLD1 to Prosent(50)
            ),
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertManueltOverstyrt(LukketPeriode("2020-01-09/2020-01-10"), false)

    }


    @Test
    internal fun `IKKE_YRKESAKTIV skal spesialhåndteres fra satt dato og settes til hundre prosent når tilkommen er satt til true`() {
        val søknadsperiode = LukketPeriode("2023-05-29/2023-06-02")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    IKKE_YRKESAKTIV,
                    mapOf(
                        søknadsperiode to ArbeidsforholdPeriodeInfo(
                            jobberNormalt = FULL_DAG,
                            jobberNå = INGENTING,
                            tilkommet = false
                        )
                    )
                ),
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(
                        LukketPeriode("2023-05-29/2023-05-31") to ArbeidsforholdPeriodeInfo(
                            jobberNormalt = Duration.ofHours(
                                4
                            ), Duration.ofHours(1), tilkommet = false
                        ), // k9-sak knekker perioden og setter false på feature-toggle-dato.
                        LukketPeriode("2023-06-01/2023-06-02") to ArbeidsforholdPeriodeInfo(
                            jobberNormalt = Duration.ofHours(
                                4
                            ), Duration.ofHours(1), tilkommet = true
                        )
                    )
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2023-05-29/2023-06-02") to Pleiebehov.PROSENT_100),
            nyeReglerUtbetalingsgrad = LocalDate.parse("2023-06-01")
        )

        val postResponse = testClient.opprettUttaksplan(grunnlag)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val hentResponse = testClient.hentUttaksplan(grunnlag.behandlingUUID)
        assertThat(hentResponse.statusCode).isEqualTo(HttpStatus.OK)
        val uttaksplan = hentResponse.body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(
            perioder = listOf(LukketPeriode("2023-05-29/2023-05-31")),
            grad = Prosent(75),
            gradPerArbeidsforhold = mapOf(
                IKKE_YRKESAKTIV to Prosent(75),
                ARBEIDSFORHOLD1 to Prosent(75)
            ),
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertOppfylt(
            perioder = listOf(LukketPeriode("2023-06-01/2023-06-02")),
            grad = HUNDRE_PROSENT,
            gradPerArbeidsforhold = mapOf(
                IKKE_YRKESAKTIV to Prosent(100),
                ARBEIDSFORHOLD1 to Prosent(0)
            ),

            /*
             * Ved tilkommet aktivitet får man årsaken AVKORTET_MOT_INNTEKT
             * fremfor FULL_DEKNING
             */
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            endringsstatus = Endringsstatus.NY
        )

    }


    private fun opprettUttakUtenlandsopphold(
        søknadsperiode: LukketPeriode,
        utenlandsoppholdÅrsak: UtenlandsoppholdÅrsak = UtenlandsoppholdÅrsak.INGEN
    ): Saksnummer {
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
        ).copy(
            utenlandsoppholdperioder = mapOf(søknadsperiode to UtenlandsoppholdInfo(utenlandsoppholdÅrsak, "USA"))
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

    private fun Uttaksplan.assertOppfylt(
        perioder: List<LukketPeriode>,
        grad: Prosent = HUNDRE_PROSENT,
        gradPerArbeidsforhold: Map<Arbeidsforhold, Prosent> = mapOf(ARBEIDSFORHOLD1 to HUNDRE_PROSENT),
        oppfyltÅrsak: Årsak = Årsak.FULL_DEKNING,
        endringsstatus: Endringsstatus
    ) {
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
                    val utbetalingsgrad =
                        periodeInfo.utbetalingsgrader.first { it.arbeidsforhold == arbeidsforhold }.utbetalingsgrad
                    assertThat(utbetalingsgrad).isEqualByComparingTo(prosent)
                }
                assertThat(periodeInfo.utenlandsoppholdUtenÅrsak).isEqualTo(utenlandsoppholdUtenÅrsak)
            }

            else -> fail("Perioden $periode er ikke oppfylt")
        }
    }

    private fun Uttaksplan.assertManueltOverstyrt(periode: LukketPeriode, manueltOverstyrt: Boolean) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        assertThat(periodeInfo.manueltOverstyrt).isEqualTo(manueltOverstyrt)
    }

    private fun Duration.prosent(prosent: Long): Duration {
        return this.multipliedBy(prosent).dividedBy(100)
    }

}
