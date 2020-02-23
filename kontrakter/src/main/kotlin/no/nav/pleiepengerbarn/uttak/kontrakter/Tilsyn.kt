package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.Duration

data class Tilsyn(val periode:LukketPeriode, val grad:Prosent)

data class TilsynPeriodeInfo(
        val lengde: Duration
)