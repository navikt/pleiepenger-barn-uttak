package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.GraderBeregnet
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate

internal object BeregnGrader {

    internal fun beregn(beregnGraderGrunnlag: BeregnGraderGrunnlag): GraderBeregnet {
        val etablertTilsynsprosent = finnEtablertTilsynsprosent(beregnGraderGrunnlag.etablertTilsyn)
        val skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper = beregnGraderGrunnlag.arbeid.seBortFraAndreArbeidsforhold(beregnGraderGrunnlag.periode, beregnGraderGrunnlag.nyeReglerUtbetalingsgrad)
        val søkersTapteArbeidstid =
            beregnGraderGrunnlag.arbeid.finnSøkersTapteArbeidstid(skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper)
        val uttaksgradResultat = avklarUttaksgrad(beregnGraderGrunnlag, etablertTilsynsprosent, finnØnsketUttaksgradProsent(beregnGraderGrunnlag.oppgittTilsyn))
        val utbetalingsgrader = BeregnUtbetalingsgrader.beregn(
            uttaksgradResultat.uttaksgrad,
            uttaksgradResultat.overstyrtUttaksgrad,
            uttaksgradResultat.oppfyltÅrsak == Årsak.GRADERT_MOT_TILSYN,
            beregnGraderGrunnlag
        )
        val faktiskUttaksgrad = uttaksgradResultat.overstyrtUttaksgrad ?: uttaksgradResultat.uttaksgrad

        return GraderBeregnet(
            pleiebehov = beregnGraderGrunnlag.pleiebehov,
            graderingMotTilsyn = GraderingMotTilsyn(
                etablertTilsyn = etablertTilsynsprosent,
                andreSøkeresTilsyn = beregnGraderGrunnlag.andreSøkeresTilsyn,
                andreSøkeresTilsynReberegnet = beregnGraderGrunnlag.andreSøkeresTilsynReberegnet,
                tilgjengeligForSøker = uttaksgradResultat.restTilSøker,
                overseEtablertTilsynÅrsak = uttaksgradResultat.overseEtablertTilsynÅrsak
            ),
            søkersTapteArbeidstid = søkersTapteArbeidstid,
            oppgittTilsyn = beregnGraderGrunnlag.oppgittTilsyn,
            uttaksgrad = faktiskUttaksgrad.setScale(0, RoundingMode.HALF_UP),
            utbetalingsgrader = utbetalingsgrader,
            årsak = uttaksgradResultat.årsak(),
            manueltOverstyrt = uttaksgradResultat.overstyrtUttaksgrad != null || utbetalingsgrader.any { it.value.overstyrt == true }
        )
    }

    internal fun beregnMedMaksGrad(
        beregnGraderGrunnlag: BeregnGraderGrunnlag,
        maksGradIProsent: Prosent
    ): GraderBeregnet {
        val etablertTilsynsprosent = finnEtablertTilsynsprosent(beregnGraderGrunnlag.etablertTilsyn)
        val skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper = beregnGraderGrunnlag.arbeid.seBortFraAndreArbeidsforhold(beregnGraderGrunnlag.periode, beregnGraderGrunnlag.nyeReglerUtbetalingsgrad)
        val søkersTapteArbeidstid =
            beregnGraderGrunnlag.arbeid.finnSøkersTapteArbeidstid(skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper)
        val ønsketUttaksgradProsent = finnØnsketUttaksgradProsent(beregnGraderGrunnlag.oppgittTilsyn);
        val ønsketUttaksgrad = minOf(ønsketUttaksgradProsent, maksGradIProsent)
        val uttaksgradResultat = avklarUttaksgrad(
            beregnGraderGrunnlag,
            etablertTilsynsprosent,
            ønsketUttaksgrad
        )
        val utbetalingsgrader = BeregnUtbetalingsgrader.beregn(
            uttaksgradResultat.uttaksgrad,
            uttaksgradResultat.overstyrtUttaksgrad,
            uttaksgradResultat.oppfyltÅrsak == Årsak.GRADERT_MOT_TILSYN,
            beregnGraderGrunnlag
        )

        return GraderBeregnet(
            pleiebehov = beregnGraderGrunnlag.pleiebehov,
            graderingMotTilsyn = GraderingMotTilsyn(
                etablertTilsyn = etablertTilsynsprosent,
                andreSøkeresTilsyn = beregnGraderGrunnlag.andreSøkeresTilsyn,
                andreSøkeresTilsynReberegnet = beregnGraderGrunnlag.andreSøkeresTilsynReberegnet,
                tilgjengeligForSøker = uttaksgradResultat.restTilSøker,
                overseEtablertTilsynÅrsak = uttaksgradResultat.overseEtablertTilsynÅrsak
            ),
            søkersTapteArbeidstid = søkersTapteArbeidstid,
            oppgittTilsyn = beregnGraderGrunnlag.oppgittTilsyn,
            uttaksgrad = uttaksgradResultat.uttaksgrad.setScale(0, RoundingMode.HALF_UP),
            utbetalingsgrader = utbetalingsgrader,
            årsak = uttaksgradResultat.årsak()
        )
    }

    private fun avklarUttaksgrad(
        beregnGraderGrunnlag: BeregnGraderGrunnlag,
        etablertTilsynprosent: Prosent,
        ønsketUttaksgradProsent: Prosent
    ): UttaksgradResultat {

        val skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper = beregnGraderGrunnlag.arbeid.seBortFraAndreArbeidsforhold(beregnGraderGrunnlag.periode, beregnGraderGrunnlag.nyeReglerUtbetalingsgrad)
        val søkersTapteArbeidstid =
            beregnGraderGrunnlag.arbeid.finnSøkersTapteArbeidstid(skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper)

        val restTilSøker =
            finnRestTilSøker(beregnGraderGrunnlag.pleiebehov, etablertTilsynprosent, beregnGraderGrunnlag.andreSøkeresTilsyn, beregnGraderGrunnlag.overseEtablertTilsynÅrsak)

        val overstyrtUttak = if (beregnGraderGrunnlag.overstyrtInput != null) beregnGraderGrunnlag.overstyrtInput.overstyrtUttaksgrad else null

        if (restTilSøker < TJUE_PROSENT) {
            val forLavGradÅrsak =
                utledForLavGradÅrsak(beregnGraderGrunnlag.pleiebehov, etablertTilsynprosent, beregnGraderGrunnlag.andreSøkeresTilsyn, beregnGraderGrunnlag.overseEtablertTilsynÅrsak)
            if (!(beregnGraderGrunnlag.ytelseType == YtelseType.PLS && restTilSøker > Prosent.ZERO && Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE == forLavGradÅrsak)) {
                return UttaksgradResultat(
                    restTilSøker,
                    Prosent.ZERO,
                    overstyrtUttak,
                    ikkeOppfyltÅrsak = forLavGradÅrsak,
                    overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
                )
            }
        }

        if (skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper) {
            val søkersTapteArbeidstidUtenAndreArbeidsforhold = beregnGraderGrunnlag.arbeid.finnSøkersTapteArbeidstid(true)
            if (søkersTapteArbeidstidUtenAndreArbeidsforhold < TJUE_PROSENT) {
                return UttaksgradResultat(
                    restTilSøker,
                    Prosent.ZERO,
                    overstyrtUttak,
                    ikkeOppfyltÅrsak = Årsak.FOR_LAV_TAPT_ARBEIDSTID,
                    overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
                )
            }
        } else {
            if (søkersTapteArbeidstid < TJUE_PROSENT) {
                return UttaksgradResultat(
                    restTilSøker,
                    Prosent.ZERO,
                    overstyrtUttak,
                    ikkeOppfyltÅrsak = Årsak.FOR_LAV_TAPT_ARBEIDSTID,
                    overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
                )
            }
        }
        if (ønsketUttaksgradProsent < TJUE_PROSENT) {
            return UttaksgradResultat(
                restTilSøker,
                Prosent.ZERO,
                overstyrtUttak,
                ikkeOppfyltÅrsak = Årsak.FOR_LAV_ØNSKET_UTTAKSGRAD,
                overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
            )
        }

        if (ønsketUttaksgradProsent < restTilSøker && ønsketUttaksgradProsent < søkersTapteArbeidstid) {
            return UttaksgradResultat(
                restTilSøker,
                ønsketUttaksgradProsent,
                overstyrtUttak,
                oppfyltÅrsak = Årsak.AVKORTET_MOT_SØKERS_ØNSKE,
                overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
            )
        }
        if (restTilSøker < søkersTapteArbeidstid) {
            return UttaksgradResultat(
                restTilSøker,
                restTilSøker,
                overstyrtUttak,
                oppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN,
                overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
            )
        }
        if (beregnGraderGrunnlag.arbeid.fulltFravær()) {
            return UttaksgradResultat(
                restTilSøker,
                søkersTapteArbeidstid.setScale(2, RoundingMode.HALF_UP),
                overstyrtUttak,
                oppfyltÅrsak = Årsak.FULL_DEKNING,
                overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
            )
        }
        return UttaksgradResultat(
            restTilSøker,
            søkersTapteArbeidstid.setScale(2, RoundingMode.HALF_UP),
            overstyrtUttak,
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
        )
    }

    private fun finnØnsketUttaksgradProsent(ønsketUttaksgrad: Duration?): Prosent {
        if (ønsketUttaksgrad == null) {
            return HUNDRE_PROSENT
        }
        if (ønsketUttaksgrad > FULL_DAG) {
            return HUNDRE_PROSENT
        }
        if (ønsketUttaksgrad < Duration.ZERO) {
            return Prosent.ZERO
        }
        return BigDecimal(ønsketUttaksgrad.toMillis()).setScale(2, RoundingMode.HALF_UP)
            .divide(BigDecimal(FULL_DAG.toMillis()), 2, RoundingMode.HALF_UP) * HUNDRE_PROSENT
    }

    private fun finnRestTilSøker(
        pleiebehov: Pleiebehov,
        etablertTilsynsprosent: Prosent,
        andreSøkeresTilsyn: Prosent,
        overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?
    ): BigDecimal {
        if (pleiebehov == Pleiebehov.PROSENT_0) {
            return Prosent.ZERO
        }
        val pleiebehovprosent = pleiebehov.prosent
        if (overseEtablertTilsynÅrsak != null) {
            return pleiebehovprosent - andreSøkeresTilsyn
        }
        val gradertMotTilsyn = HUNDRE_PROSENT - etablertTilsynsprosent
        val restTilSøker =
            pleiebehovprosent - (etablertTilsynsprosent * (pleiebehovprosent.divide(
                HUNDRE_PROSENT,
                2,
                RoundingMode.HALF_UP
            ))) - andreSøkeresTilsyn
        val minsteAvRestTilSøkerOgGraderingMotTilsyn = minOf(gradertMotTilsyn, restTilSøker)
        if (minsteAvRestTilSøkerOgGraderingMotTilsyn < Prosent.ZERO) {
            return Prosent.ZERO
        }
        return minsteAvRestTilSøkerOgGraderingMotTilsyn
    }

    private fun utledForLavGradÅrsak(
        pleiebehov: Pleiebehov,
        etablertTilsynsprosent: Prosent,
        andreSøkeresTilsyn: Prosent,
        overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?
    ): Årsak? {
        if (pleiebehov == Pleiebehov.PROSENT_0) {
            return Årsak.UTENOM_PLEIEBEHOV
        }
        if (overseEtablertTilsynÅrsak != null) {
            if (andreSøkeresTilsyn > ÅTTI_PROSENT) {
                return Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE
            }
        } else {
            when {
                andreSøkeresTilsyn > ÅTTI_PROSENT -> {
                    return Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE
                }

                etablertTilsynsprosent > ÅTTI_PROSENT -> {
                    return Årsak.FOR_LAV_REST_PGA_ETABLERT_TILSYN
                }

                andreSøkeresTilsyn + etablertTilsynsprosent > ÅTTI_PROSENT -> {
                    return Årsak.FOR_LAV_REST_PGA_ETABLERT_TILSYN_OG_ANDRE_SØKERE
                }
            }
        }
        return null
    }

    private fun finnEtablertTilsynsprosent(etablertTilsyn: Duration): Prosent {
        if (etablertTilsyn > FULL_DAG) {
            return HUNDRE_PROSENT
        }
        if (etablertTilsyn < Duration.ZERO) {
            return Prosent.ZERO
        }
        return BigDecimal(etablertTilsyn.toMillis()).setScale(2, RoundingMode.HALF_UP)
            .divide(BigDecimal(FULL_DAG.toMillis()), 2, RoundingMode.HALF_UP) * HUNDRE_PROSENT
    }

}

private fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.seBortFraAndreArbeidsforhold(periode: LukketPeriode, nyeReglerUtbetalingsgrad: LocalDate?): Boolean {
    val nyeReglerGjelder = nyeReglerUtbetalingsgrad != null
            && !periode.fom.isBefore(nyeReglerUtbetalingsgrad)

    val harIkkeYrkesaktiv = this.keys.any {
        GRUPPE_SOM_SKAL_SPESIALHÅNDTERES.contains(
            Arbeidstype.values().find { arbeidstype -> arbeidstype.kode == it.type })
    }
    val harAndreArbeidsforhold = this.any {
        Arbeidstype.values()
            .find { arbeidstype -> arbeidstype.kode == it.key.type } !in GRUPPE_SOM_SKAL_SPESIALHÅNDTERES && !it.value.utenArbeidtid()
                && !(Arbeidstype.FRILANSER.kode == it.key.type && it.value.ikkeFravær())
    }

    return harIkkeYrkesaktiv && harAndreArbeidsforhold && !nyeReglerGjelder
}

private fun ArbeidsforholdPeriodeInfo.utenArbeidtid() =
    this.jobberNormalt == Duration.ZERO && this.jobberNå == Duration.ZERO

private fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.fulltFravær() = this.values.all { it.fulltFravær() }
private fun ArbeidsforholdPeriodeInfo.fulltFravær() = jobberNå == Duration.ZERO
private fun ArbeidsforholdPeriodeInfo.ikkeFravær() = jobberNormalt <= jobberNå
