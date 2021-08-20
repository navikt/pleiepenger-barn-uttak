package no.nav.pleiepengerbarn.uttak.regler

import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.pleiepengerbarn.uttak.kontrakter.*

object UttaksplanMerger {

    fun slåSammenUttaksplaner(forrigeUttaksplan: Uttaksplan, nyUttaksplan: Uttaksplan): Uttaksplan {
        val timelineForrigeUttaksplan = lagTimeline(forrigeUttaksplan)
        val timelineNyUttaksplan = lagTimeline(nyUttaksplan)
        return  lagSammenslåttUttaksplan(timelineForrigeUttaksplan, timelineNyUttaksplan)
    }

    private fun lagTimeline(uttaksplan:Uttaksplan): LocalDateTimeline<UttaksperiodeInfo> {
        val segmenter = uttaksplan.perioder.map { (periode, info) -> LocalDateSegment(periode.fom, periode.tom, info) }
        return LocalDateTimeline(segmenter)
    }

    private fun lagSammenslåttUttaksplan(timelineForrigeUttaksplan: LocalDateTimeline<UttaksperiodeInfo>, timelineNyUttaksplan: LocalDateTimeline<UttaksperiodeInfo>): Uttaksplan {
        val sammenslåttTimeline = timelineNyUttaksplan.crossJoin(timelineForrigeUttaksplan).compress()
        val perioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()
        sammenslåttTimeline.toSegments().forEach {
            perioder[LukketPeriode(it.fom, it.tom)] = it.value
        }
        return Uttaksplan(perioder)
    }

}