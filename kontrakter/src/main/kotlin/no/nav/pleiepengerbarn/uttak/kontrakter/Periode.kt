package no.nav.pleiepengerbarn.uttak.kontrakter

import java.math.BigDecimal
import java.time.LocalDate

typealias Grad = BigDecimal

data class Periode(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate
)

data class PeriodeInnhold(
        val grad: Grad
)