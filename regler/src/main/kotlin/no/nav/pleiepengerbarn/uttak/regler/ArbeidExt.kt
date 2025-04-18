package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate

internal fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.finnSøkersTapteArbeidstid(
    skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper: Boolean,
    nyeReglerGjelder: Boolean
): Prosent {
    var sumJobberNå = Duration.ZERO
    var sumJobberNormalt = Duration.ZERO
    val oppdatertArbeid = if (skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper) {
        this.filter {
            Arbeidstype.values()
                .find { arbeidstype -> arbeidstype.kode == it.key.type } !in getGruppeSomSkalSpesialhåndteres(nyeReglerGjelder)
                    && (it.value.tilkommet != true || !nyeReglerGjelder)
        }
    } else {
        this.filter {
            it.value.tilkommet != true || !nyeReglerGjelder
        }
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

private fun ArbeidsforholdPeriodeInfo.ikkeFravær() = jobberNormalt <= jobberNå

internal fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.harSpesialhåndteringstilfelleForGamleRegler(brukNyeRegler: Boolean, periode: LukketPeriode, nyeReglerUtbetalingsgrad: LocalDate?): Boolean {
    val harSpesialhåndteringAktivitetstyper = any {
        Arbeidstype.values()
            .find { arbeidstype -> arbeidstype.kode == it.key.type } in getGruppeSomSkalSpesialhåndteres(brukNyeRegler)
    }
    val andreAktiviteter = filter {
        Arbeidstype.values()
            .find { arbeidstype -> arbeidstype.kode == it.key.type } !in getGruppeSomSkalSpesialhåndteres(brukNyeRegler)
    }
    val harBareFrilansUtenFravær = andreAktiviteter.isNotEmpty() && andreAktiviteter.all { Arbeidstype.FRILANSER.kode == it.key.type && it.value.ikkeFravær() }

    val nyeReglerGjelder = nyeReglerUtbetalingsgrad != null
            && !periode.fom.isBefore(nyeReglerUtbetalingsgrad)

    return harSpesialhåndteringAktivitetstyper && harBareFrilansUtenFravær && !nyeReglerGjelder
}
