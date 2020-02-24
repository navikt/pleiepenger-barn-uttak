package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode
import java.time.LocalDate

internal fun LukketPeriode.overlapper(periode: LukketPeriode) : Boolean {
    if (fom.isEqual(periode.fom)) return true
    if (fom.isEqual(periode.tom)) return true
    if (tom.isEqual(periode.tom)) return true
    if (tom.isEqual(periode.fom)) return true
    return fom.isBefore(periode.tom) && tom.isAfter(periode.tom)
}

internal fun LukketPeriode.erLik(periode: LukketPeriode) = fom.isEqual(periode.fom) && tom.isEqual(periode.tom)
private fun LukketPeriode.erKantIKant(periode: LukketPeriode) = tom.plusDays(1).isEqual(periode.fom)

internal fun Map<LukketPeriode, *>.overordnetPeriode() : LukketPeriode {
    var fom : LocalDate? = null
    var tom : LocalDate? = null

    forEach { periode, _ ->
        if (fom == null || periode.fom.isBefore(fom)) {
            fom = periode.fom
        }
        if (tom == null || periode.tom.isAfter(tom)) {
            tom = periode.tom
        }
    }

    return LukketPeriode(fom!!, tom!!)
}

internal fun Map<LukketPeriode, *>.sortertPåFom() = toSortedMap(compareBy { it.fom.toEpochDay() })
internal fun Map<LukketPeriode, *>.sortertPåTom() = toSortedMap(compareBy { it.tom.toEpochDay() })

internal fun LukketPeriode.perioderSomIkkeInngårI(perioder: Map<LukketPeriode,*>) : List<LukketPeriode> {
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

internal fun List<LukketPeriode>.overlappendePeriode(uttaksperiode: Uttaksperiode) = find {
    (it.fom == uttaksperiode.periode.fom || it.fom.isBefore(uttaksperiode.periode.fom)) &&
    (it.tom == uttaksperiode.periode.tom || it.tom.isAfter(uttaksperiode.periode.tom))
}