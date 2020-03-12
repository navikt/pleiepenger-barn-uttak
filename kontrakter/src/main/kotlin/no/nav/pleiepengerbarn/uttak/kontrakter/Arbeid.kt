package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.Duration

typealias Arbeid = List<Arbeidsforhold>

data class Arbeidsforhold(
        val arbeidsforhold: ArbeidsforholdReferanse,
        val perioder: Map<LukketPeriode, ArbeidsforholdPeriodeInfo>
)

data class ArbeidsforholdReferanse(
        val type: String? = null,
        val organisasjonsnummer: String? = null,
        val aktørId: String? = null,
        val arbeidsforholdId: String? = null
)

data class ArbeidsforholdPeriodeInfo(
        val jobberNormaltPerUke: Duration,
        val skalJobbeProsent: Prosent
)