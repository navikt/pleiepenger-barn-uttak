package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode

internal fun LukketPeriode.overlapper(periode: LukketPeriode) : Boolean {
    if (fom.isEqual(periode.fom)) return true
    if (fom.isEqual(periode.tom)) return true
    if (tom.isEqual(periode.tom)) return true
    if (tom.isEqual(periode.fom)) return true
    return fom.isBefore(periode.tom) && tom.isAfter(periode.tom)
}

internal fun LukketPeriode.erLik(periode: LukketPeriode) = fom.isEqual(periode.fom) && tom.isEqual(periode.tom)
private fun LukketPeriode.erKantIKant(periode: LukketPeriode) = tom.plusDays(1).isEqual(periode.fom)

internal fun List<LukketPeriode>.overordnetPeriode() = LukketPeriode(
        fom = sortertPåFom().first().fom,
        tom = sortertPåTom().last().tom
)

internal fun <T> Map<LukketPeriode, T>.sortertPåFom() = toSortedMap(compareBy { it.fom })
internal fun <T> Map<LukketPeriode, T>.sortertPåTom() = toSortedMap(compareBy { it.tom })
internal fun List<LukketPeriode>.sortertPåFom() = sortedBy { it.fom }
internal fun List<LukketPeriode>.sortertPåTom() = sortedBy { it.tom }


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
    (it.fom == periode.fom || it.fom.isBefore(periode.fom)) &&
    (it.tom == periode.tom || it.tom.isAfter(periode.tom))
}