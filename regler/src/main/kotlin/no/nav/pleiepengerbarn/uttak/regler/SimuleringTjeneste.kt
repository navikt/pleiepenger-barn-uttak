package no.nav.pleiepengerbarn.uttak.regler

import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.pleiepengerbarn.uttak.kontrakter.*

object SimuleringTjeneste {

    fun erResultatEndret(forrigeUttaksplan: Uttaksplan?, simulertUttaksplan: Uttaksplan): Boolean {
        if (forrigeUttaksplan == null) {
            return true //Alt er nytt, derfor endret
        }
        return forrigeUttaksplan.tilForenkletUttaksplan() != simulertUttaksplan.tilForenkletUttaksplan()
    }

}

private fun Uttaksplan.tilForenkletUttaksplan(): ForenkletUttaksplan {

    val perioder = mutableMapOf<LukketPeriode, ForenkletUttakInfo>()
    this.perioder.forEach {
        val utbetalingsgrader = mutableMapOf<Arbeidsforhold, ForenkletUttaksgrad>()
        it.value.utbetalingsgrader.forEach { utbetalingsgrad -> utbetalingsgrader[utbetalingsgrad.arbeidsforhold] = ForenkletUttaksgrad(utbetalingsgrad.utbetalingsgrad) }
        perioder[it.key] = ForenkletUttakInfo(
           uttaksgrad = it.value.uttaksgrad,
           utfall = it.value.utfall,
           årsaker = it.value.årsaker,
           utbetalingsgrader = utbetalingsgrader
        )
    }

    return ForenkletUttaksplan(perioder = perioder.slåSammenPerioderMedLikInfo())
}

private fun Map<LukketPeriode, ForenkletUttakInfo>.slåSammenPerioderMedLikInfo(): Map<LukketPeriode, ForenkletUttakInfo> {
    val segmenter = this.map {LocalDateSegment(it.key.fom, it.key.tom, it.value)}
    val timeline = LocalDateTimeline(segmenter)
    val komprimertTimeline = timeline.compress()
    val komprimertePerioder = mutableMapOf<LukketPeriode, ForenkletUttakInfo>()
    komprimertTimeline.toSegments().forEach { komprimertePerioder[LukketPeriode(it.fom, it.tom)] = it.value }
    return komprimertePerioder
}


// NB: data classes med BigDecimal/Prosent må sammenlignes med compareTo
private data class ForenkletUttaksplan(val perioder: Map<LukketPeriode, ForenkletUttakInfo>)
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

