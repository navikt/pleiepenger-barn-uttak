package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.GraderBeregnet
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

internal object BeregnGrader {

    internal fun beregn(
        pleiebehov: Pleiebehov,
        etablertTilsyn: Duration,
        oppgittTilsyn: Duration? = null,
        andreSøkeresTilsyn: Prosent,
        andreSøkeresTilsynReberegnet: Boolean,
        overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak? = null,
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>
    ): GraderBeregnet {
        val etablertTilsynsprosent = finnEtablertTilsynsprosent(etablertTilsyn)
        val skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper = arbeid.seBortFraAndreArbeidsforhold()
        val søkersTapteArbeidstid = arbeid.finnSøkersTapteArbeidstid(skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper)
        val uttaksgradResultat = avklarUttaksgrad(
            pleiebehov,
            etablertTilsynsprosent,
            oppgittTilsyn,
            andreSøkeresTilsyn,
            arbeid,
            overseEtablertTilsynÅrsak
        )
        val utbetalingsgrader = BeregnUtbetalingsgrader.beregn(uttaksgradResultat.uttaksgrad, arbeid)

        return GraderBeregnet(
            pleiebehov = pleiebehov,
            graderingMotTilsyn = GraderingMotTilsyn(
                etablertTilsyn = etablertTilsynsprosent,
                andreSøkeresTilsyn = andreSøkeresTilsyn,
                andreSøkeresTilsynReberegnet = andreSøkeresTilsynReberegnet,
                tilgjengeligForSøker = uttaksgradResultat.restTilSøker,
                overseEtablertTilsynÅrsak = uttaksgradResultat.overseEtablertTilsynÅrsak
            ),
            søkersTapteArbeidstid = søkersTapteArbeidstid,
            oppgittTilsyn = oppgittTilsyn,
            uttaksgrad = uttaksgradResultat.uttaksgrad.setScale(0, RoundingMode.HALF_UP),
            utbetalingsgrader = utbetalingsgrader,
            årsak = uttaksgradResultat.årsak()
        )
    }

    private fun avklarUttaksgrad(
        pleiebehov: Pleiebehov,
        etablertTilsynprosent: Prosent,
        ønsketUttaksgrad: Duration?,
        andreSøkeresTilsyn: Prosent,
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>,
        overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?
    ): UttaksgradResultat {
        val skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper = arbeid.seBortFraAndreArbeidsforhold()
        val søkersTapteArbeidstid = arbeid.finnSøkersTapteArbeidstid(skalSeBortIfraArbeidstidFraSpesialhåndterteArbeidtyper)

        val restTilSøker =
            finnRestTilSøker(pleiebehov, etablertTilsynprosent, andreSøkeresTilsyn, overseEtablertTilsynÅrsak)

        val ønsketUttaksgradProsent = finnØnsketUttaksgradProsent(ønsketUttaksgrad)

        if (restTilSøker < TJUE_PROSENT) {
            val forLavGradÅrsak =
                utledForLavGradÅrsak(pleiebehov, etablertTilsynprosent, andreSøkeresTilsyn, overseEtablertTilsynÅrsak)
            return UttaksgradResultat(
                restTilSøker,
                Prosent.ZERO,
                ikkeOppfyltÅrsak = forLavGradÅrsak,
                overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak
            )
        }
        val seBortFraAndreArbeidsforhold = arbeid.seBortFraAndreArbeidsforhold()
        if (seBortFraAndreArbeidsforhold) {
            val søkersTapteArbeidstidUtenAndreArbeidsforhold = arbeid.finnSøkersTapteArbeidstid(true)
            if (søkersTapteArbeidstidUtenAndreArbeidsforhold < TJUE_PROSENT) {
                return UttaksgradResultat(
                    restTilSøker,
                    Prosent.ZERO,
                    ikkeOppfyltÅrsak = Årsak.FOR_LAV_TAPT_ARBEIDSTID,
                    overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak
                )
            }
        } else {
            if (søkersTapteArbeidstid < TJUE_PROSENT) {
                return UttaksgradResultat(
                    restTilSøker,
                    Prosent.ZERO,
                    ikkeOppfyltÅrsak = Årsak.FOR_LAV_TAPT_ARBEIDSTID,
                    overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak
                )
            }
        }
        if (ønsketUttaksgradProsent < TJUE_PROSENT) {
            return UttaksgradResultat(
                restTilSøker,
                Prosent.ZERO,
                ikkeOppfyltÅrsak = Årsak.FOR_LAV_ØNSKET_UTTAKSGRAD,
                overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak
            )
        }

        if (ønsketUttaksgradProsent < restTilSøker && ønsketUttaksgradProsent < søkersTapteArbeidstid) {
            return UttaksgradResultat(
                restTilSøker,
                ønsketUttaksgradProsent,
                oppfyltÅrsak = Årsak.AVKORTET_MOT_SØKERS_ØNSKE,
                overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak
            )
        }
        if (restTilSøker < søkersTapteArbeidstid) {
            return UttaksgradResultat(
                restTilSøker,
                restTilSøker,
                oppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN,
                overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak
            )
        }
        if (arbeid.fulltFravær()) {
            return UttaksgradResultat(
                restTilSøker,
                søkersTapteArbeidstid.setScale(2, RoundingMode.HALF_UP),
                oppfyltÅrsak = Årsak.FULL_DEKNING,
                overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak
            )
        }
        return UttaksgradResultat(
            restTilSøker,
            søkersTapteArbeidstid.setScale(2, RoundingMode.HALF_UP),
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak
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
        return BigDecimal(ønsketUttaksgrad.toMillis()).setScale(
            2,
            RoundingMode.HALF_UP
        ) / BigDecimal(FULL_DAG.toMillis()) * HUNDRE_PROSENT
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
            pleiebehovprosent - (etablertTilsynsprosent * (pleiebehovprosent / HUNDRE_PROSENT)) - andreSøkeresTilsyn
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
        return BigDecimal(etablertTilsyn.toMillis()).setScale(
            2,
            RoundingMode.HALF_UP
        ) / BigDecimal(FULL_DAG.toMillis()) * HUNDRE_PROSENT
    }

}

private fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.seBortFraAndreArbeidsforhold(): Boolean {
    val harIkkeYrkesaktiv = this.keys.any { GRUPPE_SOM_SKAL_SPESIALHÅNDTERES.contains(Arbeidstype.values().find { arbeidstype -> arbeidstype.kode == it.type }) }
    val harAndreArbeidsforhold = this.any { Arbeidstype.values().find { arbeidstype -> arbeidstype.kode == it.key.type } !in GRUPPE_SOM_SKAL_SPESIALHÅNDTERES && !it.value.utenArbeidtid()
            && !(FeatureToggle.isActive("SPESIALHANDTERING_GRUPPE_PLUSS_FL") && Arbeidstype.FRILANSER.kode == it.key.type && it.value.ikkeFravær()) }

    return harIkkeYrkesaktiv && harAndreArbeidsforhold
}

private fun ArbeidsforholdPeriodeInfo.utenArbeidtid() =
    this.jobberNormalt == Duration.ZERO && this.jobberNå == Duration.ZERO

private fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.fulltFravær() = this.values.all { it.fulltFravær() }
private fun ArbeidsforholdPeriodeInfo.fulltFravær() = jobberNå == Duration.ZERO
private fun ArbeidsforholdPeriodeInfo.ikkeFravær() = jobberNormalt <= jobberNå
