package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.LocalDate

data class Uttaksperiode(val fom:LocalDate, val tom: LocalDate, var uttaksperiodeResultat: UttaksperiodeResultat? = null)