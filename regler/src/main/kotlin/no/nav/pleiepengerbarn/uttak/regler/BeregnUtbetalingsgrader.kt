package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.OverstyrtInput
import no.nav.pleiepengerbarn.uttak.kontrakter.OverstyrtUtbetalingsgradPerArbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.regler.domene.Utbetalingsgrad
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

object BeregnUtbetalingsgrader {

    internal fun beregn(
        uttaksgrad: Prosent,
        overstyrtUttaksgrad: Prosent? = null,
        gradertMotTilsyn: Boolean,
        beregnGraderGrunnlag: BeregnGraderGrunnlag
    ): Map<Arbeidsforhold, Utbetalingsgrad> {
        val brukNyeRegler = gjelderNyeRegler(beregnGraderGrunnlag)
        beregnGraderGrunnlag.arbeid.sjekkAtArbeidsforholdFinnesBlandtAktivitetsgrupper(brukNyeRegler)

        // Timer som jobbes normalt
        var sumJobberNormalt = finnTotalNormalarbeidstid(beregnGraderGrunnlag, brukNyeRegler)

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
        getAktivitetsgruppe(brukNyeRegler).forEach { aktivitetsgruppe ->
            val arbeidForAktivitetsgruppe = beregnGraderGrunnlag.arbeid.forAktivitetsgruppe(aktivitetsgruppe)
            if (!gjelderSpesialgruppePåGamleRegler(aktivitetsgruppe, brukNyeRegler)) {
                // HOVEDLØYPE FOR AKTIVITETER
                // Finner prosentvis fordeling av gjenværende timer som dekkes innenfor gruppe
                val fordeling = finnFordeling(arbeidForAktivitetsgruppe, brukNyeRegler)

                // Finner utbetalingsgrad basert på fordeling. En aktivitet kan aldri få dekket mer enn sin tapte arbeidstid, unntaket er overstyring
                val utbetalingsgraderOgGjenværendeTimerSomDekkes = beregnForAktivitetsGruppe(
                    gjenværendeTimerSomDekkes,
                    arbeidForAktivitetsgruppe,
                    fordeling,
                    beregnGraderGrunnlag.overstyrtInput,
                    brukNyeRegler
                )
                // Gjenværende timer som dekkes oppdateres med det som enda ikke er fordelt
                gjenværendeTimerSomDekkes = utbetalingsgraderOgGjenværendeTimerSomDekkes.gjenværendeTimerSomDekkes
                alleUtbetalingsgrader.putAll(utbetalingsgraderOgGjenværendeTimerSomDekkes.utbetalingsgrad)
            } else if (gjelderSpesialgruppePåGamleRegler(aktivitetsgruppe, brukNyeRegler)) {
                // GAMLE REGLER + spesialhåndteringsgruppe
                // Finner ut om vi skal har kombinasjonen IKKE_YREKSAKTIV/KUN_YTELSE og Frilans uten fravær på gamle regler
                val spesialhåndteringsgruppeSkalSpesialhåndteres =
                    beregnGraderGrunnlag.arbeid.harSpesialhåndteringstilfelleForGamleRegler(
                        brukNyeRegler,
                        beregnGraderGrunnlag.periode,
                        beregnGraderGrunnlag.virkningstidspunktRegelNyEllerBortfaltAktivitet
                    )
                // Finner utbetalingsgrad for spesialgruppe
                val utbetalingsgraderForSpesialhåndtering =
                    beregnForSpesialhåndtertGruppeMedGamleRegler(
                        arbeidForAktivitetsgruppe,
                        gjenværendeTimerSomDekkes,
                        faktiskUttaksgrad,
                        gradertMotTilsyn,
                        spesialhåndteringsgruppeSkalSpesialhåndteres,
                        beregnGraderGrunnlag
                    )
                alleUtbetalingsgrader.putAll(utbetalingsgraderForSpesialhåndtering.utbetalingsgrad)
            }
        }
        return alleUtbetalingsgrader
    }

    private fun gjelderSpesialgruppePåGamleRegler(
        aktivitetsgruppe: Set<Arbeidstype>,
        brukNyeRegler: Boolean
    ) = aktivitetsgruppe == getGruppeSomSkalSpesialhåndteres(brukNyeRegler) && !brukNyeRegler

    private fun gjelderNyeRegler(beregnGraderGrunnlag: BeregnGraderGrunnlag) =
        (beregnGraderGrunnlag.virkningstidspunktRegelNyEllerBortfaltAktivitet != null
                && !beregnGraderGrunnlag.periode.fom.isBefore(beregnGraderGrunnlag.virkningstidspunktRegelNyEllerBortfaltAktivitet))

    private fun finnTotalNormalarbeidstid(
        beregnGraderGrunnlag: BeregnGraderGrunnlag,
        brukNyeRegler: Boolean,
    ): Duration {
        var sumJobberNormalt1 = Duration.ZERO
        beregnGraderGrunnlag.arbeid.entries.filter {
            brukNyeRegler || !getGruppeSomSkalSpesialhåndteres(brukNyeRegler).contains(
                Arbeidstype.values().find { arbeidstype -> arbeidstype.kode == it.key.type })
        }.filter {
            it.value.tilkommet != true || !brukNyeRegler
        }.forEach {
            sumJobberNormalt1 += it.value.jobberNormalt
        }
        return sumJobberNormalt1
    }

    private fun beregnForSpesialhåndtertGruppeMedGamleRegler(
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>,
        gjenværendeTimerSomDekkes: Duration,
        uttaksgrad: Prosent,
        gradertMotTilsyn: Boolean,
        spesialhåndteringsgruppeSkalSpesialhåndteres: Boolean,
        beregnGraderGrunnlag: BeregnGraderGrunnlag
    ): UtbetalingsgraderOgGjenværendeTimerSomDekkes {
        val utbetalingsgrader = mutableMapOf<Arbeidsforhold, Utbetalingsgrad>()
        arbeid.forEach { (arbeidsforhold, info) ->
            utbetalingsgrader[arbeidsforhold] =
                if (beregnGraderGrunnlag.overstyrtInput != null && beregnGraderGrunnlag.overstyrtInput.overstyrtUtbetalingsgradPerArbeidsforhold.any { it.arbeidsforhold == arbeidsforhold }) {
                    utledGradForOverstyrte(arbeidsforhold, info, beregnGraderGrunnlag.overstyrtInput)
                } else {
                    Utbetalingsgrad(
                        utbetalingsgrad = utledGradForSpesialhåndteringMedGamleRegler(
                            uttaksgrad,
                            gradertMotTilsyn,
                            spesialhåndteringsgruppeSkalSpesialhåndteres,
                            arbeidsforhold.type
                        ),
                        normalArbeidstid = info.jobberNormalt,
                        faktiskArbeidstid = info.jobberNå,
                        tilkommet = info.tilkommet,
                        overstyrt = false
                    )
                }
        }
        return UtbetalingsgraderOgGjenværendeTimerSomDekkes(
            utbetalingsgrader,
            gjenværendeTimerSomDekkes
        )
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

    private fun utledGradForSpesialhåndteringMedGamleRegler(
        uttaksgrad: Prosent,
        gradertMotTilsyn: Boolean,
        spesialhåndteringsgruppeSkalSpesialhåndteres: Boolean,
        type: String
    ): Prosent {
        return if (spesialhåndteringsgruppeSkalSpesialhåndteres && !gradertMotTilsyn && uttaksgrad > Prosent.ZERO) {
            HUNDRE_PROSENT
        } else if (type == Arbeidstype.IKKE_YRKESAKTIV_UTEN_ERSTATNING.kode) {
            HUNDRE_PROSENT
        } else {
            uttaksgrad
        }
    }

    private fun beregnForAktivitetsGruppe(
        taptArbeidstidSomDekkes: Duration,
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>,
        fordeling: Map<Arbeidsforhold, Prosent>,
        overstyrtInput: OverstyrtInput?,
        brukNyeRegler: Boolean
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
                    info.taptArbeid(brukNyeRegler)
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
        brukNyeRegler: Boolean
    ): Map<Arbeidsforhold, Prosent> {
        var sumTapt = Duration.ZERO
        arbeid.values.forEach {
            sumTapt += it.taptArbeid(brukNyeRegler)
        }
        val fordeling = mutableMapOf<Arbeidsforhold, Prosent>()

        arbeid.forEach {
            if (sumTapt != Duration.ZERO) {
                val tapt = it.value.taptArbeid(brukNyeRegler)
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

private fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.sjekkAtArbeidsforholdFinnesBlandtAktivitetsgrupper(skalBrukeNyeRegler: Boolean) {
    val lovligeArbeidstyper = getAktivitetsgruppe(skalBrukeNyeRegler).flatten().map { it.kode }.toSet()
    this.keys.forEach {
        if (!lovligeArbeidstyper.contains(it.type)) {
            throw IllegalArgumentException("Ulovlig arbeidstype ${it.type}")
        }
    }
}

private fun ArbeidsforholdPeriodeInfo.taptArbeid(brukNyeRegler: Boolean): Duration {
    if (tilkommet == true && brukNyeRegler) {
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
