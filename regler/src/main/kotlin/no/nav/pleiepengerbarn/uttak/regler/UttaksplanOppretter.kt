package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*


object UttaksplanOppretter {

    fun opprettUttaksperioder(avklarteFakta: AvklarteFakta): Uttaksplan {
        val perioder = mutableListOf<Uttaksperiode>()
        for (p in avklarteFakta.søktePerioder) {
            perioder.add(Uttaksperiode(fom = p.fom, tom =  p.tom))
        }
        return Uttaksplan(perioder)
    }


}
