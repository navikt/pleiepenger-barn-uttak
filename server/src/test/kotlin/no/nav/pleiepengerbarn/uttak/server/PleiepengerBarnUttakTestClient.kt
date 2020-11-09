package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity

internal class PleiepengerBarnUttakTestClient(private val restTemplate: TestRestTemplate) {

    private companion object {
        private const val UttaksplanPath = "/uttaksplan"
        private const val BehandlingId = "behandlingId"
    }

    internal fun opprettUttaksplan(grunnlag: Uttaksgrunnlag): ResponseEntity<Uttaksplan> {
        return restTemplate.exchange(UttaksplanPath, HttpMethod.POST, HttpEntity<Any>(grunnlag, HttpHeaders()), Uttaksplan::class.java)
    }

    internal fun hentUttaksplan(saksnummer: Saksnummer): ResponseEntity<Uttaksplaner> {
        return restTemplate.exchange(UttaksplanPath + "saksnummer=$saksnummer", HttpMethod.GET, HttpEntity<Any>(HttpHeaders()), Uttaksplaner::class.java)
    }

}

