package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.springframework.util.LinkedMultiValueMap
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun Uttaksplan.slåSammenLikePerioder(): Uttaksplan {

    //Samle alle perioder som har samme info
    val perioderMedLikInfo = LinkedMultiValueMap<UttaksperiodeInfo, LukketPeriode>()
    this.perioder.forEach { (periode, info) ->
        // Fjerner knekkpunkter når perioder skal slås sammen, siden de bare er det for sporing og ikke skal føre til at
        // periodene blir oppfattet som forskjellige.
        val infoUtenKnekkpunkter = info.copy(knekkpunktTyper = setOf())
        perioderMedLikInfo.add(infoUtenKnekkpunkter, periode)
    }

    //Slå sammen perioder og bygg nye perioder
    val nyePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()
    perioderMedLikInfo.forEach { (info, perioder) ->
        val sammenslåttePerioder = perioder.slåSammen()
        sammenslåttePerioder.forEach { sammenslåttPeriode ->
            nyePerioder[sammenslåttPeriode] = info
        }
    }

    return Uttaksplan(perioder = nyePerioder, trukketUttak = this.trukketUttak, kvoteInfo = this.kvoteInfo, commitId = this.commitId)
}

private fun List<LukketPeriode>.slåSammen(): List<LukketPeriode> {
    val sortertePerioder = this.sortedBy { it.fom }

    var nyPeriode: LukketPeriode? = null
    val nyePerioder = mutableListOf<LukketPeriode>()

    sortertePerioder.forEach { periode ->
        if (nyPeriode == null) {
            nyPeriode = periode
        } else if (bareHelgEllerIngenDagerMellom(nyPeriode!!.tom, periode.fom)) {
            nyPeriode = LukketPeriode(nyPeriode!!.fom, periode.tom)
        } else {
            nyePerioder.add(nyPeriode!!)
            nyPeriode = periode
        }
    }
    if (nyPeriode != null) {
        nyePerioder.add(nyPeriode!!)
    }

    return nyePerioder
}

internal fun bareHelgEllerIngenDagerMellom(dato1: LocalDate, dato2: LocalDate): Boolean {
    require(dato1 < dato2) {"Dato1($dato1) må være før dato2($dato2)."}
    val dagerMellom = ChronoUnit.DAYS.between(dato1, dato2)
    if (dagerMellom == 3L) {
        if (dato1.dayOfWeek == DayOfWeek.FRIDAY) {
            return true
        }
    }
    if (dagerMellom == 1L) {
        return true
    }
    return false
}