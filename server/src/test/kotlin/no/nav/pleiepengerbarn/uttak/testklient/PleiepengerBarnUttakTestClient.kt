package no.nav.pleiepengerbarn.uttak.testklient

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.server.UttakplanApi
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity

internal class PleiepengerBarnUttakTestClient(private val restTemplate: TestRestTemplate) {

    internal fun opprettUttaksplan(grunnlag: Uttaksgrunnlag): ResponseEntity<Uttaksplan> {
        return restTemplate.exchange(UttakplanApi.UttaksplanPath, HttpMethod.POST, HttpEntity<Any>(grunnlag, headers()), Uttaksplan::class.java)
    }

    internal fun simulerUttaksplan(grunnlag: Uttaksgrunnlag): ResponseEntity<Simulering> {
        return restTemplate.exchange(UttakplanApi.UttaksplanSimuleringPath, HttpMethod.POST, HttpEntity<Any>(grunnlag, headers()), Simulering::class.java)
    }

    internal fun hentUttaksplan(behandlingUUID: BehandlingUUID): ResponseEntity<Uttaksplan> {
        return restTemplate.exchange(UttakplanApi.UttaksplanPath + "?behandlingUUID=$behandlingUUID", HttpMethod.GET, HttpEntity<Any>(headers()), Uttaksplan::class.java)
    }

    internal fun hentUttaksplan(behandlingUUID: BehandlingUUID, slåSammenLikePerioder: Boolean): ResponseEntity<Uttaksplan> {
        return restTemplate.exchange(UttakplanApi.UttaksplanPath + "?behandlingUUID=$behandlingUUID&slåSammenLikePerioder=$slåSammenLikePerioder", HttpMethod.GET, HttpEntity<Any>(headers()), Uttaksplan::class.java)
    }

    internal fun slettUttaksplan(behandlingUUID: BehandlingUUID) {
        restTemplate.exchange(UttakplanApi.UttaksplanPath + "?behandlingUUID=$behandlingUUID", HttpMethod.DELETE, HttpEntity<Any>(headers()), Unit::class.java)
    }

    private fun headers():HttpHeaders {
        val headers = HttpHeaders()
        headers.put("NAV_PSB_UTTAK_TOKEN", listOf("no_secret"))
        return headers
    }

}

internal fun testClientMotLokalServer(): PleiepengerBarnUttakTestClient {
    val restTemplateBuilder = RestTemplateBuilder()
        .rootUri("http://localhost:8080/pleiepenger-barn-uttak/")
    return PleiepengerBarnUttakTestClient(TestRestTemplate(restTemplateBuilder))
}
