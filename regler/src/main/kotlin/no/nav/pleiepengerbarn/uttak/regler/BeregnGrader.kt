package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

private val TJUE_PROSENT = Prosent(20)
private val HUNDRE_PROSENT = Prosent(100)

private val FULL_DAG = Duration.ofHours(7).plusMinutes(30)


internal object BeregnGrader {

    internal fun beregn(
        pleiebehov: Pleiebehov,
        etablertTilsyn: Duration,
        andreSøkeresTilsyn: Prosent,
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>
            ): GraderBeregnet {
        val etablertTilsynsprosent = finnEtablertTilsynsprosent(pleiebehov, etablertTilsyn)
        val uttaksgradResultat = avklarUttaksgrad(pleiebehov, etablertTilsynsprosent, andreSøkeresTilsyn, arbeid)
        val fordeling = finnFordeling(arbeid)
        val utbetalingsgrader = avklarUtbetalingsgrader(uttaksgradResultat.uttaksgrad, arbeid, fordeling)

        return GraderBeregnet(
            graderingMotTilsyn = GraderingMotTilsyn(
                pleiebehov = pleiebehov,
                etablertTilsyn = etablertTilsynsprosent,
                andreSøkeresTilsyn = andreSøkeresTilsyn,
                tilgjengeligForSøker = uttaksgradResultat.restTilSøker
            ),
            uttaksgrad = uttaksgradResultat.uttaksgrad.setScale(0, RoundingMode.HALF_UP),
            utbetalingsgrader = utbetalingsgrader,
            årsak = uttaksgradResultat.årsak()
        )
    }

    private fun avklarUttaksgrad(pleiebehov: Pleiebehov,
                                 etablertTilsynprosent: Prosent,
                                 andreSøkeresTilsyn: Prosent,
                                 arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>): UttaksgradResultat {
        val restTilSøker = finnRestTilSøker(pleiebehov, etablertTilsynprosent, andreSøkeresTilsyn)

        val graderingMotInntektstap = finnGraderingMotInntektstap(arbeid)

        if (restTilSøker < TJUE_PROSENT || graderingMotInntektstap < TJUE_PROSENT) {
            return UttaksgradResultat(restTilSøker, Prosent.ZERO, avslåttÅrsak = Årsak.FOR_LAV_GRAD)
        }
        if (restTilSøker < graderingMotInntektstap) {
            return UttaksgradResultat(restTilSøker, restTilSøker, innvilgetÅrsak = Årsak.GRADERT_MOT_TILSYN)
        }
        if (arbeid.fulltFravær()) {
            return UttaksgradResultat(restTilSøker, graderingMotInntektstap.setScale(2, RoundingMode.HALF_UP), innvilgetÅrsak = Årsak.FULL_DEKNING)
        }
        return UttaksgradResultat(restTilSøker, graderingMotInntektstap.setScale(2,RoundingMode.HALF_UP), innvilgetÅrsak = Årsak.AVKORTET_MOT_INNTEKT)
    }

    private fun finnRestTilSøker(pleiebehov: Pleiebehov, etablertTilsynsprosent: Prosent, andreSøkeresTilsyn: Prosent): BigDecimal {
        val tilsynsbehovprosent = pleiebehov.prosent
        return tilsynsbehovprosent - etablertTilsynsprosent - andreSøkeresTilsyn
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
            if (it.value.jobberNå != null) {
                val tapt = it.value.jobberNormalt - it.value.jobberNå
                fordeling[it.key] = ((BigDecimal(tapt.toMillis()).setScale(8)/BigDecimal(sumTapt.toMillis())) * HUNDRE_PROSENT).setScale(2, RoundingMode.HALF_UP)
            } else {
                fordeling[it.key] = Prosent.ZERO
            }
        }

       return fordeling
    }

    private fun finnGraderingMotInntektstap(arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>): Prosent {
        var sumJobberNå = Duration.ZERO
        var sumJobberNormalt = Duration.ZERO
        arbeid.values.forEach {
            sumJobberNå += it.jobberNå
            sumJobberNormalt += it.jobberNormalt
        }

        val graderingMotInntektstap = HUNDRE_PROSENT - (BigDecimal(sumJobberNå.toMillis()).setScale(8) / BigDecimal(sumJobberNormalt.toMillis()) * HUNDRE_PROSENT)

        if ( graderingMotInntektstap > HUNDRE_PROSENT) {
            throw IllegalStateException("Faktisk arbeid > normalt arbeid")
        }
        if (graderingMotInntektstap < Prosent.ZERO) {
            throw IllegalStateException("Negativ gradering mot inntektstap ($graderingMotInntektstap)")
        }
        return graderingMotInntektstap
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
    val andreSøkeresTilsyn: Prosent,
    val tilgjengeligForSøker: Prosent
)

data class GraderBeregnet(
        val graderingMotTilsyn: GraderingMotTilsyn,
        val uttaksgrad: Prosent,
        val utbetalingsgrader: Map<Arbeidsforhold, Utbetalingsgrad>,
        val årsak: Årsak
)