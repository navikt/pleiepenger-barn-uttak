package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo
import no.nav.pleiepengerbarn.uttak.regler.domene.TaptArbeidstid
import java.time.Duration

internal fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.finnSøkersTapteArbeidstid(
    skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper: Boolean
): TaptArbeidstid {
    var sumJobberNå = Duration.ZERO
    var sumJobberNormalt = Duration.ZERO
    val oppdatertArbeid = if (skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper) {
        this.filter {
            Arbeidstype.values()
                .find { arbeidstype -> arbeidstype.kode == it.key.type } !in GRUPPE_SOM_SKAL_SPESIALHÅNDTERES
        }
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

    return TaptArbeidstid(sumJobberNå, sumJobberNormalt)
}

private fun ArbeidsforholdPeriodeInfo.ikkeFravær() = jobberNormalt <= jobberNå

internal fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.harSpesialhåndteringstilfelle(): Boolean {
    val harSpesialhåndteringAktivitetstyper = any {
        Arbeidstype.values()
            .find { arbeidstype -> arbeidstype.kode == it.key.type } in GRUPPE_SOM_SKAL_SPESIALHÅNDTERES
    }
    val andreAktiviteter = filter {
        Arbeidstype.values()
            .find { arbeidstype -> arbeidstype.kode == it.key.type } !in GRUPPE_SOM_SKAL_SPESIALHÅNDTERES
    }
    val harBareFrilansUtenFravær = andreAktiviteter.isNotEmpty() && andreAktiviteter.all { Arbeidstype.FRILANSER.kode == it.key.type && it.value.ikkeFravær() }

    return harSpesialhåndteringAktivitetstyper && harBareFrilansUtenFravær
}
