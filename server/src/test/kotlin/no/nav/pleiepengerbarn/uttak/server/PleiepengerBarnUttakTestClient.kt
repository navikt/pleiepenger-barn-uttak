package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity

internal class PleiepengerBarnUttakTestClient(private val restTemplate: TestRestTemplate) {

    internal fun opprettUttaksplan(grunnlag: Uttaksgrunnlag): ResponseEntity<Uttaksplan> {
        return restTemplate.exchange(UttakplanApi.UttaksplanPath, HttpMethod.POST, HttpEntity<Any>(grunnlag, HttpHeaders()), Uttaksplan::class.java)
    }

    internal fun hentUttaksplan(saksnummer: Saksnummer): ResponseEntity<Uttaksplan> {
        return restTemplate.exchange(UttakplanApi.FullUttaksplanPath + "?saksnummer=$saksnummer", HttpMethod.GET, HttpEntity<Any>(HttpHeaders()), Uttaksplan::class.java)
    }




}
