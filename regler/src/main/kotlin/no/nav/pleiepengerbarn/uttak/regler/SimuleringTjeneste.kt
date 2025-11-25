package no.nav.pleiepengerbarn.uttak.regler

import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.slf4j.LoggerFactory

object SimuleringTjeneste {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun erResultatEndret(forrigeUttaksplan: Uttaksplan?, simulertUttaksplan: Uttaksplan): Boolean {
        if (forrigeUttaksplan == null) {
            logger.info("Uttaksimulering: Alt er nytt, derfor endret")
            return true
        }
        val forrigeUttaksplanForenklet = forrigeUttaksplan.tilForenkletUttaksplan()
        val simulertUttaksplanForenklet = simulertUttaksplan.tilForenkletUttaksplan()
        if (forrigeUttaksplanForenklet == simulertUttaksplanForenklet){
            logger.info("Uttaksimulering: Uendret")
            return false;
        }
        val perioderMedFjernedeVerdier = forrigeUttaksplanForenklet.disjoint(simulertUttaksplanForenklet).compress().toSegments().map { it.localDateInterval }
        val perioderMedNyeVerdier = simulertUttaksplanForenklet.disjoint(forrigeUttaksplanForenklet).compress().toSegments().map { it.localDateInterval }
        val perioderMedEndredeVerdier = simulertUttaksplanForenklet.combine(forrigeUttaksplanForenklet, { intervall, lhs, rhs -> LocalDateSegment(intervall, lhs.value != rhs.value) }, LocalDateTimeline.JoinStyle.INNER_JOIN)
            .compress()
            .filterValue { it == true }
            .toSegments().map { it.localDateInterval }
        logger.info("Uttaksimulering: Har endringer. Nye perioder: ${perioderMedNyeVerdier}, fjernede perioder: ${perioderMedFjernedeVerdier}, endrede perioder: ${perioderMedEndredeVerdier}")
        return true;
    }

}

private fun Uttaksplan.tilForenkletUttaksplan(): LocalDateTimeline<ForenkletUttakInfo> {
    val ukomprimertTidslinje = LocalDateTimeline(this.perioder.map {
        LocalDateSegment(
            it.key.fom,
            it.key.tom,
            ForenkletUttakInfo(
                uttaksgrad = it.value.uttaksgrad,
                utfall = it.value.utfall,
                årsaker = it.value.årsaker,
                utbetalingsgrader = mutableMapOf<Arbeidsforhold, ForenkletUttaksgrad>() //TODO hvorfor settes denne til et tomt map?
            )
        )
    })
    return ukomprimertTidslinje.compress() //viktig her, slår sammen intilliggende perioder med lik info
}


// NB: data classes med BigDecimal/Prosent må sammenlignes med compareTo
private data class ForenkletUttakInfo(val uttaksgrad: Prosent, val utfall: Utfall, val årsaker: Set<Årsak>,  val utbetalingsgrader: Map<Arbeidsforhold, ForenkletUttaksgrad>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ForenkletUttakInfo

        if (uttaksgrad.compareTo(other.uttaksgrad) != 0) return false
        if (utfall != other.utfall) return false
        if (årsaker != other.årsaker) return false
        if (utbetalingsgrader != other.utbetalingsgrader) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uttaksgrad.hashCode()
        result = 31 * result + utfall.hashCode()
        result = 31 * result + årsaker.hashCode()
        result = 31 * result + utbetalingsgrader.hashCode()
        return result
    }
}

private data class ForenkletUttaksgrad(val utbetalingsgrad: Prosent) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ForenkletUttaksgrad

        if (utbetalingsgrad.compareTo(other.utbetalingsgrad) != 0) return false

        return true
    }

    override fun hashCode(): Int {
        return utbetalingsgrad.hashCode()
    }
}

