package no.nav.pleiepengerbarn.uttak.kontrakter

import java.math.BigDecimal

typealias Arbeidsprosent = BigDecimal

data class Arbeid(val arbeidsforhold: Arbeidsforhold, val lukketPeriode: LukketPeriode, val grad: Arbeidsprosent)