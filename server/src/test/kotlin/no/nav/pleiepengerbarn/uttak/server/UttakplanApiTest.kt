package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.InnvilgetPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
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

private const val UTTAKSPLAN_PATH = "/uttaksplan"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UttakplanApiTest(@Autowired val restTemplate: TestRestTemplate) {

    @Test
    fun `Opprett uttaksplan`() {
        val requestBody = """
            {
                "søker": {
                    "fødselsdato": "1990-09-29"
                },
                "saksnummer": "123",
                "behandlingId": "474abb91-0e61-4459-ba5f-7e960d45c165",
                "søknadsperioder": [
                    "2020-01-01/2020-03-31"
                ],
                "arbeid": [
                    {
                        "arbeidsforhold": {
                            "arbeidsforholdId": "123-456-789"
                        },
                        "perioder" : {
                            "2020-01-01/2020-03-31": {
                                "jobberNormaltPerUke": "PT37H30M",
                                "skalJobbeProsent": "0"
                            }
                        }
                    }
                ],
                "tilsynsbehov": {
                    "2020-01-01/2020-03-31": {
                        "prosent": 100
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
    }

}