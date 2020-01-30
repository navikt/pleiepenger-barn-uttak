package no.nav.pleiepengerbarn.uttak.server

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PingApi {
    @GetMapping("/ping")
    fun ping() : ResponseEntity<String> {
        return ResponseEntity
                .ok()
                .headers { it.contentType = MediaType.APPLICATION_JSON }
                .body("""
                    {
                        "pong": true
                    }
                """.trimIndent())
    }
}