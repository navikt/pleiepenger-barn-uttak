package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.regler.FULL_DAG
import no.nav.pleiepengerbarn.uttak.regler.FeatureToggle
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

class TaptArbeidstid(
    val sumJobberNå: Duration,
    val sumJobberNormalt: Duration
) {
    fun beregnTaptArbeidIProsent(): Prosent {
        if (sumJobberNormalt == Duration.ZERO) {
            return Prosent.ZERO
        }

        val søkersTapteArbeidstid =
            HUNDRE_PROSENT - (BigDecimal(sumJobberNå.toMillis()).setScale(8, RoundingMode.HALF_UP).divide(
                BigDecimal(
                    sumJobberNormalt.toMillis()
                ), 8, RoundingMode.HALF_UP
            ) * HUNDRE_PROSENT)

        if (søkersTapteArbeidstid > HUNDRE_PROSENT) {
            throw IllegalStateException("Faktisk arbeid > normalt arbeid")
        }
        if (søkersTapteArbeidstid < Prosent.ZERO) {
            return Prosent.ZERO
        }
        return søkersTapteArbeidstid;
    }

    fun beregnTaptArbeidIProsentJustertMotNormalArbeidstid(): Prosent {
        if (FeatureToggle.isActive(
                key = "JUSTER_NORMALTID_ANDRE_PARTERS_TILSYN",
                default = false
            )
        ) {
            val beregnetTilsynsgrad = beregnTaptArbeidIProsent() * beregnetJusteringsgrad()
            if (beregnetTilsynsgrad > HUNDRE_PROSENT) {
                return HUNDRE_PROSENT
            }
            return beregnetTilsynsgrad.setScale(8, RoundingMode.HALF_UP)
        }
        return beregnTaptArbeidIProsent()
    }

    private fun beregnetJusteringsgrad(): BigDecimal {
        return BigDecimal(sumJobberNormalt.toMillis()).setScale(8, RoundingMode.HALF_UP).divide(
            BigDecimal(
                FULL_DAG.toMillis()
            ), 8, RoundingMode.HALF_UP
        )
    }
}
