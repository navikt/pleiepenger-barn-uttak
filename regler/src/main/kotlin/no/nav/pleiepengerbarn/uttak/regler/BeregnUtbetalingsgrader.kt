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
    IKKE_YRKESAKTIV_UTEN_ERSTATNING("IKKE_YRKESAKTIV_UTEN_ERSTATNING"),
    KUN_YTELSE("BA"),
    INAKTIV("MIDL_INAKTIV"),
    SYKEPENGER_AV_DAGPENGER("SP_AV_DP"),
    PSB_AV_DP("PSB_AV_DP")
}

val GRUPPE_SOM_SKAL_SPESIALHÅNDTERES = setOf(
    Arbeidstype.IKKE_YRKESAKTIV,
    Arbeidstype.IKKE_YRKESAKTIV_UTEN_ERSTATNING,
    Arbeidstype.KUN_YTELSE
)
private val AKTIVITETS_GRUPPER = listOf(
    setOf(
        Arbeidstype.ARBEIDSTAKER,
        Arbeidstype.FRILANSER,
        Arbeidstype.SELVSTENDIG_NÆRINGSDRIVENDE
    ),
    setOf(
        Arbeidstype.SYKEPENGER_AV_DAGPENGER,
        Arbeidstype.PSB_AV_DP,
        Arbeidstype.DAGPENGER,
        Arbeidstype.INAKTIV
    ),
    GRUPPE_SOM_SKAL_SPESIALHÅNDTERES
)

object BeregnUtbetalingsgrader {

    internal fun beregn(
        uttaksgrad: Prosent,
        gradertMotTilsyn: Boolean,
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>
    ): Map<Arbeidsforhold, Utbetalingsgrad> {
        arbeid.sjekkAtArbeidsforholdFinnesBlandtAktivitetsgrupper()

        var sumJobberNormalt = Duration.ZERO
        arbeid.entries.filter {
            !GRUPPE_SOM_SKAL_SPESIALHÅNDTERES.contains(
                Arbeidstype.values().find { arbeidstype -> arbeidstype.kode == it.key.type })
        }.forEach {
            sumJobberNormalt += it.value.jobberNormalt
        }

        val timerSomDekkes = sumJobberNormalt.prosent(uttaksgrad)

        var gjenværendeTimerSomDekkes = timerSomDekkes

        val spesialhåndteringsgruppeSkalSpesialhåndteres = arbeid.harSpesialhåndteringstilfelle()

        val alleUtbetalingsgrader = mutableMapOf<Arbeidsforhold, Utbetalingsgrad>()
        AKTIVITETS_GRUPPER.forEach { aktivitetsgruppe ->
            val arbeidForAktivitetsgruppe = arbeid.forAktivitetsgruppe(aktivitetsgruppe)
            if (aktivitetsgruppe == GRUPPE_SOM_SKAL_SPESIALHÅNDTERES) {
                val utbetalingsgraderForSpesialhåndtering =
                    beregnForSpesialhåndtertGruppe(
                        arbeidForAktivitetsgruppe,
                        gjenværendeTimerSomDekkes,
                        uttaksgrad,
                        gradertMotTilsyn,
                        spesialhåndteringsgruppeSkalSpesialhåndteres
                    )
                alleUtbetalingsgrader.putAll(utbetalingsgraderForSpesialhåndtering.utbetalingsgrad)
            } else {
                val fordeling = finnFordeling(arbeidForAktivitetsgruppe)
                val utbetalingsgraderOgGjenværendeTimerSomDekkes = beregnForAktivitetsGruppe(
                    gjenværendeTimerSomDekkes,
                    arbeidForAktivitetsgruppe,
                    fordeling
                )
                gjenværendeTimerSomDekkes = utbetalingsgraderOgGjenværendeTimerSomDekkes.gjenværendeTimerSomDekkes
                alleUtbetalingsgrader.putAll(utbetalingsgraderOgGjenværendeTimerSomDekkes.utbetalingsgrad)
            }
        }
        return alleUtbetalingsgrader
    }

    private fun beregnForSpesialhåndtertGruppe(
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>,
        gjenværendeTimerSomDekkes: Duration,
        uttaksgrad: Prosent,
        gradertMotTilsyn: Boolean,
        spesialhåndteringsgruppeSkalSpesialhåndteres: Boolean
    ): UtbetalingsgraderOgGjenværendeTimerSomDekkes {
        val utbetalingsgrader = mutableMapOf<Arbeidsforhold, Utbetalingsgrad>()
        arbeid.forEach { (arbeidsforhold, info) ->
            utbetalingsgrader[arbeidsforhold] = Utbetalingsgrad(
                utbetalingsgrad = utledGrad(uttaksgrad, gradertMotTilsyn, spesialhåndteringsgruppeSkalSpesialhåndteres, arbeidsforhold.type),
                normalArbeidstid = info.jobberNormalt,
                faktiskArbeidstid = info.jobberNå
            )
        }
        return UtbetalingsgraderOgGjenværendeTimerSomDekkes(
            utbetalingsgrader,
            gjenværendeTimerSomDekkes
        )
    }

    private fun utledGrad(
        uttaksgrad: Prosent,
        gradertMotTilsyn: Boolean,
        spesialhåndteringsgruppeSkalSpesialhåndteres: Boolean,
        type: String
    ): Prosent {
        return if (spesialhåndteringsgruppeSkalSpesialhåndteres && !gradertMotTilsyn && uttaksgrad > Prosent.ZERO) {
            HUNDRE_PROSENT
        } else if(type == Arbeidstype.IKKE_YRKESAKTIV_UTEN_ERSTATNING.kode) {
            HUNDRE_PROSENT
        } else {
            uttaksgrad
        }
    }

    private fun beregnForAktivitetsGruppe(
        taptArbeidstidSomDekkes: Duration,
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>,
        fordeling: Map<Arbeidsforhold, Prosent>
    ): UtbetalingsgraderOgGjenværendeTimerSomDekkes {
        val utbetalingsgrader = mutableMapOf<Arbeidsforhold, Utbetalingsgrad>()
        var sumTimerForbrukt = Duration.ZERO
        arbeid.forEach { (arbeidsforhold, info) ->
            val fordelingsprosent = fordeling[arbeidsforhold]
                ?: throw IllegalStateException("Dette skal ikke skje. Finner ikke fordeling for $arbeidsforhold.")
            if (info.jobberNormalt > Duration.ZERO) {
                val timerForbrukt = min(
                    taptArbeidstidSomDekkes.prosent(fordelingsprosent),
                    info.taptArbeid()
                )
                val utbetalingsgrad = BigDecimal(timerForbrukt.toMillis()).setScale(2, RoundingMode.HALF_UP)
                        .divide(BigDecimal(info.jobberNormalt.toMillis()), 2, RoundingMode.HALF_UP) * HUNDRE_PROSENT
                utbetalingsgrader[arbeidsforhold] = Utbetalingsgrad(
                    utbetalingsgrad = utbetalingsgrad,
                    normalArbeidstid = info.jobberNormalt,
                    faktiskArbeidstid = info.jobberNå
                )
                sumTimerForbrukt += timerForbrukt
            }
        }
        return UtbetalingsgraderOgGjenværendeTimerSomDekkes(
            utbetalingsgrader,
            taptArbeidstidSomDekkes - sumTimerForbrukt
        )
    }

    private fun min(duration1: Duration, duration2: Duration) = if (duration1 < duration2) duration1 else duration2

    private fun finnFordeling(arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>): Map<Arbeidsforhold, Prosent> {
        var sumTapt = Duration.ZERO
        arbeid.values.forEach {
            sumTapt += it.taptArbeid()
        }
        val fordeling = mutableMapOf<Arbeidsforhold, Prosent>()

        arbeid.forEach {
            if (sumTapt != Duration.ZERO) {
                val tapt = it.value.taptArbeid()
                fordeling[it.key] = ((BigDecimal(tapt.toMillis()).setScale(8, RoundingMode.HALF_UP)
                        .divide(BigDecimal(sumTapt.toMillis()), 8, RoundingMode.HALF_UP)) * HUNDRE_PROSENT).setScale(2, RoundingMode.HALF_UP)
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
    val lovligeArbeidstyper = AKTIVITETS_GRUPPER.flatten().map { it.kode }.toSet()
    this.keys.forEach {
        if (!lovligeArbeidstyper.contains(it.type)) {
            throw IllegalArgumentException("Ulovlig arbeidstype ${it.type}")
        }
    }
}

private fun ArbeidsforholdPeriodeInfo.taptArbeid(): Duration {
    if (jobberNå > jobberNormalt) {
        return Duration.ZERO
    }
    return jobberNormalt - jobberNå
}


private data class UtbetalingsgraderOgGjenværendeTimerSomDekkes(
    val utbetalingsgrad: Map<Arbeidsforhold, Utbetalingsgrad>,
    val gjenværendeTimerSomDekkes: Duration
)
