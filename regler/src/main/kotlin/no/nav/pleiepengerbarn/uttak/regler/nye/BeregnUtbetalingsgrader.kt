package no.nav.pleiepengerbarn.uttak.regler.nye

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.BeregnGraderGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.FeatureToggle
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.domene.Utbetalingsgrad
import no.nav.pleiepengerbarn.uttak.regler.prosent
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
    Arbeidstype.IKKE_YRKESAKTIV_UTEN_ERSTATNING,
    Arbeidstype.KUN_YTELSE
)
private val AKTIVITETS_GRUPPER = listOf(
    setOf(
        Arbeidstype.ARBEIDSTAKER,
        Arbeidstype.IKKE_YRKESAKTIV,
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
        overstyrtUttaksgrad: Prosent? = null,
        gradertMotTilsyn: Boolean,
        beregnGraderGrunnlag: BeregnGraderGrunnlag
    ): Map<Arbeidsforhold, Utbetalingsgrad> {
        beregnGraderGrunnlag.arbeid.sjekkAtArbeidsforholdFinnesBlandtAktivitetsgrupper()

        // Timer som jobbes normalt
        var sumJobberNormalt = finnTotalNormalarbeidstid(beregnGraderGrunnlag)

        val faktiskUttaksgrad = overstyrtUttaksgrad ?: uttaksgrad

        // Finner timer som dekkes av normal arbeidstid basert på uttaksgrad, påvirkes av overstyring dersom skalUttaksgradOverstyreTimerDekket er true
        val timerSomDekkes = finnTimerSomDekkes(
            sumJobberNormalt,
            uttaksgrad,
            overstyrtUttaksgrad,
            beregnGraderGrunnlag.overstyrtInput?.skalUttaksgradOverstyreTimerDekket
        )

        // En variabel å holde på antall gjenværende timer, starter med alle som dekkes og oppdateres løpende
        var gjenværendeTimerSomDekkes = timerSomDekkes
        // Map for holde på utbetalingsgrader
        val alleUtbetalingsgrader = mutableMapOf<Arbeidsforhold, Utbetalingsgrad>()
        // Beregner utbetalingsgrad gruppevis
        AKTIVITETS_GRUPPER.forEach { aktivitetsgruppe ->
            val arbeidForAktivitetsgruppe = beregnGraderGrunnlag.arbeid.forAktivitetsgruppe(aktivitetsgruppe)
            // HOVEDLØYPE FOR AKTIVITETER
            // Finner prosentvis fordeling av gjenværende timer som dekkes innenfor gruppe
            val fordeling = finnFordeling(arbeidForAktivitetsgruppe)

            // Finner utbetalingsgrad basert på fordeling. En aktivitet kan aldri få dekket mer enn sin tapte arbeidstid, unntaket er overstyring
            val utbetalingsgraderOgGjenværendeTimerSomDekkes = beregnForAktivitetsGruppe(
                gjenværendeTimerSomDekkes,
                arbeidForAktivitetsgruppe,
                fordeling,
                beregnGraderGrunnlag.overstyrtInput,
            )
            // Gjenværende timer som dekkes oppdateres med det som enda ikke er fordelt
            gjenværendeTimerSomDekkes = utbetalingsgraderOgGjenværendeTimerSomDekkes.gjenværendeTimerSomDekkes
            alleUtbetalingsgrader.putAll(utbetalingsgraderOgGjenværendeTimerSomDekkes.utbetalingsgrad)
        }
        return alleUtbetalingsgrader
    }

    private fun finnTotalNormalarbeidstid(
        beregnGraderGrunnlag: BeregnGraderGrunnlag,
    ): Duration {
        var sumJobberNormalt1 = Duration.ZERO
        beregnGraderGrunnlag.arbeid.entries.filter  {
            it.value.tilkommet != true
        }.forEach {
            sumJobberNormalt1 += it.value.jobberNormalt
        }
        return sumJobberNormalt1
    }

    private fun utledGradForOverstyrte(
        arbeidsforhold: Arbeidsforhold,
        info: ArbeidsforholdPeriodeInfo,
        overstyrtInput: OverstyrtInput
    ): Utbetalingsgrad {
        val overstyrtUtbetalingsgradPåArbeidsforhold: OverstyrtUtbetalingsgradPerArbeidsforhold? =
            overstyrtInput.overstyrtUtbetalingsgradPerArbeidsforhold.find {
                it.arbeidsforhold == arbeidsforhold
            }
        return Utbetalingsgrad(
            utbetalingsgrad = overstyrtUtbetalingsgradPåArbeidsforhold!!.overstyrtUtbetalingsgrad,
            normalArbeidstid = info.jobberNormalt,
            faktiskArbeidstid = info.jobberNå,
            tilkommet = info.tilkommet,
            overstyrt = true
        )
    }

    private fun beregnForAktivitetsGruppe(
        taptArbeidstidSomDekkes: Duration,
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>,
        fordeling: Map<Arbeidsforhold, Prosent>,
        overstyrtInput: OverstyrtInput?,
    ): UtbetalingsgraderOgGjenværendeTimerSomDekkes {
        // Init verdier for løkke
        val utbetalingsgrader = mutableMapOf<Arbeidsforhold, Utbetalingsgrad>()
        var sumTimerForbrukt = Duration.ZERO

        // For hvert arbeidsforhold/aktivitet, setter utbetalingsgrad basert på tid som dekkes og fordeling
        arbeid.forEach { (arbeidsforhold, info) ->

            // Dersom vi har en overstyring brukes denne
            if (overstyrtInput != null && overstyrtInput.overstyrtUtbetalingsgradPerArbeidsforhold.any { it.arbeidsforhold == arbeidsforhold }) {
                utbetalingsgrader[arbeidsforhold] = utledGradForOverstyrte(arbeidsforhold, info, overstyrtInput)
            }
            // Arbeidsforhold uten normalarbeidstid ignoreres
            else if (info.jobberNormalt > Duration.ZERO) {
                // Finner prosentandel for aktivitet
                val fordelingsprosent = fordeling[arbeidsforhold]
                    ?: throw IllegalStateException("Dette skal ikke skje. Finner ikke fordeling for $arbeidsforhold.")

                // Finner det minste av sin del av tapt tid som dekkes og faktisk tapt tid (normal - faktisk)
                val timerForbrukt = min(
                    taptArbeidstidSomDekkes.prosent(fordelingsprosent),
                    info.taptArbeid()
                )

                // Regner ut utbetalingsgrad utifra oppgitt normalarbeidstid
                val utbetalingsgrad = BigDecimal(timerForbrukt.toMillis()).setScale(2, RoundingMode.HALF_UP)
                    .divide(BigDecimal(info.jobberNormalt.toMillis()), 2, RoundingMode.HALF_UP) * HUNDRE_PROSENT

                utbetalingsgrader[arbeidsforhold] = Utbetalingsgrad(
                    utbetalingsgrad = utbetalingsgrad,
                    normalArbeidstid = info.jobberNormalt,
                    faktiskArbeidstid = info.jobberNå,
                    tilkommet = info.tilkommet,
                    overstyrt = false
                )

                // trekker antall timer som dekkes for aktivitet fra restverdi som dekkes
                sumTimerForbrukt += timerForbrukt
            }
            // TODO: Fjern denne delen
            else if (FeatureToggle.isActive("INKLUDER_TILKOMMET_UTEN_ARBEIDSTID") && info.tilkommet == true) {
                utbetalingsgrader[arbeidsforhold] = Utbetalingsgrad(
                    utbetalingsgrad = Prosent.ZERO,
                    normalArbeidstid = Duration.ZERO,
                    faktiskArbeidstid = Duration.ZERO,
                    tilkommet = true,
                    overstyrt = false
                )
            }
        }
        return UtbetalingsgraderOgGjenværendeTimerSomDekkes(
            utbetalingsgrader,
            taptArbeidstidSomDekkes - sumTimerForbrukt
        )
    }

    /** Finner timer som dekkes
     * Dersom uttaksgraden er satt til å endre timer som dekkes vil timer som dekkes være prosenten av normalarbeidstiden basert på den overstyrte graden, ellers brukes den ikke-overstyrte uttaksgraden.
     */
    private fun finnTimerSomDekkes(
        sumJobberNormalt: Duration,
        uttaksgrad: Prosent,
        overstyrtUttaksgrad: Prosent?,
        skalUttaksgradEndreTimerDekket: Boolean?
    ) =
        if (skalUttaksgradEndreTimerDekket == false || overstyrtUttaksgrad == null) sumJobberNormalt.prosent(uttaksgrad) else sumJobberNormalt.prosent(
            overstyrtUttaksgrad
        )

    private fun min(duration1: Duration, duration2: Duration) = if (duration1 < duration2) duration1 else duration2

    private fun finnFordeling(
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>,
    ): Map<Arbeidsforhold, Prosent> {
        var sumTapt = Duration.ZERO
        arbeid.values.forEach {
            sumTapt += it.taptArbeid()
        }
        val fordeling = mutableMapOf<Arbeidsforhold, Prosent>()

        arbeid.forEach {
            if (sumTapt != Duration.ZERO) {
                val tapt = it.value.taptArbeid()
                fordeling[it.key] = ((BigDecimal(tapt.toMillis()).setScale(8, RoundingMode.HALF_UP)
                    .divide(BigDecimal(sumTapt.toMillis()), 8, RoundingMode.HALF_UP)) * HUNDRE_PROSENT).setScale(
                    2,
                    RoundingMode.HALF_UP
                )
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
    if (tilkommet == true) {
        return Duration.ZERO
    }
    if (jobberNå > jobberNormalt) {
        return Duration.ZERO
    }
    return jobberNormalt - jobberNå
}


private data class UtbetalingsgraderOgGjenværendeTimerSomDekkes(
    val utbetalingsgrad: Map<Arbeidsforhold, Utbetalingsgrad>,
    val gjenværendeTimerSomDekkes: Duration
)
