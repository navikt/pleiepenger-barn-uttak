package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.Duration

data class Arbeid(
        val arbeidsforhold: Arbeidsforhold,
        val perioder: Map<LukketPeriode, ArbeidsforholdPeriodeInfo>
)

data class Arbeidsforhold(
        val type: String,
        val organisasjonsnummer: String? = null,
        val aktørId: String? = null,
        val arbeidsforholdId: String? = null
)

data class ArbeidsforholdPeriodeInfo(
        val jobberNormalt: Duration,
        val taptArbeidstid: Duration
) {
    init {
        require(taptArbeidstid <= jobberNormalt) {"Tapt arbeidstid ($taptArbeidstid) kan ikke være mer en jobber normalt ($jobberNormalt)."}
    }
}