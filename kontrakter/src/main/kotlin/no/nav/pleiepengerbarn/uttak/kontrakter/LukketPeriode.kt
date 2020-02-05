package no.nav.pleiepengerbarn.uttak.kontrakter

import java.lang.IllegalArgumentException
import java.time.LocalDate

data class LukketPeriode(
        val fom:LocalDate,
        val tom:LocalDate
) {
    init {
        if(fom.isAfter(tom)) {
            throw IllegalArgumentException("Fom må være før eller lik tom.")
        }
    }
}