package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak
import java.lang.IllegalArgumentException
import java.time.DayOfWeek
import java.time.LocalDate

internal fun LukketPeriode.overlapperHelt(annen: LukketPeriode) =
        (fom == annen.fom || fom.isBefore(annen.fom)) &&
        (tom == annen.tom || tom.isAfter(annen.tom))

internal fun LukketPeriode.overlapperDelvis(annen: LukketPeriode) =
        (fom == annen.tom || fom.isBefore(annen.tom)) &&
        (tom == annen.fom || tom.isAfter(annen.fom))


internal fun <T> Map<LukketPeriode, T>.sortertPåFom() = toSortedMap(compareBy { it.fom })
internal fun Collection<SøktUttak>.sortertPåFom() = sortedBy { it.periode.fom }

fun Collection<LukketPeriode>.sjekkOmOverlapp(): Boolean {
    val segmenter = map { LocalDateSegment(it.fom, it.tom, true) }
    try {
        LocalDateTimeline(segmenter)
    } catch (e: IllegalArgumentException) {
        return true
    }
    return false
}

internal fun LukketPeriode.virkedager(): Int {
    var nåværende = fom
    var antall = 0
    while (!nåværende.isAfter(tom)) {
        if (nåværende.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            antall++
        }
        nåværende = nåværende.plusDays(1)
    }
    return antall
}

internal fun LukketPeriode.tilVirkedager(): Set<LocalDate> {
    val datoer = mutableSetOf<LocalDate>()
    this.stream().forEach {
        if (it.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            datoer.add(it)
        }
    }
    return datoer
}

internal fun LukketPeriode.stream() = fom.datesUntil(tom.plusDays(1))
