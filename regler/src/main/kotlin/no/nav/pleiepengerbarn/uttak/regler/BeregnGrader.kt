package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

private val TI_PROSENT = Prosent(10)
private val TJUE_PROSENT = Prosent(20)
private val ÅTTI_PROSENT = Prosent(80)
private val HUNDRE_PROSENT = Prosent(100)

private val FULL_DAG = Duration.ofHours(7).plusMinutes(30)


internal object BeregnGrader {

    internal fun beregn(
        pleiebehov: Pleiebehov,
        etablertTilsyn: Duration,
        oppgittTilsyn: Duration? = null,
        andreSøkeresTilsyn: Prosent,
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>
            ): GraderBeregnet {
        val etablertTilsynsprosent = finnEtablertTilsynsprosent(pleiebehov, etablertTilsyn)
        val søkersTapteArbeidstid = arbeid.finnSøkersTapteArbeidstid()
        val uttaksgradResultat = avklarUttaksgrad(pleiebehov, etablertTilsynsprosent, oppgittTilsyn, andreSøkeresTilsyn, arbeid, søkersTapteArbeidstid)
        val fordeling = finnFordeling(arbeid)
        val utbetalingsgrader = avklarUtbetalingsgrader(uttaksgradResultat.uttaksgrad, arbeid, fordeling)

        return GraderBeregnet(
            pleiebehov = pleiebehov,
            graderingMotTilsyn = GraderingMotTilsyn(
                pleiebehov = pleiebehov,
                etablertTilsyn = etablertTilsynsprosent,
                andreSøkeresTilsyn = andreSøkeresTilsyn,
                tilgjengeligForSøker = uttaksgradResultat.restTilSøker,
                overseEtablertTilsynÅrsak = uttaksgradResultat.overseEtablertTilsynÅrsak
            ),
            søkersTapteArbeidstid = søkersTapteArbeidstid,
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
                                 søkersTapteArbeidstid: Prosent): UttaksgradResultat {
        val (restTilSøker, overseEtablertTilsynÅrsak) = finnRestTilSøker(pleiebehov, etablertTilsynprosent, andreSøkeresTilsyn)
        if (etablertTilsynprosent > ÅTTI_PROSENT) {
            return UttaksgradResultat(restTilSøker, Prosent.ZERO, ikkeOppfyltÅrsak = Årsak.FOR_HØY_TILSYNSGRAD, overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak)
        }

        val ønsketUttaksgradProsent = finnØnsketUttaksgradProsent(ønsketUttaksgrad)

        if (restTilSøker < TJUE_PROSENT || søkersTapteArbeidstid < TJUE_PROSENT || ønsketUttaksgradProsent < TJUE_PROSENT) {
            return UttaksgradResultat(restTilSøker, Prosent.ZERO, ikkeOppfyltÅrsak = Årsak.FOR_LAV_GRAD, overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak)
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

    private fun finnRestTilSøker(pleiebehov: Pleiebehov, etablertTilsynsprosent: Prosent, andreSøkeresTilsyn: Prosent): Pair<BigDecimal, OverseEtablertTilsynÅrsak?> {
        val pleiebehovprosent = pleiebehov.prosent
        if (etablertTilsynsprosent > Prosent.ZERO && etablertTilsynsprosent < TI_PROSENT) {
            return Pair(pleiebehovprosent - andreSøkeresTilsyn, OverseEtablertTilsynÅrsak.FOR_LAVT)
        }
        return Pair(pleiebehovprosent - etablertTilsynsprosent - andreSøkeresTilsyn, null)
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



    private fun finnEtablertTilsynsprosent(pleiebehov: Pleiebehov, etablertTilsyn: Duration): Prosent {
        if (etablertTilsyn > FULL_DAG) {
            return HUNDRE_PROSENT
        }
        if (etablertTilsyn < Duration.ZERO) {
            return Prosent.ZERO
        }
        val etablertTilsynProsent = BigDecimal(etablertTilsyn.toMillis()).setScale(2) / BigDecimal(FULL_DAG.toMillis()) * HUNDRE_PROSENT
        if (pleiebehov == Pleiebehov.PROSENT_200) {
            return etablertTilsynProsent * BigDecimal(2)
        }
        return etablertTilsynProsent
    }

}

private fun Duration.prosent(prosent: Prosent): Duration = Duration.ofMillis( (BigDecimal(this.toMillis()).setScale(8) * prosent / HUNDRE_PROSENT).toLong() )

private fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.fulltFravær() = this.values.all { it.fulltFravær() }
private fun ArbeidsforholdPeriodeInfo.fulltFravær() = jobberNå == Duration.ZERO

data class Utbetalingsgrad(
    val utbetalingsgrad: Prosent,
    val normalArbeidstid: Duration,
    val faktiskArbeidstid: Duration?
)

data class GraderingMotTilsyn(
    val pleiebehov: Pleiebehov,
    val etablertTilsyn: Prosent,
    val overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?,
    val andreSøkeresTilsyn: Prosent,
    val tilgjengeligForSøker: Prosent
)

data class GraderBeregnet(
        val pleiebehov: Pleiebehov,
        val graderingMotTilsyn: GraderingMotTilsyn,
        val søkersTapteArbeidstid: Prosent,
        val uttaksgrad: Prosent,
        val utbetalingsgrader: Map<Arbeidsforhold, Utbetalingsgrad>,
        val årsak: Årsak
)