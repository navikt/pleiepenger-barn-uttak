package no.nav.pleiepengerbarn.uttak.kontrakter

import java.util.*

data class Arbeidsforhold(
        val arbeidstype:Arbeidstype,
        val organisasjonsnummer: Organisasjonsnummer? = null,
        val fødselsnummer: Fødselsnummer? = null,
        val arbeidsforholdId: UUID? = null,
        val perioder: Map<LukketPeriode, ArbeidsforholdPeriodeInfo> = mapOf()
) {
    init {
        require(!(organisasjonsnummer == null && fødselsnummer == null)) { "Arbeidsgiver må ha enten organisasjonsnummer eller fødselsnummer" }
        require(!(organisasjonsnummer != null && fødselsnummer != null)) { "Arbeidsgiver kan ikke ha både organisasjonsnummer og fødselsnummer" }
        require(!(arbeidstype in listOf(Arbeidstype.FRILANS, Arbeidstype.SELVSTENDIG) && arbeidsforholdId!=null)) {"Bare arbeidsgiver kan ha arbeidsforholdId"}
    }
}

data class ArbeidsforholdPeriodeInfo(
        val inntekt:Beløp,
        val arbeidsprosent: Prosent = Prosent.ZERO
)