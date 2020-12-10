package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.math.BigDecimal
import java.time.Duration

internal object AvklarGrader {

    private val TJUE_PROSENT = Prosent(20)
    private val HUNDRE_PROSENT = Prosent(100)

    private val FULL_DAG = Duration.ofHours(7).plusMinutes(30)

    internal fun avklarGrader(
            tilsynsbehov: TilsynsbehovStørrelse,
            etablertTilsyn: Duration,
            andreSøkeresTilsyn: Prosent,
            arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>
            ): AvklarteGrader {
        val (avklartUttaksgrad, justeringsfaktor) = avklarUttaksgradOgJusteringsfaktor(tilsynsbehov, etablertTilsyn, andreSøkeresTilsyn, arbeid)
        val avklartUtbetalingsgrader = avklarUtbetalingsgrader(arbeid, justeringsfaktor)

        return AvklarteGrader(avklartUttaksgrad, avklartUtbetalingsgrader)
    }

    private fun avklarUttaksgradOgJusteringsfaktor(tilsynsbehovStørrelse: TilsynsbehovStørrelse,
                                 etablertTilsyn: Duration,
                                 andreSøkeresTilsyn: Prosent,
                                 arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>): Pair<Prosent, Prosent> {
        val restTilSøker = finnRestTilSøker(tilsynsbehovStørrelse, etablertTilsyn, andreSøkeresTilsyn)

        val søktUttaksgrad = finnSøktUttaksprosent(arbeid)

        if (restTilSøker < TJUE_PROSENT || søktUttaksgrad < TJUE_PROSENT) {
            return Pair(Prosent.ZERO, Prosent.ZERO)
        }
        if (restTilSøker < søktUttaksgrad) {
            return Pair(restTilSøker, restTilSøker / søktUttaksgrad * Prosent(100))
        }
        return Pair(søktUttaksgrad.setScale(2), HUNDRE_PROSENT)
    }

    private fun finnRestTilSøker(tilsynsbehovStørrelse: TilsynsbehovStørrelse, etablertTilsyn: Duration, andreSøkeresTilsyn: Prosent): BigDecimal {
        val tilsynsbehovprosent = tilsynsbehovStørrelse.prosent
        val etablertTilsynsprosent = finnEtablertTilsynsprosent(tilsynsbehovStørrelse, etablertTilsyn)
        return tilsynsbehovprosent - etablertTilsynsprosent - andreSøkeresTilsyn
    }

    private fun avklarUtbetalingsgrader(arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>, justeringsfaktor: Prosent): Map<Arbeidsforhold, Prosent> {
        var sumTaptArbeid = Duration.ZERO
        arbeid.values.forEach { sumTaptArbeid += it.taptArbeidstid }
        val utbetalingsgrader = mutableMapOf<Arbeidsforhold, Prosent>()
        arbeid.forEach { (arbeidsforhold, info) ->
            val ikkeJustertUtbetalingsgrad = BigDecimal(info.taptArbeidstid.toMillis()).setScale(2) / BigDecimal(info.jobberNormalt.toMillis()) * HUNDRE_PROSENT
            utbetalingsgrader[arbeidsforhold] = (ikkeJustertUtbetalingsgrad * (justeringsfaktor / HUNDRE_PROSENT)).setScale(2)
        }
        return utbetalingsgrader
    }

    private fun finnSøktUttaksprosent(arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>): Prosent {
        var sumSøktUttak = Duration.ZERO
        arbeid.values.forEach {
            sumSøktUttak += it.søkersTilsyn
        }
        val søktUttaksgrad = BigDecimal(sumSøktUttak.toMillis()).setScale(2) / BigDecimal(FULL_DAG.toMillis()) * HUNDRE_PROSENT

        if ( søktUttaksgrad > HUNDRE_PROSENT) {
            //TODO: kaste exception istedet?
            return HUNDRE_PROSENT
        }
        if (søktUttaksgrad < Prosent.ZERO) {
            //TODO: kaste exception istedet?
            return Prosent.ZERO
        }
        return søktUttaksgrad
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


data class AvklarteGrader(
        val uttaksgrad: Prosent,
        val utbetalingsgrader: Map<Arbeidsforhold, Prosent>
)