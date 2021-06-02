package no.nav.pleiepengerbarn.uttak.regler

import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

internal object Helger {

    internal fun fjern(søktUttakListe: List<SøktUttak>): List<SøktUttak> {
        val søktUttakSegmenter = søktUttakListe.map {søktUttak -> LocalDateSegment(søktUttak.periode.fom, søktUttak.periode.tom, søktUttak.oppgittTilsyn)}
        val helgeSegmenter = finnHelger(søktUttakListe).map {helg -> LocalDateSegment(helg.fom, helg.tom, null)}
        val timelineUtenHelger = LocalDateTimeline(søktUttakSegmenter).disjoint(LocalDateTimeline(helgeSegmenter))

        return timelineUtenHelger.toSegments().map { segment -> SøktUttak(LukketPeriode(segment.fom, segment.tom), segment.value) }
    }

    private fun finnHelger(søktUttakListe: List<SøktUttak>): List<LukketPeriode> {
        if (søktUttakListe.isEmpty()) {
            return listOf()
        }
        val minFom = søktUttakListe.minOf { it.periode.fom }
        val maxTom = søktUttakListe.maxOf { it.periode.tom }
        val helger = mutableListOf<LukketPeriode>()
        if (minFom.dayOfWeek == DayOfWeek.SATURDAY) {
            helger.add(LukketPeriode(minFom, minFom.plusDays(1)))
        } else if (minFom.dayOfWeek == DayOfWeek.SUNDAY) {
            helger.add(LukketPeriode(minFom, minFom))
        }
        var dato = minFom
        while (dato <= maxTom) {
            val startHelg = dato.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
            helger.add(LukketPeriode(startHelg, startHelg.plusDays(1)))
            dato = startHelg.plusDays(2)
        }
        return helger
    }

}