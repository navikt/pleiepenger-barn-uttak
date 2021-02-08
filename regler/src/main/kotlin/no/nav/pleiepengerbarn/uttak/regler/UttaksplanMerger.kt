package no.nav.pleiepengerbarn.uttak.regler

import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.pleiepengerbarn.uttak.kontrakter.*

object UttaksplanMerger {

    fun slåSammenUttaksplaner(forrigeUttaksplan: Uttaksplan, nyUttaksplan: Uttaksplan): Uttaksplan {
        val segmenterForrigeUttaksplan = lagSegmenter(forrigeUttaksplan)
        val segmenterNyUttaksplan = lagSegmenter(nyUttaksplan)
        return lagSammenslåttUttaksplan(segmenterForrigeUttaksplan, segmenterNyUttaksplan)
    }

    private fun lagSegmenter(uttaksplan:Uttaksplan): List<LocalDateSegment<UttaksperiodeInfo>> {
        return uttaksplan.perioder.map { (periode, info) -> LocalDateSegment(periode.fom, periode.tom, info) }
    }

    private fun lagSammenslåttUttaksplan(segmenterForrigeUttaksplan: List<LocalDateSegment<UttaksperiodeInfo>>, segmenterNyUttaksplan: List<LocalDateSegment<UttaksperiodeInfo>>): Uttaksplan {
        val timelineGammelUttaksplan = LocalDateTimeline(segmenterForrigeUttaksplan)
        val timelineNyUttaksplan = LocalDateTimeline(segmenterNyUttaksplan)
        val sammenslåttTimeline = timelineNyUttaksplan.crossJoin(timelineGammelUttaksplan).compress()
        val perioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()
        sammenslåttTimeline.toSegments().forEach {
            perioder[LukketPeriode(it.fom, it.tom)] = it.value
        }
        return Uttaksplan(perioder)
    }

}