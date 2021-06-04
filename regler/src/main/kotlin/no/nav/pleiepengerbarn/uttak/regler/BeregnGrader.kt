package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.lang.IllegalStateException
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
        val søkersTapteArbeidstid = arbeid.finnSøkersTapteArbeidstid()
        val uttaksgradResultat = avklarUttaksgrad(pleiebehov, etablertTilsynsprosent, oppgittTilsyn, andreSøkeresTilsyn, arbeid, søkersTapteArbeidstid, overseEtablertTilsynÅrsak)
        val fordeling = finnFordeling(arbeid)
        val utbetalingsgrader = avklarUtbetalingsgrader(uttaksgradResultat.uttaksgrad, arbeid, fordeling)

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

    private fun avklarUttaksgrad(pleiebehov: Pleiebehov,
                                 etablertTilsynprosent: Prosent,
                                 ønsketUttaksgrad: Duration?,
                                 andreSøkeresTilsyn: Prosent,
                                 arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>,
                                 søkersTapteArbeidstid: Prosent,
                                 overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?
    ): UttaksgradResultat {
        val restTilSøker = finnRestTilSøker(pleiebehov, etablertTilsynprosent, andreSøkeresTilsyn, overseEtablertTilsynÅrsak)

        val ønsketUttaksgradProsent = finnØnsketUttaksgradProsent(ønsketUttaksgrad)

        if (restTilSøker < TJUE_PROSENT) {
            val forLavGradÅrsak = utledForLavGradÅrsak(pleiebehov, etablertTilsynprosent, andreSøkeresTilsyn, overseEtablertTilsynÅrsak)
            return UttaksgradResultat(restTilSøker, Prosent.ZERO, ikkeOppfyltÅrsak = forLavGradÅrsak, overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak)
        }
        if (søkersTapteArbeidstid < TJUE_PROSENT) {
            return UttaksgradResultat(restTilSøker, Prosent.ZERO, ikkeOppfyltÅrsak = Årsak.FOR_LAV_TAPT_ARBEIDSTID, overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak)
        }
        if (ønsketUttaksgradProsent < TJUE_PROSENT) {
            return UttaksgradResultat(restTilSøker, Prosent.ZERO, ikkeOppfyltÅrsak = Årsak.FOR_LAV_ØNSKET_UTTAKSGRAD, overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak)
        }

        if (ønsketUttaksgradProsent < restTilSøker && ønsketUttaksgradProsent < søkersTapteArbeidstid) {
            return UttaksgradResultat(restTilSøker, ønsketUttaksgradProsent, oppfyltÅrsak = Årsak.AVKORTET_MOT_SØKERS_ØNSKE, overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak)
        }
        if (restTilSøker < søkersTapteArbeidstid) {
            return UttaksgradResultat(restTilSøker, restTilSøker, oppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN, overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak)
        }
        if (arbeid.fulltFravær()) {
            return UttaksgradResultat(restTilSøker, søkersTapteArbeidstid.setScale(2, RoundingMode.HALF_UP), oppfyltÅrsak = Årsak.FULL_DEKNING, overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak)
        }
        return UttaksgradResultat(restTilSøker, søkersTapteArbeidstid.setScale(2,RoundingMode.HALF_UP), oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT, overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak)
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
        return BigDecimal(ønsketUttaksgrad.toMillis()).setScale(2, RoundingMode.HALF_UP) / BigDecimal(FULL_DAG.toMillis()) * HUNDRE_PROSENT
    }

    private fun finnRestTilSøker(pleiebehov: Pleiebehov, etablertTilsynsprosent: Prosent, andreSøkeresTilsyn: Prosent, overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?): BigDecimal {
        if (pleiebehov == Pleiebehov.PROSENT_0) {
            return Prosent.ZERO
        }
        val pleiebehovprosent = pleiebehov.prosent
        if (overseEtablertTilsynÅrsak != null) {
            return pleiebehovprosent - andreSøkeresTilsyn
        }
        val gradertMotTilsyn = HUNDRE_PROSENT - etablertTilsynsprosent
        val restTilSøker = pleiebehovprosent - (etablertTilsynsprosent*(pleiebehovprosent / HUNDRE_PROSENT)) - andreSøkeresTilsyn
        val minsteAvRestTilSøkerOgGraderingMotTilsyn = minOf(gradertMotTilsyn, restTilSøker)
        if (minsteAvRestTilSøkerOgGraderingMotTilsyn < Prosent.ZERO) {
            return Prosent.ZERO
        }
        return minsteAvRestTilSøkerOgGraderingMotTilsyn
    }

    private fun utledForLavGradÅrsak(pleiebehov: Pleiebehov, etablertTilsynsprosent: Prosent, andreSøkeresTilsyn: Prosent, overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?): Årsak? {
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

    private fun avklarUtbetalingsgrader(uttaksgrad: Prosent, arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>, fordeling: Map<Arbeidsforhold, Prosent>): Map<Arbeidsforhold, Utbetalingsgrad> {
        var sumJobberNormalt = Duration.ZERO
        arbeid.values.forEach {
            sumJobberNormalt += it.jobberNormalt
        }

        val taptArbeidstidSomDekkes = sumJobberNormalt.prosent(uttaksgrad)

        val utbetalingsgrader = mutableMapOf<Arbeidsforhold, Utbetalingsgrad>()
        arbeid.forEach { (arbeidsforhold, info) ->
            val fordelingsprosent = fordeling[arbeidsforhold]
                ?: throw IllegalStateException("Dette skal ikke skje. Finner ikke fordeling for $arbeidsforhold.")
            val timerDekket = taptArbeidstidSomDekkes.prosent(fordelingsprosent)
            val utbetalingsgrad = BigDecimal(timerDekket.toMillis()).setScale(2) / BigDecimal(info.jobberNormalt.toMillis()) * HUNDRE_PROSENT
            utbetalingsgrader[arbeidsforhold] = Utbetalingsgrad(utbetalingsgrad = utbetalingsgrad, normalArbeidstid = info.jobberNormalt, faktiskArbeidstid = info.jobberNå)

        }

        return utbetalingsgrader
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
                fordeling[it.key] = ((BigDecimal(tapt.toMillis()).setScale(8)/BigDecimal(sumTapt.toMillis())) * HUNDRE_PROSENT).setScale(2, RoundingMode.HALF_UP)
            } else {
                fordeling[it.key] = Prosent.ZERO
            }
        }

       return fordeling
    }

    private fun finnEtablertTilsynsprosent(etablertTilsyn: Duration): Prosent {
        if (etablertTilsyn > FULL_DAG) {
            return HUNDRE_PROSENT
        }
        if (etablertTilsyn < Duration.ZERO) {
            return Prosent.ZERO
        }
        return BigDecimal(etablertTilsyn.toMillis()).setScale(2) / BigDecimal(FULL_DAG.toMillis()) * HUNDRE_PROSENT
    }

}

private fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.fulltFravær() = this.values.all { it.fulltFravær() }
private fun ArbeidsforholdPeriodeInfo.fulltFravær() = jobberNå == Duration.ZERO

data class Utbetalingsgrad(
    val utbetalingsgrad: Prosent,
    val normalArbeidstid: Duration,
    val faktiskArbeidstid: Duration?
)

data class GraderBeregnet(
        val pleiebehov: Pleiebehov,
        val graderingMotTilsyn: GraderingMotTilsyn,
        val søkersTapteArbeidstid: Prosent,
        val oppgittTilsyn: Duration?,
        val uttaksgrad: Prosent,
        val utbetalingsgrader: Map<Arbeidsforhold, Utbetalingsgrad>,
        val årsak: Årsak
)