package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.NULL_PROSENT
import no.nav.pleiepengerbarn.uttak.testklient.*
import no.nav.pleiepengerbarn.uttak.testklient.FULL_DAG
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID
import kotlin.test.fail

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@ActiveProfiles("postgres")
@Tag("integration")
class NedjusteringAvUttaksgradTest(@Autowired val restTemplate: TestRestTemplate) {

    private val testClient by lazy { PleiepengerBarnUttakTestClient(restTemplate) }

    @BeforeEach
    internal fun setUp() {
        System.setProperty("GIR_ALDRI_MER_ENN_60_DAGER", "true")
    }

    @AfterEach
    internal fun tearDown() {
        System.clearProperty("GIR_ALDRI_MER_ENN_60_DAGER")
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
            saksnummer=RandomStringUtils.random(4)
        )

        val postResponse = testClient.opprettUttaksplan(grunnlag)
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val grunnlagNedjustertUttaksgrad = grunnlag.copy(
            nedjustertSøkersUttaksgrad = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        NedjustertUttaksgrad(uttaksgrad = BigDecimal.ZERO),
                )
        )


        val postResponseNedjustering = testClient.nedjusterSøkersUttaksgrad(grunnlagNedjustertUttaksgrad)
        val uttaksplan = postResponseNedjustering.body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(LukketPeriode("2020-01-01/2020-01-02"), NULL_PROSENT, HUNDRE_PROSENT, Endringsstatus.NY)
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
            saksnummer=RandomStringUtils.random(4)
        )

        testClient.opprettUttaksplan(grunnlagOriginal)

        val grunnlagNedjustertUttaksgradOriginal = grunnlagOriginal.copy(
            nedjustertSøkersUttaksgrad = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        NedjustertUttaksgrad(uttaksgrad = BigDecimal.ZERO),
            )
        )

        testClient.nedjusterSøkersUttaksgrad(grunnlagNedjustertUttaksgradOriginal)

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
            saksnummer=grunnlagOriginal.saksnummer
        )

        testClient.opprettUttaksplan(grunnlag)

        val grunnlagNedjustertUttaksgrad = grunnlag.copy(
            nedjustertSøkersUttaksgrad = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        NedjustertUttaksgrad(uttaksgrad = BigDecimal.TEN),
            )
        )

        val postResponseNedjustering = testClient.nedjusterSøkersUttaksgrad(grunnlagNedjustertUttaksgrad)
        val uttaksplan = postResponseNedjustering.body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(LukketPeriode("2020-01-01/2020-01-02"), BigDecimal.TEN, HUNDRE_PROSENT, Endringsstatus.ENDRET)
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
            saksnummer=RandomStringUtils.random(4)
        )

        testClient.opprettUttaksplan(grunnlagOriginal)

        val grunnlagNedjustertUttaksgradOriginal = grunnlagOriginal.copy(
            nedjustertSøkersUttaksgrad = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        NedjustertUttaksgrad(uttaksgrad = BigDecimal.ZERO),
            )
        )

        testClient.nedjusterSøkersUttaksgrad(grunnlagNedjustertUttaksgradOriginal)

        val behandlingUUID = UUID.randomUUID().toString()
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
            saksnummer=grunnlagOriginal.saksnummer
        ).copy(sisteVedtatteUttaksplanForBehandling = mapOf(behandlingUUID to originalBehandlingUUID))

        testClient.opprettUttaksplan(grunnlag)

        val grunnlagNedjustertUttaksgrad = grunnlag.copy(
            nedjustertSøkersUttaksgrad = mapOf(
                LukketPeriode("2020-01-01/2020-01-03") to
                        NedjustertUttaksgrad(uttaksgrad = BigDecimal.TEN),
            )
        )

        val postResponseNedjustering = testClient.nedjusterSøkersUttaksgrad(grunnlagNedjustertUttaksgrad)
        val uttaksplan = postResponseNedjustering.body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(LukketPeriode("2020-01-01/2020-01-02"), BigDecimal.TEN, HUNDRE_PROSENT, Endringsstatus.ENDRET)
        uttaksplan.assertOppfylt(LukketPeriode("2020-01-03/2020-01-03"), BigDecimal.TEN, HUNDRE_PROSENT, Endringsstatus.ENDRET)
        uttaksplan.assertOppfylt(LukketPeriode("2020-01-06/2020-01-08"), null, HUNDRE_PROSENT, Endringsstatus.UENDRET)
        uttaksplan.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(KnekkpunktType.FORRIGE_UTTAKPLAN, KnekkpunktType.PLEIEBEHOV, KnekkpunktType.ANNEN_PARTS_UTTAK),
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
            saksnummer=RandomStringUtils.random(4)
        )

        testClient.opprettUttaksplan(grunnlagOriginal)

        val grunnlagNedjustertUttaksgradOriginal = grunnlagOriginal.copy(
            nedjustertSøkersUttaksgrad = mapOf(
                LukketPeriode("2020-01-01/2020-01-03") to
                        NedjustertUttaksgrad(uttaksgrad = BigDecimal.TEN),
            )
        )

        testClient.nedjusterSøkersUttaksgrad(grunnlagNedjustertUttaksgradOriginal)

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
            saksnummer=RandomStringUtils.random(4)
        ).copy(kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(annenPartsBehandling)), sisteVedtatteUttaksplanForBehandling = mapOf(annenPartsBehandling to annenPartsBehandling))

        testClient.opprettUttaksplan(grunnlag)

        val grunnlagNedjustertUttaksgrad = grunnlag.copy(
            nedjustertSøkersUttaksgrad = mapOf(
                LukketPeriode("2020-01-01/2020-01-03") to
                        NedjustertUttaksgrad(uttaksgrad = BigDecimal.TEN),
            )
        )

        val postResponseNedjustering = testClient.nedjusterSøkersUttaksgrad(grunnlagNedjustertUttaksgrad)
        val uttaksplan = postResponseNedjustering.body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(LukketPeriode("2020-01-01/2020-01-03"), BigDecimal.TEN, BigDecimal.valueOf(90), Endringsstatus.NY)
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
            saksnummer=RandomStringUtils.random(4)
        )

        testClient.opprettUttaksplan(grunnlagOriginal)

        val grunnlagNedjustertUttaksgradOriginal = grunnlagOriginal.copy(
            nedjustertSøkersUttaksgrad = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        NedjustertUttaksgrad(uttaksgrad = BigDecimal.TEN),
            )
        )

        testClient.nedjusterSøkersUttaksgrad(grunnlagNedjustertUttaksgradOriginal)

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
            saksnummer=RandomStringUtils.random(4)
        ).copy(kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(annenPartsBehandling)), sisteVedtatteUttaksplanForBehandling = mapOf(annenPartsBehandling to annenPartsBehandling))

        val opprettUttaksplanResponse = testClient.opprettUttaksplan(grunnlag)
        val uttaksplan = opprettUttaksplanResponse.body ?: fail("Mangler uttaksplan")

        uttaksplan.assertOppfylt(LukketPeriode("2020-01-01/2020-01-02"), null, BigDecimal.valueOf(90), Endringsstatus.NY)
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


    // Denne testen er litt rar, må vi lage logikk som låser grad/innvilgelse for å unngå avslag under 20%?
    @Test
    internal fun `Annen parts uttak med nedjustert søkers uttaksgrad - Uttaksplan for behandling uten nedjustering - annen parts sak revurderes`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-10")

        // Første søknad, Part 1, nedjustering av kvote fra 100 til 10
        val part1Behandling1 = UUID.randomUUID().toString()
        val part1Sak = RandomStringUtils.random(4)
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
            saksnummer= part1Sak
        )

        testClient.opprettUttaksplan(part1Grunnlag)

        val part1GrunnlagNedjustertUttaksgrad = part1Grunnlag.copy(
            nedjustertSøkersUttaksgrad = mapOf(
                LukketPeriode("2020-01-01/2020-01-02") to
                        NedjustertUttaksgrad(uttaksgrad = BigDecimal.TEN),
            )
        )

        testClient.nedjusterSøkersUttaksgrad(part1GrunnlagNedjustertUttaksgrad)

        // Første søknad, Part 2, ingen nedjustering av kvote, får 100 - 10 = 90% i første periode, ellers avslag
        val part2Behandling = UUID.randomUUID().toString()
        val part2Sak = RandomStringUtils.random(4)
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
            saksnummer= part2Sak
        ).copy(kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling1, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(part1Behandling1 to part1Behandling1))

        val part2Uttaksplan = testClient.opprettUttaksplan(part2Grunnlag).body ?: fail("Mangler uttaksplan")

        part2Uttaksplan.assertOppfylt(LukketPeriode("2020-01-01/2020-01-02"), null, BigDecimal.valueOf(90), Endringsstatus.NY)
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

        // Revurdering part 1, får 100 - 90 = 10% i første periode => avslag
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
            saksnummer=part1Sak
        ).copy(kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling2, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(part1Behandling2 to part1Behandling2, part2Behandling to part2Behandling))

        val part2Uttaksplan2 = testClient.opprettUttaksplan(part2Grunnlag2).body ?: fail("Mangler uttaksplan")

        part2Uttaksplan2.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-01/2020-01-02"),
            ikkeOppfyltÅrsaker = setOf(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE),
            knekkpunktTyper = setOf(),
            endringsstatus = Endringsstatus.ENDRET
        )
        part2Uttaksplan2.assertOppfylt(LukketPeriode("2020-01-03/2020-01-03"), null, HUNDRE_PROSENT, Endringsstatus.UENDRET)
        part2Uttaksplan2.assertOppfylt(LukketPeriode("2020-01-06/2020-01-08"), null, HUNDRE_PROSENT, Endringsstatus.UENDRET)
        part2Uttaksplan2.assertIkkeOppfylt(
            periode = LukketPeriode("2020-01-09/2020-01-10"),
            ikkeOppfyltÅrsaker = setOf(Årsak.UTENOM_PLEIEBEHOV),
            knekkpunktTyper = setOf(KnekkpunktType.FORRIGE_UTTAKPLAN, KnekkpunktType.PLEIEBEHOV, KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.UENDRET
        )



        // Simulering part 2, gir 100% i første periode
        val grunnlagForrigeBehandling = part2Grunnlag.copy(
            kravprioritetForBehandlinger = mapOf(søknadsperiode to listOf(part1Behandling2, part2Behandling)),
            sisteVedtatteUttaksplanForBehandling = mapOf(
                part1Behandling2 to part1Behandling2,
                part2Behandling to part2Behandling
            )
        )
        val simulerUttaksplan = grunnlagForrigeBehandling.simulering();

        simulerUttaksplan.simulertUttaksplan.assertOppfylt(LukketPeriode("2020-01-01/2020-01-02"), null, HUNDRE_PROSENT, Endringsstatus.ENDRET)
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
            knekkpunktTyper = setOf(KnekkpunktType.FORRIGE_UTTAKPLAN, KnekkpunktType.PLEIEBEHOV, KnekkpunktType.ANNEN_PARTS_UTTAK),
            endringsstatus = Endringsstatus.UENDRET
        )

        assertThat(simulerUttaksplan.uttakplanEndret).isTrue();

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
                    assertThat(periodeInfo.uttaksgradMedReduksjonGrunnetInntektsgradering).isEqualByComparingTo(nedjustertGrad)
                    assertThat(periodeInfo.uttaksgradUtenReduksjonGrunnetInntektsgradering).isEqualByComparingTo(fullGrad)
                    assertThat(periodeInfo.endringsstatus).isEqualTo(endringsstatus)
                } else {
                    assertThat(periodeInfo.uttaksgrad).isEqualByComparingTo(fullGrad)
                    assertThat(periodeInfo.uttaksgradMedReduksjonGrunnetInntektsgradering).isNull()
                    assertThat(periodeInfo.uttaksgradUtenReduksjonGrunnetInntektsgradering).isNull()
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

}
