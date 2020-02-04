package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.LocalDate

data class LukketPeriode(
        val fom:LocalDate,
        val tom:LocalDate
)