package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

enum class RettVedDød(val lengde:Long, val enhet: TemporalUnit) {

    RETT_6_UKER(6, ChronoUnit.WEEKS),
    RETT_12_UKER(3, ChronoUnit.MONTHS)

}
