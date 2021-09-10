package no.nav.pleiepengerbarn.uttak.regler

import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.pleiepengerbarn.uttak.kontrakter.*

object UttaksplanMerger {

    fun slåSammenUttaksplaner(forrigeUttaksplan: Uttaksplan, nyUttaksplan: Uttaksplan, trukketUttak: List<LukketPeriode>): Uttaksplan {
        val timelineForrigeUttaksplan = lagTimeline(forrigeUttaksplan)
        val timelineForrigeUttaksplanMinusTrukketUttak = fjernTrukketUttak(timelineForrigeUttaksplan, trukketUttak)
        val timelineNyUttaksplan = lagTimeline(nyUttaksplan)
        val uttaksperioder = lagSammenslåttUttaksplan(timelineForrigeUttaksplanMinusTrukketUttak, timelineNyUttaksplan)
        return Uttaksplan(perioder = uttaksperioder, trukketUttak = trukketUttak)
    }

    private fun lagTimeline(uttaksplan:Uttaksplan): LocalDateTimeline<UttaksperiodeInfo> {
        val segmenter = uttaksplan.perioder.map { (periode, info) -> LocalDateSegment(periode.fom, periode.tom, info) }
        return LocalDateTimeline(segmenter)
    }

    private fun lagSammenslåttUttaksplan(timelineForrigeUttaksplan: LocalDateTimeline<UttaksperiodeInfo>, timelineNyUttaksplan: LocalDateTimeline<UttaksperiodeInfo>): Map<LukketPeriode, UttaksperiodeInfo> {
        val sammenslåttTimeline = timelineNyUttaksplan.crossJoin(timelineForrigeUttaksplan).compress()
        val perioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()
        sammenslåttTimeline.toSegments().forEach {
            perioder[LukketPeriode(it.fom, it.tom)] = it.value
        }
        return perioder
    }

    private fun fjernTrukketUttak(timelineForrigeUttaksplan: LocalDateTimeline<UttaksperiodeInfo>, trukketUttak: List<LukketPeriode>): LocalDateTimeline<UttaksperiodeInfo> {
        val timelineTrukketUttak = LocalDateTimeline(trukketUttak.map { LocalDateSegment(it.fom, it.tom, null) })
        return timelineForrigeUttaksplan.disjoint(timelineTrukketUttak)
    }

}


