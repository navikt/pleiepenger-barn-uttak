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
            andresTilsyn: Prosent,
            arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>
            ): AvklarteGrader {

        val avklartUttaksgrad = avklarUttaksgrad(tilsynsbehov, etablertTilsyn, andresTilsyn, arbeid)
        val avklartUtbetalingsgrader = avklarUtbetalingsgrader(tilsynsbehov, arbeid, andresTilsyn, etablertTilsyn)

        return AvklarteGrader(avklartUttaksgrad, avklartUtbetalingsgrader)
    }

    private fun avklarUttaksgrad(tilsynsbehovStørrelse: TilsynsbehovStørrelse,
                                 etablertTilsyn: Duration,
                                 andreSøkeresTilsyn: Prosent,
                                 arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>): Prosent {
        val tilsynsbehovprosent = tilsynsbehovStørrelse.prosent
        val etablertTilsynsprosent = finnEtablertTilsynsprosent(tilsynsbehovStørrelse, etablertTilsyn)
        val restTilSøker = tilsynsbehovprosent - etablertTilsynsprosent - andreSøkeresTilsyn
        val søktUttaksgrad = finnSøktUttaksprosent(arbeid)

        if (restTilSøker < TJUE_PROSENT || søktUttaksgrad < TJUE_PROSENT) {
            return Prosent.ZERO
        }
        if (restTilSøker < søktUttaksgrad) {
            return restTilSøker
        }
        return søktUttaksgrad.setScale(2)
    }

    private fun avklarUtbetalingsgrader(tilsynsbehovStørrelse: TilsynsbehovStørrelse, arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>, uttaksgradAndreSøkere: Prosent, etablertTilsyn: Duration): Map<Arbeidsforhold, Prosent> {
        var sumTaptArbeid = Duration.ZERO
        arbeid.values.forEach { sumTaptArbeid += it.taptArbeidstid }
        val utbetalingsgradJusteringsfaktor = finnUtbetalingsgradJusteringsfaktor(tilsynsbehovStørrelse, sumTaptArbeid, uttaksgradAndreSøkere, etablertTilsyn)
        val utbetalingsgrader = mutableMapOf<Arbeidsforhold, Prosent>()
        arbeid.forEach { (arbeidsforhold, info) ->
            val ikkeJustertUtbetalingsgrad = BigDecimal(info.taptArbeidstid.toMillis()).setScale(2) / BigDecimal(info.jobberNormalt.toMillis()) * HUNDRE_PROSENT
            utbetalingsgrader[arbeidsforhold] = (ikkeJustertUtbetalingsgrad * (utbetalingsgradJusteringsfaktor / HUNDRE_PROSENT)).setScale(2)
        }
        return utbetalingsgrader
    }

    private fun finnUtbetalingsgradJusteringsfaktor(tilsynsbehovStørrelse: TilsynsbehovStørrelse, sumTaptArbeid: Duration, uttaksgradAndreSøkere: Prosent, etablertTilsyn: Duration): Prosent {
        if (tilsynsbehovStørrelse == TilsynsbehovStørrelse.PROSENT_0) {
            return BigDecimal.ZERO.setScale(2)
        }
        val tilsynsgrad = finnTilsynsgrad(uttaksgradAndreSøkere, etablertTilsyn)
        val fullDagMinusTilsyn = FULL_DAG - FULL_DAG.multipliedBy(tilsynsgrad.toLong()).dividedBy(100)
        if (sumTaptArbeid <= fullDagMinusTilsyn) {
            return Prosent(100).setScale(2)
        }

        if (sumTaptArbeid < FULL_DAG) {
            return (BigDecimal(FULL_DAG.toMillis()).setScale(2) - (BigDecimal(FULL_DAG.toMillis()) * tilsynsgrad / HUNDRE_PROSENT)) / BigDecimal(sumTaptArbeid.toMillis()) * HUNDRE_PROSENT
        }
        val justeringsfaktor = tilsynsbehovStørrelse.prosent.setScale(2) - tilsynsgrad
        if (justeringsfaktor > HUNDRE_PROSENT) {
            return HUNDRE_PROSENT.setScale(2)
        }
        return justeringsfaktor
    }

    private fun finnTilsynsgrad(uttaksgradAndreSøkere: Prosent, etablertTilsyn: Duration): Prosent {
        return uttaksgradAndreSøkere.setScale(2) + (BigDecimal(etablertTilsyn.toMillis())/BigDecimal(FULL_DAG.toMillis())*Prosent(100))
    }

    private fun finnSøktUttaksprosent(arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>): Prosent {
        var sumTaptArbeidstid = Duration.ZERO
        var sumJobberNormalt = Duration.ZERO
        arbeid.values.forEach {
            sumTaptArbeidstid += it.taptArbeidstid
            sumJobberNormalt += it.jobberNormalt
        }
        val søktUttaksgrad = BigDecimal(sumTaptArbeidstid.toMillis()).setScale(2) / BigDecimal(sumJobberNormalt.toMillis()) * HUNDRE_PROSENT

        if ( søktUttaksgrad > HUNDRE_PROSENT) {
            return HUNDRE_PROSENT
        }
        if (søktUttaksgrad < Prosent.ZERO) {
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