package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.Duration

data class ArbeidInfo(
        val jobberNormaltPerUke: Duration,
        val skalJobbeProsent: Prosent
)