package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.LocalDate

data class Søker(
        val fødselsdato: LocalDate,
        val dødsdato: LocalDate? = null
)