package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksPeriodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.inneholder
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom
import java.time.LocalDate

object UttaksplanMerger {

    fun slåSammenUttaksplaner(uttaksplaner:List<Uttaksplan>):Uttaksplan {
        require(uttaksplaner.isNotEmpty()) {"Det må finnes minst en uttaksplan"}
        if (uttaksplaner.size == 1) {
            return uttaksplaner[0]
        }

        val nyePerioder = mutableMapOf<LukketPeriode, UttaksPeriodeInfo>()


        //TODO Sørg for at uttaksplaner er sortert slik at nyeste plan kommer første
        //TODO Sjekk om det er minst en uttaksperioder, hvis ikke returner en tom plan


        var (start, index) = finnFørsteUttaksdato(uttaksplaner)
        val slutt = finnSisteUttaksdato(uttaksplaner)

        require(!start.isAfter(slutt)) {"FOM(${start}) kan ikke være etter TOM(${slutt})"}

        var dato = start
        var startPeriode:LocalDate? = start
        var sisteOverlappendePeriode: Uttaksperiode? = null
        while(!dato.isAfter(slutt)) {
            
            val (overlappendePeriode, overlappendeIndex) = finnFørsteOverlapp(dato.plusDays(1), uttaksplaner)

            if (sisteOverlappendePeriode == null) {
                sisteOverlappendePeriode = overlappendePeriode
            }

            if (startPeriode != null) {
                if (overlappendeIndex == null) {
                    if (sisteOverlappendePeriode != null) {
                        nyePerioder[sisteOverlappendePeriode.key.copy(fom = startPeriode, tom = dato)] = sisteOverlappendePeriode.value
                        startPeriode = null
                    }
                    sisteOverlappendePeriode = null
                } else if (overlappendeIndex == index) {
                    //Ingenting å gjøre
                } else {
                    nyePerioder[sisteOverlappendePeriode?.key?.copy(fom = startPeriode, tom = dato)!!] = sisteOverlappendePeriode.value
                    index = overlappendeIndex
                    sisteOverlappendePeriode = overlappendePeriode
                    startPeriode = dato.plusDays(1)
                }

            } else {
                if (overlappendeIndex != null) {
                    startPeriode = dato
                    index = overlappendeIndex
                }
            }
            dato = dato.plusDays(1)
        }

        return Uttaksplan(nyePerioder)
    }


    private fun finnFørsteUttaksdato(uttaksplaner:List<Uttaksplan>): Pair<LocalDate, Int> {
        var førsteUttaksdato = LocalDate.MAX
        var førsteUttaksdatoIndex = 0
        var index = 0
        uttaksplaner.forEach {
            val uttaksplansFørsteUttaksdato = it.perioder.sortertPåFom().firstKey().fom
            if (uttaksplansFørsteUttaksdato < førsteUttaksdato) {
                førsteUttaksdato = uttaksplansFørsteUttaksdato
                førsteUttaksdatoIndex = index
            }
            index++
        }
        return Pair(førsteUttaksdato, førsteUttaksdatoIndex)
    }

    private fun finnSisteUttaksdato(uttaksplaner:List<Uttaksplan>): LocalDate {
        var sisteUttaksdato = LocalDate.MIN
        uttaksplaner.forEach {
            val uttaksplansSisteUttaksdato = it.perioder.sortertPåFom().lastKey().tom
            if (uttaksplansSisteUttaksdato > sisteUttaksdato) {
                sisteUttaksdato = uttaksplansSisteUttaksdato
            }
        }
        return sisteUttaksdato
    }

    private fun finnFørsteOverlapp(dato:LocalDate, uttaksplaner: List<Uttaksplan>):Pair<Uttaksperiode?, Int?> {
        uttaksplaner.forEachIndexed {index, uttaksplan ->
            uttaksplan.perioder.forEach {periode ->
                if (periode.key.inneholder(dato)) {
                    return Pair(periode, index)
                }
            }
        }
        return Pair(null, null)
    }


}