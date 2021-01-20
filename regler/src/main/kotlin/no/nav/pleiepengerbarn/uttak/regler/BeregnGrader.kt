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
            tilsynsbehov: TilsynsbehovStørrelse,
            etablertTilsyn: Duration,
            andreSøkeresTilsyn: Prosent,
            arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>
            ): GraderBeregnet {
        val uttaksgradResultat = avklarUttaksgrad(tilsynsbehov, etablertTilsyn, andreSøkeresTilsyn, arbeid)
        val fordeling = finnFordeling(arbeid)
        val utbetalingsgrader = avklarUtbetalingsgrader(uttaksgradResultat.uttaksgrad, arbeid, fordeling)

        return GraderBeregnet(uttaksgradResultat.uttaksgrad.setScale(0, RoundingMode.HALF_UP), utbetalingsgrader, uttaksgradResultat.årsak())
    }

    private fun avklarUttaksgrad(tilsynsbehovStørrelse: TilsynsbehovStørrelse,
                                 etablertTilsyn: Duration,
                                 andreSøkeresTilsyn: Prosent,
                                 arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>): UttaksgradResultat {
        val restTilSøker = finnRestTilSøker(tilsynsbehovStørrelse, etablertTilsyn, andreSøkeresTilsyn)

        val graderingMotInntektstap = finnGraderingMotInntektstap(arbeid)

        if (restTilSøker < TJUE_PROSENT || graderingMotInntektstap < TJUE_PROSENT) {
            return UttaksgradResultat(Prosent.ZERO, avslåttÅrsak = AvslåttÅrsaker.FOR_LAV_GRAD)
        }
        if (restTilSøker < graderingMotInntektstap) {
            return UttaksgradResultat(restTilSøker, innvilgetÅrsak = InnvilgetÅrsaker.GRADERT_MOT_TILSYN)
        }
        if (arbeid.fulltFravær()) {
            return UttaksgradResultat(graderingMotInntektstap.setScale(2, RoundingMode.HALF_UP), innvilgetÅrsak = InnvilgetÅrsaker.FULL_DEKNING)
        }
        return UttaksgradResultat(graderingMotInntektstap.setScale(2,RoundingMode.HALF_UP), innvilgetÅrsak = InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
    }

    private fun finnRestTilSøker(tilsynsbehovStørrelse: TilsynsbehovStørrelse, etablertTilsyn: Duration, andreSøkeresTilsyn: Prosent): BigDecimal {
        val tilsynsbehovprosent = tilsynsbehovStørrelse.prosent
        val etablertTilsynsprosent = finnEtablertTilsynsprosent(tilsynsbehovStørrelse, etablertTilsyn)
        return tilsynsbehovprosent - etablertTilsynsprosent - andreSøkeresTilsyn
    }

    private fun avklarUtbetalingsgrader(uttaksgrad: Prosent, arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>, fordeling: Map<Arbeidsforhold, Prosent>): Map<Arbeidsforhold, Prosent> {
        var sumJobberNormalt = Duration.ZERO
        arbeid.values.forEach {
            sumJobberNormalt += it.jobberNormalt
        }

        val taptArbeidstidSomDekkes = sumJobberNormalt.prosent(uttaksgrad)

        val utbetalingsgrader = mutableMapOf<Arbeidsforhold, Prosent>()
        arbeid.forEach { (arbeidsforhold, info) ->
            val fordelingsprosent = fordeling[arbeidsforhold]
                ?: throw IllegalStateException("Dette skal ikke skje. Finner ikke fordeling for $arbeidsforhold.")
            val timerDekket = taptArbeidstidSomDekkes.prosent(fordelingsprosent)
            utbetalingsgrader[arbeidsforhold] = BigDecimal(timerDekket.toMillis()).setScale(2) / BigDecimal(info.jobberNormalt.toMillis()) * HUNDRE_PROSENT

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

    private fun finnEtablertTilsynsprosent(tilsynsbehovStørrelse: TilsynsbehovStørrelse, etablertTilsyn: Duration): Prosent {
        if (etablertTilsyn > FULL_DAG) {
            return HUNDRE_PROSENT
        }
        if (etablertTilsyn < Duration.ZERO) {
            return Prosent.ZERO
        }
        val etablertTilsynProsent = BigDecimal(etablertTilsyn.toMillis()).setScale(2) / BigDecimal(FULL_DAG.toMillis()) * HUNDRE_PROSENT
        if (tilsynsbehovStørrelse == TilsynsbehovStørrelse.PROSENT_200) {
            return etablertTilsynProsent * BigDecimal(2)
        }
        return etablertTilsynProsent
    }

}

private fun Duration.prosent(prosent: Prosent): Duration = Duration.ofMillis( (BigDecimal(this.toMillis()).setScale(8) * prosent / HUNDRE_PROSENT).toLong() )

private fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.fulltFravær() = this.values.all { it.fulltFravær() }
private fun ArbeidsforholdPeriodeInfo.fulltFravær() = jobberNå == Duration.ZERO

data class GraderBeregnet(
        val uttaksgrad: Prosent,
        val utbetalingsgrader: Map<Arbeidsforhold, Prosent>,
        val årsak: Årsak
)