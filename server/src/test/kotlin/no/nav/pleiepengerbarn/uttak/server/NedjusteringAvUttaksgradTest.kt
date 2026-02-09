package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.NULL_PROSENT
import no.nav.pleiepengerbarn.uttak.testklient.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import java.util.*
import kotlin.random.Random
import kotlin.test.fail

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@ActiveProfiles("postgres")
@Tag("integration")
@EnableMockOAuth2Server
@AutoConfigureTestRestTemplate
class NedjusteringAvUttaksgradTest(@Autowired val restTemplate: TestRestTemplate) {

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    private val testClient by lazy {
        val token = mockOAuth2Server.issueToken(
            issuerId = "azure",
            audience = "pleiepenger-barn-uttak"
        ).serialize()
        PleiepengerBarnUttakTestClient(restTemplate, token)
    }

    @Test
    internal fun `Uttak med nedjustert søkers uttaksgrad grunnet inntektsgradering`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = UUID.randomUUID().toString(),
            saksnummer = "SAK1234"
        ).copy(
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        Inntektsgradering(uttaksgrad = BigDecimal.ZERO),
            )
        )

        val postResponse = testClient.opprettUttaksplan(grunnlag)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val uttaksplan = postResponse.body ?: fail("Mangler uttaksplan")



        uttaksplan.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            NULL_PROSENT,
            HUNDRE_PROSENT,
            Endringsstatus.NY
        )
        uttaksplan.assertOppfylt(LukketPeriode("2020-01-03/2020-01-03"), null, HUNDRE_PROSENT, Endringsstatus.NY)
        uttaksplan.assertOppfylt(LukketPeriode("2020-01-06/2020-01-08"), null, HUNDRE_PROSENT, Endringsstatus.NY)
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(KnekkpunktType.PLEIEBEHOV),
            endringsstatus = Endringsstatus.NY
        )

    }


    @Test
    internal fun `Uttak med nedjustert søkers uttaksgrad i forrige behandling med endring til gjeldende`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")
        val inntektsgraderingOriginal = BigDecimal.ZERO
        val grunnlagOriginal = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = UUID.randomUUID().toString(),
            saksnummer = nyttSaksnummer()
        ).copy(
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        Inntektsgradering(uttaksgrad = inntektsgraderingOriginal),
            )
        )

        val uttaksplanOriginal = testClient.opprettUttaksplan(grunnlagOriginal).body ?: fail("Mangler uttaksplan")

        uttaksplanOriginal.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            inntektsgraderingOriginal,
            HUNDRE_PROSENT,
            Endringsstatus.NY
        )
        uttaksplanOriginal.assertOppfylt(LukketPeriode("2020-01-03/2020-01-03"), null, HUNDRE_PROSENT, Endringsstatus.NY)
        uttaksplanOriginal.assertOppfylt(LukketPeriode("2020-01-06/2020-01-08"), null, HUNDRE_PROSENT, Endringsstatus.NY)
        uttaksplanOriginal.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(KnekkpunktType.PLEIEBEHOV),
            endringsstatus = Endringsstatus.NY
        )



        val inntektsgradering = BigDecimal.valueOf(30)
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = UUID.randomUUID().toString(),
            saksnummer = grunnlagOriginal.saksnummer
        ).copy(
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        Inntektsgradering(uttaksgrad = inntektsgradering),
            )
        )


        val uttaksplan = testClient.opprettUttaksplan(grunnlag).body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            inntektsgradering,
            HUNDRE_PROSENT,
            Endringsstatus.ENDRET
        )
        uttaksplan.assertOppfylt(LukketPeriode("2020-01-03/2020-01-03"), null, HUNDRE_PROSENT, Endringsstatus.UENDRET)
        uttaksplan.assertOppfylt(LukketPeriode("2020-01-06/2020-01-08"), null, HUNDRE_PROSENT, Endringsstatus.UENDRET)
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(KnekkpunktType.FORRIGE_UTTAKPLAN, KnekkpunktType.PLEIEBEHOV),
            endringsstatus = Endringsstatus.UENDRET
        )

    }

    @Test
    internal fun `Uttak med nedjustert søkers uttaksgrad i forrige behandling med endring til gjeldende og ulik periode`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")
        val originalBehandlingUUID = UUID.randomUUID().toString()
        val originalInntektsgradering = BigDecimal.ZERO
        val grunnlagOriginal = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = originalBehandlingUUID,
            saksnummer = nyttSaksnummer()
        ).copy(
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        Inntektsgradering(uttaksgrad = originalInntektsgradering),
            )
        )

        val uttaksplanOriginal = testClient.opprettUttaksplan(grunnlagOriginal).body ?: fail("Mangler uttaksplan")



        uttaksplanOriginal.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            originalInntektsgradering,
            HUNDRE_PROSENT,
            Endringsstatus.NY
        )
        uttaksplanOriginal.assertOppfylt(
            LukketPeriode("2020-01-03/2020-01-03"),
            null,
            HUNDRE_PROSENT,
            Endringsstatus.NY
        )
        uttaksplanOriginal.assertOppfylt(LukketPeriode("2020-01-06/2020-01-08"), null, HUNDRE_PROSENT, Endringsstatus.NY)
        uttaksplanOriginal.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(
                KnekkpunktType.PLEIEBEHOV
            ),
            endringsstatus = Endringsstatus.NY
        )



        val behandlingUUID = UUID.randomUUID().toString()
        val inntektsgraderingprosent = BigDecimal.valueOf(50)
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = behandlingUUID,
            saksnummer = grunnlagOriginal.saksnummer
        ).copy(
            sisteVedtatteUttaksplanForBehandling = mapOf(behandlingUUID to originalBehandlingUUID),
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-03") to
                        Inntektsgradering(uttaksgrad = inntektsgraderingprosent),
            )
        )

        val uttaksplan = testClient.opprettUttaksplan(grunnlag).body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            inntektsgraderingprosent,
            HUNDRE_PROSENT,
            Endringsstatus.ENDRET
        )
        uttaksplan.assertOppfylt(
            LukketPeriode("2020-01-03/2020-01-03"),
            inntektsgraderingprosent,
            HUNDRE_PROSENT,
            Endringsstatus.ENDRET
        )
        uttaksplan.assertOppfylt(LukketPeriode("2020-01-06/2020-01-08"), null, HUNDRE_PROSENT, Endringsstatus.UENDRET)
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(
                KnekkpunktType.FORRIGE_UTTAKPLAN,
                KnekkpunktType.PLEIEBEHOV,
                KnekkpunktType.ANNEN_PARTS_UTTAK
            ),
            endringsstatus = Endringsstatus.UENDRET
        )

    }

    @Test
    internal fun `Annen parts uttak med nedjustert søkers uttaksgrad - Uttaksplan for behandling med nedjustering i samme periode`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")
        val annenPartsBehandling = UUID.randomUUID().toString()
        val grunnlagOriginal = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = annenPartsBehandling,
            saksnummer = nyttSaksnummer()
        ).copy(
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-03") to
                        Inntektsgradering(uttaksgrad = BigDecimal.TEN),
            )
        )

        testClient.opprettUttaksplan(grunnlagOriginal)


        val nyBehandling = UUID.randomUUID().toString()
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = nyBehandling,
            saksnummer = nyttSaksnummer()
        ).copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(annenPartsBehandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(annenPartsBehandling to annenPartsBehandling)
        ).copy(
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-03") to
                        Inntektsgradering(uttaksgrad = BigDecimal.TEN),
            )
        )

        val uttaksplan = testClient.opprettUttaksplan(grunnlag).body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-03"),
            BigDecimal.valueOf(20),
            BigDecimal.valueOf(80),
            Endringsstatus.NY
        )
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(),
            endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(KnekkpunktType.PLEIEBEHOV, KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.NY
        )

    }


    @Test
    internal fun `Annen parts uttak med nedjustert søkers uttaksgrad - Uttaksplan for behandling uten nedjustering`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")
        val annenPartsBehandling = UUID.randomUUID().toString()
        val grunnlagOriginal = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = annenPartsBehandling,
            saksnummer = nyttSaksnummer()
        ).copy(
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        Inntektsgradering(uttaksgrad = BigDecimal.TEN),
            )
        )

        testClient.opprettUttaksplan(grunnlagOriginal)

        val nyBehandling = UUID.randomUUID().toString()
        val grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = nyBehandling,
            saksnummer = nyttSaksnummer()
        ).copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(annenPartsBehandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(annenPartsBehandling to annenPartsBehandling)
        )

        val uttaksplan = testClient.opprettUttaksplan(grunnlag).body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            null,
            BigDecimal.valueOf(80),
            Endringsstatus.NY
        )
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-03/2020-01-03"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(),
            endringsstatus = Endringsstatus.NY
        )
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(KnekkpunktType.PLEIEBEHOV, KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.NY
        )

    }


    @Test
    internal fun `Annen parts uttak med nedjustert søkers uttaksgrad under 20% - Uttaksplan for behandling uten nedjustering - annen parts sak revurderes`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")

        // Første søknad, Part 1, nedjustering av kvote fra 100 til 10
        val part1Behandling1 = UUID.randomUUID().toString()
        val part1Sak = nyttSaksnummer()
        val part1Grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = part1Behandling1,
            saksnummer = part1Sak
        ).copy(
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        Inntektsgradering(uttaksgrad = BigDecimal.TEN),
            )
        )

        testClient.opprettUttaksplan(part1Grunnlag)


        // Første søknad, Part 2, ingen nedjustering av kvote, får 100 - 20 = 80% i første periode, ellers avslag
        val part2Behandling = UUID.randomUUID().toString()
        val part2Sak = nyttSaksnummer()
        val part2Grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = part2Behandling,
            saksnummer = part2Sak
        ).copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling1, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(part1Behandling1 to part1Behandling1)
        )

        val part2Uttaksplan = testClient.opprettUttaksplan(part2Grunnlag).body ?: fail("Mangler uttaksplan")

        part2Uttaksplan.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            null,
            BigDecimal.valueOf(80),
            Endringsstatus.NY
        )
        part2Uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-03/2020-01-03"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.NY
        )
        part2Uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(),
            endringsstatus = Endringsstatus.NY
        )
        part2Uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(KnekkpunktType.PLEIEBEHOV, KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.NY
        )

        // Revurdering part 1, får 100 - 80 = 20% i første periode
        val part1Behandling2 = UUID.randomUUID().toString()
        val part2Grunnlag2 = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = part1Behandling2,
            saksnummer = part1Sak
        ).copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling2, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(
                part1Behandling2 to part1Behandling2,
                part2Behandling to part2Behandling
            )
        )

        val part2Uttaksplan2 = testClient.opprettUttaksplan(part2Grunnlag2).body ?: fail("Mangler uttaksplan")

        part2Uttaksplan2.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            null,
            BigDecimal.valueOf(20),
            Endringsstatus.ENDRET
        )
        part2Uttaksplan2.assertOppfylt(
            LukketPeriode("2020-01-03/2020-01-03"),
            null,
            HUNDRE_PROSENT,
            Endringsstatus.UENDRET
        )
        part2Uttaksplan2.assertOppfylt(
            LukketPeriode("2020-01-06/2020-01-08"),
            null,
            HUNDRE_PROSENT,
            Endringsstatus.UENDRET
        )
        part2Uttaksplan2.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(
                KnekkpunktType.FORRIGE_UTTAKPLAN,
                KnekkpunktType.PLEIEBEHOV,
                KnekkpunktType.ANNEN_PARTS_UTTAK
            ),
            endringsstatus = Endringsstatus.UENDRET
        )


        // Simulering part 2, gir 80% i første periode
        val grunnlagForrigeBehandling = part2Grunnlag.copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling2, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(
                part1Behandling2 to part1Behandling2,
                part2Behandling to part2Behandling
            )
        )
        val simulerUttaksplan = grunnlagForrigeBehandling.simulering();

        simulerUttaksplan.simulertUttaksplan.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            null,
            BigDecimal.valueOf(80),
            Endringsstatus.UENDRET
        )
        simulerUttaksplan.simulertUttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-03/2020-01-03"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(KnekkpunktType.FORRIGE_UTTAKPLAN, KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.UENDRET
        )
        simulerUttaksplan.simulertUttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(),
            endringsstatus = Endringsstatus.UENDRET
        )
        simulerUttaksplan.simulertUttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(
                KnekkpunktType.FORRIGE_UTTAKPLAN,
                KnekkpunktType.PLEIEBEHOV,
                KnekkpunktType.ANNEN_PARTS_UTTAK
            ),
            endringsstatus = Endringsstatus.UENDRET
        )

        assertThat(simulerUttaksplan.uttakplanEndret).isFalse();

    }


    @Test
    internal fun `Annen parts uttak med nedjustert søkers uttaksgrad over 20% - Uttaksplan for behandling uten nedjustering - annen parts sak revurderes`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")

        // Første søknad, Part 1, nedjustering av kvote fra 100 til 10
        val part1Behandling1 = UUID.randomUUID().toString()
        val part1Sak = nyttSaksnummer()
        val part1Grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = part1Behandling1,
            saksnummer = part1Sak
        ).copy(
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        Inntektsgradering(uttaksgrad = BigDecimal.valueOf(40)),
            )
        )

        testClient.opprettUttaksplan(part1Grunnlag)


        // Første søknad, Part 2, ingen nedjustering av kvote, får 100 - 40 = 60% i første periode, ellers avslag
        val part2Behandling = UUID.randomUUID().toString()
        val part2Sak = nyttSaksnummer()
        val part2Grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = part2Behandling,
            saksnummer = part2Sak
        ).copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling1, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(part1Behandling1 to part1Behandling1)
        )

        val part2Uttaksplan = testClient.opprettUttaksplan(part2Grunnlag).body ?: fail("Mangler uttaksplan")

        part2Uttaksplan.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            null,
            BigDecimal.valueOf(60),
            Endringsstatus.NY
        )
        part2Uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-03/2020-01-03"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.NY
        )
        part2Uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(),
            endringsstatus = Endringsstatus.NY
        )
        part2Uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(KnekkpunktType.PLEIEBEHOV, KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.NY
        )

        // Revurdering part 1, reduserer grunnet inntekt til 20%
        val part1Behandling2 = UUID.randomUUID().toString()
        val part2Grunnlag2 = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = part1Behandling2,
            saksnummer = part1Sak
        ).copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling2, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(
                part1Behandling2 to part1Behandling2,
                part2Behandling to part2Behandling
            ),
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        Inntektsgradering(uttaksgrad = BigDecimal.valueOf(20)),
            )

        )

        val part2Uttaksplan2 = testClient.opprettUttaksplan(part2Grunnlag2).body ?: fail("Mangler uttaksplan")

        part2Uttaksplan2.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            BigDecimal.valueOf(20),
            BigDecimal.valueOf(40),
            Endringsstatus.ENDRET
        )
        part2Uttaksplan2.assertOppfylt(
            LukketPeriode("2020-01-03/2020-01-03"),
            null,
            HUNDRE_PROSENT,
            Endringsstatus.UENDRET
        )
        part2Uttaksplan2.assertOppfylt(
            LukketPeriode("2020-01-06/2020-01-08"),
            null,
            HUNDRE_PROSENT,
            Endringsstatus.UENDRET
        )
        part2Uttaksplan2.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(
                KnekkpunktType.FORRIGE_UTTAKPLAN,
                KnekkpunktType.PLEIEBEHOV,
                KnekkpunktType.ANNEN_PARTS_UTTAK
            ),
            endringsstatus = Endringsstatus.UENDRET
        )


        // Simulering part 2, gir oppjustering fra 60% til 80% i første periode
        val grunnlagForrigeBehandling = part2Grunnlag.copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling2, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(
                part1Behandling2 to part1Behandling2,
                part2Behandling to part2Behandling
            )
        )
        val simulerUttaksplan = grunnlagForrigeBehandling.simulering();

        simulerUttaksplan.simulertUttaksplan.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            null,
            BigDecimal.valueOf(80),
            Endringsstatus.ENDRET
        )
        simulerUttaksplan.simulertUttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-03/2020-01-03"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(KnekkpunktType.FORRIGE_UTTAKPLAN, KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.UENDRET
        )
        simulerUttaksplan.simulertUttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(),
            endringsstatus = Endringsstatus.UENDRET
        )
        simulerUttaksplan.simulertUttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(
                KnekkpunktType.FORRIGE_UTTAKPLAN,
                KnekkpunktType.PLEIEBEHOV,
                KnekkpunktType.ANNEN_PARTS_UTTAK
            ),
            endringsstatus = Endringsstatus.UENDRET
        )

        assertThat(simulerUttaksplan.uttakplanEndret).isTrue();

    }

    @Test
    internal fun `Annen parts uttak med nedjustert søkers uttaksgrad over 20% - Uttaksplan for behandling med nedjustering - annen parts sak revurderes`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")

        // Første søknad, Part 1, nedjustering av kvote fra 100 til 51
        val part1Behandling1 = UUID.randomUUID().toString()
        val part1Sak = nyttSaksnummer()
        val part1Grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = part1Behandling1,
            saksnummer = part1Sak
        ).copy(
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        Inntektsgradering(uttaksgrad = BigDecimal.valueOf(51.26)),
            )
        )

        testClient.opprettUttaksplan(part1Grunnlag)


        // Første søknad, Part 2, nedjustering av kvote, får 100 - 51 = 49% i første periode, ellers avslag
        val part2Behandling = UUID.randomUUID().toString()
        val part2Sak = nyttSaksnummer()
        val part2Grunnlag = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = part2Behandling,
            saksnummer = part2Sak
        ).copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling1, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(part1Behandling1 to part1Behandling1)
        )

        val part2Uttaksplan = testClient.opprettUttaksplan(part2Grunnlag).body ?: fail("Mangler uttaksplan")

        part2Uttaksplan.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            null,
            BigDecimal.valueOf(49),
            Endringsstatus.NY
        )
        part2Uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-03/2020-01-03"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.NY
        )
        part2Uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(),
            endringsstatus = Endringsstatus.NY
        )
        part2Uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(KnekkpunktType.PLEIEBEHOV, KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.NY
        )

        // Revurdering part 1, ingen endring
        val part1Behandling2 = UUID.randomUUID().toString()
        val part2Grunnlag2 = lagGrunnlag(
            søknadsperiode = søknadsperiode,
            arbeid = listOf(
                Arbeid(
                    ARBEIDSFORHOLD1,
                    mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING))
                )
            ),
            pleiebehov = mapOf(LukketPeriode("2020-01-01/2020-01-08") to Pleiebehov.PROSENT_100),
            behandlingUUID = part1Behandling2,
            saksnummer = part1Sak
        ).copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling2, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(
                part1Behandling2 to part1Behandling2,
                part2Behandling to part2Behandling
            ),
            inntektsgradering = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        Inntektsgradering(uttaksgrad = BigDecimal.valueOf(51.26)),
            )
        )

        val part2Uttaksplan2 = testClient.opprettUttaksplan(part2Grunnlag2).body ?: fail("Mangler uttaksplan")

        part2Uttaksplan2.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            BigDecimal.valueOf(51),
            BigDecimal.valueOf(51),
            Endringsstatus.ENDRET
        )
        part2Uttaksplan2.assertOppfylt(
            LukketPeriode("2020-01-03/2020-01-03"),
            null,
            HUNDRE_PROSENT,
            Endringsstatus.UENDRET
        )
        part2Uttaksplan2.assertOppfylt(
            LukketPeriode("2020-01-06/2020-01-08"),
            null,
            HUNDRE_PROSENT,
            Endringsstatus.UENDRET
        )
        part2Uttaksplan2.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(
                KnekkpunktType.FORRIGE_UTTAKPLAN,
                KnekkpunktType.PLEIEBEHOV,
                KnekkpunktType.ANNEN_PARTS_UTTAK
            ),
            endringsstatus = Endringsstatus.UENDRET
        )


        // Simulering part 2, gir ingen endring
        val grunnlagForrigeBehandling = part2Grunnlag.copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling2, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(
                part1Behandling2 to part1Behandling2,
                part2Behandling to part2Behandling
            )
        )
        val simulerUttaksplan = grunnlagForrigeBehandling.simulering();

        simulerUttaksplan.simulertUttaksplan.assertOppfylt(
            LukketPeriode("2020-01-01/2020-01-02"),
            null,
            BigDecimal.valueOf(49),
            Endringsstatus.UENDRET
        )
        simulerUttaksplan.simulertUttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-03/2020-01-03"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(KnekkpunktType.FORRIGE_UTTAKPLAN, KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.UENDRET
        )
        simulerUttaksplan.simulertUttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-06/2020-01-08"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(),
            endringsstatus = Endringsstatus.UENDRET
        )
        simulerUttaksplan.simulertUttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(
                KnekkpunktType.FORRIGE_UTTAKPLAN,
                KnekkpunktType.PLEIEBEHOV,
                KnekkpunktType.ANNEN_PARTS_UTTAK
            ),
            endringsstatus = Endringsstatus.UENDRET
        )

        assertThat(simulerUttaksplan.uttakplanEndret).isFalse();

    }




    private fun Uttaksgrunnlag.simulering(): Simulering {
        val postResponse = testClient.simulerUttaksplan(this)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.OK)
        return postResponse.body ?: fail("Mangler simulering")
    }

    private fun Uttaksplan.assertOppfylt(
        periode: LukketPeriode,
        nedjustertGrad: Prosent? = HUNDRE_PROSENT,
        fullGrad: Prosent = HUNDRE_PROSENT,
        endringsstatus: Endringsstatus = Endringsstatus.NY,
    ) {
        val periodeInfo = perioder[periode] ?: fail("Finner ikke periode: $periode")
        when (periodeInfo.utfall) {
            Utfall.OPPFYLT -> {
                if (nedjustertGrad != null) {
                    assertThat(periodeInfo.uttaksgrad).isEqualByComparingTo(nedjustertGrad)
                    assertThat(periodeInfo.uttaksgradMedReduksjonGrunnetInntektsgradering).isEqualByComparingTo(
                        nedjustertGrad
                    )
                    assertThat(periodeInfo.uttaksgradUtenReduksjonGrunnetInntektsgradering).isEqualByComparingTo(
                        fullGrad
                    )
                    assertThat(periodeInfo.endringsstatus).isEqualTo(endringsstatus)
                } else {
                    assertThat(periodeInfo.uttaksgrad).isEqualByComparingTo(fullGrad)
                    assertThat(periodeInfo.uttaksgradMedReduksjonGrunnetInntektsgradering).isNull()
                    //assertThat(periodeInfo.uttaksgradUtenReduksjonGrunnetInntektsgradering).isNull()
                    assertThat(periodeInfo.endringsstatus).isEqualTo(endringsstatus)
                }
            }

            else -> fail("Perioden $periode er ikke oppfylt")
        }
    }

    private fun Uttaksplan.assertIkkeOppfylt(
        periode: LukketPeriode,
        ikkeOppfyltÅrsaker: Set<Årsak> = setOf(),
        knekkpunktTyper: Set<KnekkpunktType> = setOf(),
        endringsstatus: Endringsstatus
    ) {
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

    private fun nyttSaksnummer(): String = "SAK${Random.nextInt(9999)}"
}
