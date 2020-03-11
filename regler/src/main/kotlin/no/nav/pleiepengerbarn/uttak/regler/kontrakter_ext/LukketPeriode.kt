package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate

internal fun LukketPeriode.overlapper(annen: LukketPeriode) =
        (fom == annen.fom || fom.isBefore(annen.fom)) &&
        (tom == annen.tom || tom.isAfter(annen.tom))

internal fun LukketPeriode.erLik(periode: LukketPeriode) = fom.isEqual(periode.fom) && tom.isEqual(periode.tom)
private fun LukketPeriode.erKantIKant(periode: LukketPeriode) = tom.plusDays(1).isEqual(periode.fom)

internal fun Collection<LukketPeriode>.overordnetPeriode() = LukketPeriode(
        fom = sortertPåFom().first().fom,
        tom = sortertPåTom().last().tom
)

internal fun <T> Map<LukketPeriode, T>.sortertPåFom() = toSortedMap(compareBy { it.fom })
internal fun <T> Map<LukketPeriode, T>.sortertPåTom() = toSortedMap(compareBy { it.tom })
internal fun Collection<LukketPeriode>.sortertPåFom() = sortedBy { it.fom }
internal fun Collection<LukketPeriode>.sortertPåTom() = sortedBy { it.tom }


internal fun <T>LukketPeriode.perioderSomIkkeInngårI(perioder: Map<LukketPeriode, T>) : List<LukketPeriode> {
    if (perioder.isEmpty()) return listOf(this)

    val inngårIkke = mutableListOf<LukketPeriode>()
    val sortertPåFom = perioder.sortertPåFom()
    val førsteDag = sortertPåFom.firstKey().fom
    val sisteDag = perioder.sortertPåTom().lastKey().tom


    // Potenseielt første periode før periodene starter
    if (fom.isBefore(førsteDag)) {
        inngårIkke.add(LukketPeriode(
                fom = fom,
                tom = førsteDag.minusDays(1)
        ))
    }
    // Potensielt siste periode etter at periodene slutter
    if (tom.isAfter(sisteDag)) {
        inngårIkke.add(LukketPeriode(
                fom = sisteDag.plusDays(1),
                tom = tom
        ))
    }

    // Perioder mellom periodene
    var forrigePeriode = sortertPåFom.firstKey()
    sortertPåFom.forEach { (nåværendePeriode, _) ->
        if (!forrigePeriode.erLik(nåværendePeriode) && !forrigePeriode.erKantIKant(nåværendePeriode)) {
            inngårIkke.add(LukketPeriode(
                    fom = forrigePeriode.tom.plusDays(1),
                    tom = nåværendePeriode.fom.minusDays(1)
            ))
        }
        forrigePeriode = nåværendePeriode
    }
    return inngårIkke.toList()
}

internal fun List<LukketPeriode>.overlappendePeriode(periode: LukketPeriode) = find {
    it.overlapper(periode)
}

internal fun LukketPeriode.inneholder(dato: LocalDate) = fom.isEqual(dato) || tom.isEqual(dato) || (dato.isAfter(fom) && dato.isBefore(tom))

internal fun Collection<LukketPeriode>.inneholder(dato: LocalDate) = firstOrNull {
    it.inneholder(dato)
}

internal fun <T> Map<LukketPeriode, T>.inneholder(dato: LocalDate) = filterKeys {
    it.inneholder(dato)
}.entries.firstOrNull()

internal fun LukketPeriode.antallVirkedager(): Long {
    var nåværende = fom
    var antall = 0L
    while (!nåværende.isAfter(tom)) {
        if (nåværende.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            antall++
        }
        nåværende = nåværende.plusDays(1)
    }
    return antall
}

internal fun LocalDate.erLikEllerEtter(annen: LocalDate) = isEqual(annen) || isAfter(annen)

internal fun Duration.somTekst() : String {
    if (equals(Duration.ZERO)) return "0 timer"
    val timer = seconds / 3600
    val minutter = (seconds % 3600) / 60
    val sekunder = seconds % 60

    val timerTekst = when (timer) {
        0L -> null
        1L -> "$timer time"
        else -> "$timer timer"
    }
    val minutterTekst = when (minutter) {
        0L -> null
        1L -> "$minutter minutt"
        else -> "$minutter minutter"
    }
    val sekunderTekst = when (sekunder) {
        0L -> null
        1L -> "$sekunder sekund"
        else -> "$sekunder sekunder"
    }

    return listOfNotNull(timerTekst, minutterTekst, sekunderTekst).joinToString(" ")
}