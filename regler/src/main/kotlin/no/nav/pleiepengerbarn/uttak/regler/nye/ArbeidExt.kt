package no.nav.pleiepengerbarn.uttak.regler.nye

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

internal fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.finnSøkersTapteArbeidstid(
): Prosent {
    var sumJobberNå = Duration.ZERO
    var sumJobberNormalt = Duration.ZERO
    val oppdatertArbeid = this.filter {
            it.value.tilkommet != true
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

    val søkersTapteArbeidstid =
        HUNDRE_PROSENT - (BigDecimal(sumJobberNå.toMillis()).setScale(8, RoundingMode.HALF_UP).divide(BigDecimal(
            sumJobberNormalt.toMillis()), 8, RoundingMode.HALF_UP
        ) * HUNDRE_PROSENT)

    if (søkersTapteArbeidstid > HUNDRE_PROSENT) {
        throw IllegalStateException("Faktisk arbeid > normalt arbeid")
    }
    if (søkersTapteArbeidstid < Prosent.ZERO) {
        return Prosent.ZERO
    }
    return søkersTapteArbeidstid
}
