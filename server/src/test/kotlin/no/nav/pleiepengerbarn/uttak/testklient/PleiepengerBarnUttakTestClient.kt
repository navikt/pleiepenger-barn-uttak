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
        return restTemplate.exchange(UttakplanApi.UttaksplanPath, HttpMethod.POST, HttpEntity<Any>(grunnlag, HttpHeaders()), Uttaksplan::class.java)
    }

    internal fun hentUttaksplan(behandlingUUID: BehandlingUUID): ResponseEntity<Uttaksplan> {
        return restTemplate.exchange(UttakplanApi.UttaksplanPath + "?behandlingUUID=$behandlingUUID", HttpMethod.GET, HttpEntity<Any>(HttpHeaders()), Uttaksplan::class.java)
    }

    internal fun hentForenkletUttaksplan(behandlingUUID: BehandlingUUID): ResponseEntity<ForenkletUttaksplan> {
        return restTemplate.exchange(UttakplanApi.FullUttaksplanForTilkjentYtelsePath + "?behandlingUUID=$behandlingUUID", HttpMethod.GET, HttpEntity<Any>(HttpHeaders()), ForenkletUttaksplan::class.java)
    }

}

internal fun testClientMotLokalServer(): PleiepengerBarnUttakTestClient {
    val restTemplateBuilder = RestTemplateBuilder()
        .rootUri("http://localhost:8080/pleiepenger-barn-uttak/")
    return PleiepengerBarnUttakTestClient(TestRestTemplate(restTemplateBuilder))
}
