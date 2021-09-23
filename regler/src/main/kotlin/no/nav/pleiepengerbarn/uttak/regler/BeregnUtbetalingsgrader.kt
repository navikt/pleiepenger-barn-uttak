package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.regler.domene.Utbetalingsgrad
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

enum class Arbeidstype(val kode: String) {
    ARBEIDSTAKER("AT"),
    FRILANSER("FL"),
    DAGPENGER("DP"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SN"),
    IKKE_YRKESAKTIV("IKKE_YRKESAKTIV"),
    KUN_YTELSE("BA"),
    INAKTIV("MIDL_INAKTIV")
}

private val AKTIVITETS_GRUPPER = listOf(
    setOf(Arbeidstype.ARBEIDSTAKER),
    setOf(Arbeidstype.FRILANSER),
    setOf(Arbeidstype.DAGPENGER),
    setOf(Arbeidstype.SELVSTENDIG_NÆRINGSDRIVENDE),
    setOf(
        Arbeidstype.IKKE_YRKESAKTIV,
        Arbeidstype.KUN_YTELSE,
        Arbeidstype.INAKTIV
    )
)

object BeregnUtbetalingsgrader {

    internal fun beregn(uttaksgrad: Prosent, arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>): Map<Arbeidsforhold, Utbetalingsgrad> {
        arbeid.sjekkAtArbeidsforholdFinnesBlandtAktivitetsgrupper()


        var sumJobberNormalt = Duration.ZERO
        arbeid.values.forEach {
            sumJobberNormalt += it.jobberNormalt
        }

        val timerSomDekkes = sumJobberNormalt.prosent(uttaksgrad)

        var gjenværendeTimerSomDekkes = timerSomDekkes

        val alleUtbetalingsgrader = mutableMapOf<Arbeidsforhold, Utbetalingsgrad>()
        AKTIVITETS_GRUPPER.forEach { aktivitetsgruppe ->
            val arbeidForAktivitetsgruppe = arbeid.forAktivitetsgruppe(aktivitetsgruppe)
            val fordeling = finnFordeling(arbeidForAktivitetsgruppe)
            val utbetalingsgraderOgGjenværendeTimerSomDekkes = beregnForAktivitetsGruppe(gjenværendeTimerSomDekkes, arbeidForAktivitetsgruppe, fordeling)
            gjenværendeTimerSomDekkes = utbetalingsgraderOgGjenværendeTimerSomDekkes.gjenværendeTimerSomDekkes
            alleUtbetalingsgrader.putAll(utbetalingsgraderOgGjenværendeTimerSomDekkes.utbetalingsgrad)
        }
        return alleUtbetalingsgrader
    }

    private fun beregnForAktivitetsGruppe(taptArbeidstidSomDekkes: Duration, arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>, fordeling: Map<Arbeidsforhold, Prosent>): UtbetalingsgraderOgGjenværendeTimerSomDekkes {
        val utbetalingsgrader = mutableMapOf<Arbeidsforhold, Utbetalingsgrad>()
        var sumTimerForbrukt = Duration.ZERO
        arbeid.forEach { (arbeidsforhold, info) ->
            val fordelingsprosent = fordeling[arbeidsforhold]
                ?: throw IllegalStateException("Dette skal ikke skje. Finner ikke fordeling for $arbeidsforhold.")
            if (info.jobberNormalt > Duration.ZERO) {
                val timerForbrukt = taptArbeidstidSomDekkes.prosent(fordelingsprosent)
                val utbetalingsgrad = BigDecimal(timerForbrukt.toMillis()).setScale(2, RoundingMode.HALF_UP) / BigDecimal(info.jobberNormalt.toMillis()) * HUNDRE_PROSENT
                utbetalingsgrader[arbeidsforhold] = Utbetalingsgrad(utbetalingsgrad = utbetalingsgrad, normalArbeidstid = info.jobberNormalt, faktiskArbeidstid = info.jobberNå)
                sumTimerForbrukt += timerForbrukt
            }
        }
        return UtbetalingsgraderOgGjenværendeTimerSomDekkes(utbetalingsgrader, taptArbeidstidSomDekkes - sumTimerForbrukt)
    }

    private fun finnFordeling(arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>): Map<Arbeidsforhold, Prosent> {
        var sumTapt = Duration.ZERO
        arbeid.values.forEach {
            sumTapt += (it.jobberNormalt - it.jobberNå)
        }
        val fordeling = mutableMapOf<Arbeidsforhold, Prosent>()

        arbeid.forEach {
            if (sumTapt != Duration.ZERO) {
                val tapt = it.value.jobberNormalt - it.value.jobberNå
                fordeling[it.key] = ((BigDecimal(tapt.toMillis()).setScale(8, RoundingMode.HALF_UP)/BigDecimal(sumTapt.toMillis())) * HUNDRE_PROSENT).setScale(2, RoundingMode.HALF_UP)
            } else {
                fordeling[it.key] = Prosent.ZERO
            }
        }

        return fordeling
    }


}

private fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.forAktivitetsgruppe(aktivitetsgruppe: Set<Arbeidstype>): Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo> {
    val aktivitetsgruppeKoder = aktivitetsgruppe.map { it.kode }
    val arbeidForAktivitetsgruppe = mutableMapOf<Arbeidsforhold, ArbeidsforholdPeriodeInfo>()
    this.forEach { (arbeidsforhold, arbeidsforholdInfo) ->
        if (arbeidsforhold.type in aktivitetsgruppeKoder) {
            arbeidForAktivitetsgruppe[arbeidsforhold] = arbeidsforholdInfo
        }
    }
    return arbeidForAktivitetsgruppe
}

private fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.sjekkAtArbeidsforholdFinnesBlandtAktivitetsgrupper() {
    val lovligeArbeidstyper = AKTIVITETS_GRUPPER.flatten().map { it.kode} .toSet()
    this.keys.forEach {
        if (!lovligeArbeidstyper.contains(it.type)) {
            throw IllegalArgumentException("Ulovlig arbeidstype ${it.type}")
        }
    }
}

private data class UtbetalingsgraderOgGjenværendeTimerSomDekkes(
    val utbetalingsgrad: Map<Arbeidsforhold, Utbetalingsgrad>,
    val gjenværendeTimerSomDekkes: Duration
)