package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.Duration

data class ArbeidInfo(
        val jobberNormalt: Duration,
        val skalJobbe: Prosent
) {
    fun arbeidsprosent(): Prosent {
        return skalJobbe
    }
}