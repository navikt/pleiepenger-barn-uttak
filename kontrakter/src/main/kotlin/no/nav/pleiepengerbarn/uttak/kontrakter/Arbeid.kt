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
        val taptArbeidstid: Duration,
        val søkersTilsyn: Duration = Duration.ofHours(7).plusMinutes(30) //TODO: fjern default verdi når ny beregning er tatt i bruk
) {
    init {
        require(taptArbeidstid <= jobberNormalt) {"Tapt arbeidstid ($taptArbeidstid) kan ikke være mer en jobber normalt ($jobberNormalt)."}
        require(søkersTilsyn <= Duration.ofHours(7).plusMinutes(30)) {"Søkers tilsyn ($søkersTilsyn) kan ikke være mer enn 7 timer og 30 minutter."}
    }
}