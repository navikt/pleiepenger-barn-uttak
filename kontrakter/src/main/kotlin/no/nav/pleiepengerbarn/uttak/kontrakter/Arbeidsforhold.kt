package no.nav.pleiepengerbarn.uttak.kontrakter

import java.lang.IllegalArgumentException
import java.util.*

data class Arbeidsforhold(
        val arbeidstype:Arbeidstype,
        val organisasjonsnummer: Organisasjonsnummer? = null,
        val fødselsnummer: Fødselsnummer? = null,
        val arbeidsforholdId: UUID? = null
) {

    init {
        if (arbeidstype == Arbeidstype.ARBEIDSGIVER) {
            if (organisasjonsnummer == null && fødselsnummer == null) {
                throw IllegalArgumentException("Arbeidsgiver må ha enten organisasjonsnummer eller fødselsnummer")
            }
            if (organisasjonsnummer != null && fødselsnummer != null) {
                throw IllegalArgumentException("Arbeidsgiver kan ikke ha både organisasjonsnummer og fødselsnummer")
            }
        } else if (arbeidstype in listOf(Arbeidstype.FRILANS, Arbeidstype.SELVSTENDIG) && arbeidsforholdId!=null) {
            throw IllegalArgumentException("Bare arbeidsgiver kan ha arbeidsforholdId")
        }
    }

}

data class ArbeidsforholdV2(
        val organisasjonsnummer: Organisasjonsnummer? = null,
        val fødselsnummer: Fødselsnummer? = null,
        val perioder: Map<LukketPeriode, ArbeidsforholdPeriodeInfo>
) {
    init {
        require(!(organisasjonsnummer == null && fødselsnummer == null)) { "Arbeidsgiver må ha enten organisasjonsnummer eller fødselsnummer" }
        require(!(organisasjonsnummer != null && fødselsnummer != null)) { "Arbeidsgiver kan ikke ha både organisasjonsnummer og fødselsnummer" }
    }
}

data class ArbeidsforholdPeriodeInfo(
        val inntekt:Beløp,
        val arbeidsprosent: Prosent = Prosent.ZERO
)