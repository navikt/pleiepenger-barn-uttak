package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.springframework.util.LinkedMultiValueMap
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun Uttaksplan.tilForenkletUttaksplan(): ForenkletUttaksplan {
    val arbeidsforholdSet = mutableSetOf<Arbeidsforhold>()
    perioder
        .filter { it.value.utfall == Utfall.OPPFYLT }
        .map { it.value }
        .forEach { it.utbetalingsgrader.forEach { utbetalingsgrad -> arbeidsforholdSet.add(utbetalingsgrad.arbeidsforhold) } }

    val aktiviteter = arbeidsforholdSet.map { arbeidsforhold ->
        Aktivitet(
            arbeidsforhold = arbeidsforhold,
            uttaksperioder = perioder.map { it.value.tilForenkletUttaksperiode(it.key, arbeidsforhold) }
        )
    }
    return ForenkletUttaksplan(aktiviteter = aktiviteter)
}

private fun UttaksperiodeInfo.tilForenkletUttaksperiode(periode: LukketPeriode, arbeidsforhold: Arbeidsforhold): ForenkletUttaksperiode {
    val periodeInfo = this
    return when (periodeInfo.utfall) {
        Utfall.OPPFYLT -> {
            ForenkletUttaksperiode(periode = periode, oppfylt = true, utbetalingsgrad = periodeInfo.utbetalingsgrader.finnFor(arbeidsforhold))
        }
        Utfall.IKKE_OPPFYLT -> {
            ForenkletUttaksperiode(periode = periode, oppfylt = false, utbetalingsgrad = Prosent.ZERO)
        }
    }
}

private fun List<Utbetalingsgrader>.finnFor(arbeidsforhold: Arbeidsforhold): Prosent {
    return find { it.arbeidsforhold == arbeidsforhold } ?.utbetalingsgrad ?: Prosent.ZERO
}



internal fun Uttaksplan.slåSammenLikePerioder(): Uttaksplan {

    //Samle alle perioder som har samme info
    val perioderMedLikInfo = LinkedMultiValueMap<UttaksperiodeInfo, LukketPeriode>()
    this.perioder.forEach { (periode, info) ->
        perioderMedLikInfo.add(info, periode)
    }

    //Slå sammen perioder og bygg nye perioder
    val nyePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()
    perioderMedLikInfo.forEach { (info, perioder) ->
        val sammenslåttePerioder = perioder.slåSammenDersomBareHelgMellom()
        sammenslåttePerioder.forEach { sammenslåttPeriode ->
            nyePerioder[sammenslåttPeriode] = info
        }
    }

    return Uttaksplan(perioder = nyePerioder)
}

private fun List<LukketPeriode>.slåSammenDersomBareHelgMellom(): List<LukketPeriode> {
    val sortertePerioder = this.sortedBy { it.fom }

    var nyPeriode: LukketPeriode? = null
    val nyePerioder = mutableListOf<LukketPeriode>()

    sortertePerioder.forEach { periode ->
        if (nyPeriode == null) {
            nyPeriode = periode
        } else if (bareHelgMellom(nyPeriode!!.tom, periode.fom)) {
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

internal fun bareHelgMellom(dato1: LocalDate, dato2: LocalDate): Boolean {
    require(dato1 < dato2) {"Dato1($dato1) må være før dato2($dato2)."}
    if (ChronoUnit.DAYS.between(dato1, dato2) == 3L) {
        if (dato1.dayOfWeek == DayOfWeek.FRIDAY) {
            return true
        }
    }
    return false
}