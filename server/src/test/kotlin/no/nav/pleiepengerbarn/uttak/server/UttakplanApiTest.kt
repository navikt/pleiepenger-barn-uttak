package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.time.LocalDate
import java.time.Month
import java.util.*

private const val UTTAKSPLAN_PATH = "/uttaksplan"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
internal class UttakplanApiTest(@Autowired val restTemplate: TestRestTemplate) {



    @Test
    fun `Opprett uttaksplan`() {

        val behandlingId = UUID.randomUUID().toString()
        //
        // Lagre ny uttaksplan
        //
        val requestBody = """
            {
                "søker": {
                    "fødselsdato": "1990-09-29"
                },
                "saksnummer": "123",
                "behandlingId": "$behandlingId",
                "søknadsperioder": [
                    "2020-01-01/2020-03-31"
                ],
                "arbeid": [
                    {
                        "arbeidsforhold": {
                            "arbeidsforholdId": "6e914bb2-e282-4ff9-844d-8b10a968133e"
                        },
                        "perioder" : {
                            "2020-01-01/2020-03-31": {  
                                "jobberNormaltPerUke": "PT37H30M",
                                "skalJobbeProsent": 0
                            }
                        }
                    }
                ],
                "tilsynsbehov": {
                    "2020-01-01/2020-03-31": {
                        "prosent": 100
                    }
                },
                "tilsynsperioder": {
                    "2020-01-01/2020-03-31": {
                        "lengde": "PT5H"
                    }
                },
                "medlemskap": {
                    "2020-01-01/2020-03-31": {}
                }
            }
        """.trimIndent()


        val request = RequestEntity
                .post(URI.create(UTTAKSPLAN_PATH))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)



        val response = restTemplate.exchange(request, Uttaksplan::class.java)
        val uttaksplan = response.body

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)

        assertThat(uttaksplan).isNotNull()
        assertThat(uttaksplan?.perioder?.keys).hasSize(1)
        val periode = uttaksplan?.perioder?.keys?.first()
        val info = uttaksplan?.perioder?.get(periode)
        assertThat(periode).isEqualTo(LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.MARCH, 31)))
        assertThat(info?.knekkpunktTyper()).isEmpty()
        assertTrue(info is InnvilgetPeriode)
        val innvilgetPeriode = info as InnvilgetPeriode
        assertThat(innvilgetPeriode.grad).isEqualByComparingTo(Prosent(100))


        //
        // Hent uttaksplan
        //
        val getResponse = restTemplate.getForEntity(
                URI.create(UTTAKSPLAN_PATH + "?behandlingId=$behandlingId"),
                Uttaksplan::class.java)
        val uttaksplaner = response.body

        assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
        println(uttaksplaner)
    }

}