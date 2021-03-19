package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.time.Duration

internal fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.finnSøkersTapteArbeidstid(): Prosent {
    var sumJobberNå = Duration.ZERO
    var sumJobberNormalt = Duration.ZERO
    this.values.forEach {
        sumJobberNå += it.jobberNå
        sumJobberNormalt += it.jobberNormalt
    }

    if (sumJobberNormalt == Duration.ZERO) {
        return Prosent.ZERO
    }

    val søkersTapteArbeidstid = HUNDRE_PROSENT - (BigDecimal(sumJobberNå.toMillis()).setScale(8) / BigDecimal(sumJobberNormalt.toMillis()) * HUNDRE_PROSENT)

    if ( søkersTapteArbeidstid > HUNDRE_PROSENT) {
        throw IllegalStateException("Faktisk arbeid > normalt arbeid")
    }
    if (søkersTapteArbeidstid < Prosent.ZERO) {
        throw IllegalStateException("Negativ gradering mot inntektstap ($søkersTapteArbeidstid)")
    }
    return søkersTapteArbeidstid
}