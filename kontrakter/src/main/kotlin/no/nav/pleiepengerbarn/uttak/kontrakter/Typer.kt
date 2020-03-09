package no.nav.pleiepengerbarn.uttak.kontrakter

import java.math.BigDecimal

typealias Saksnummer = String
typealias BehandlingId = String
typealias ArbeidsforholdRef = String
typealias Arbeid = Map<ArbeidsforholdRef, Map<LukketPeriode, ArbeidInfo>>
typealias Prosent = BigDecimal