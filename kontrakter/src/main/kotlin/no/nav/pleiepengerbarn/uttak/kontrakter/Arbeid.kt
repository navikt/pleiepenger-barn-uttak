package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.Duration
import java.util.*

data class Arbeidsforhold(
        val arbeidstype:Arbeidstype,
        val organisasjonsnummer: Organisasjonsnummer? = null,
        val fødselsnummer: Fødselsnummer? = null,
        val arbeidsforholdId: UUID? = null
) {
    init {
        require(!(organisasjonsnummer == null && fødselsnummer == null)) { "Arbeidsgiver må ha enten organisasjonsnummer eller fødselsnummer" }
        require(!(organisasjonsnummer != null && fødselsnummer != null)) { "Arbeidsgiver kan ikke ha både organisasjonsnummer og fødselsnummer" }
        require(!(arbeidstype in listOf(Arbeidstype.FRILANS, Arbeidstype.SELVSTENDIG) && arbeidsforholdId!=null)) {"Bare arbeidsgiver kan ha arbeidsforholdId"}
    }
}

data class ArbeidsforholdOgArbeidsperioder(val arbeidsforhold: Arbeidsforhold, val perioder: Map<LukketPeriode, ArbeidInfo> = mapOf())


data class ArbeidInfo(
        val jobberNormalt: Duration,
        val skalJobbe: Prosent
) {
    fun arbeidsprosent(): Prosent {
        return skalJobbe
    }
}