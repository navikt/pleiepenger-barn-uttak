package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

internal fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.finnSøkersTapteArbeidstid(seBortFraIkkeYrkesaktiv: Boolean): Prosent {
    var sumJobberNå = Duration.ZERO
    var sumJobberNormalt = Duration.ZERO
    val oppdatertArbeid = if (seBortFraIkkeYrkesaktiv) {
        this.filter { it.key.type !in ARBEIDSTYPER_SOM_BARE_SKAL_TELLES_ALENE }
    } else {
        this
    }
    oppdatertArbeid.values.forEach {
        sumJobberNå += if (it.jobberNå > it.jobberNormalt) {
            it.jobberNormalt //Aldri tell mer enn max per dag
        } else {
            it.jobberNå
        }
        sumJobberNormalt += it.jobberNormalt
    }

    if (sumJobberNå > sumJobberNormalt) {
        sumJobberNå = sumJobberNormalt
    }

    if (sumJobberNormalt == Duration.ZERO) {
        return Prosent.ZERO
    }

    val søkersTapteArbeidstid = HUNDRE_PROSENT - (BigDecimal(sumJobberNå.toMillis()).setScale(8, RoundingMode.HALF_UP) / BigDecimal(sumJobberNormalt.toMillis()) * HUNDRE_PROSENT)

    if ( søkersTapteArbeidstid > HUNDRE_PROSENT) {
        throw IllegalStateException("Faktisk arbeid > normalt arbeid")
    }
    if (søkersTapteArbeidstid < Prosent.ZERO) {
        return Prosent.ZERO
    }
    return søkersTapteArbeidstid
}