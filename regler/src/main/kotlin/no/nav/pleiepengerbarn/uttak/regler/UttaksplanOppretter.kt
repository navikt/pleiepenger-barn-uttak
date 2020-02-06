package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*


object UttaksplanOppretter {

    fun opprettUttaksperioder(regelGrunnlag: RegelGrunnlag): Uttaksplan {
        val perioder = mutableListOf<Uttaksperiode>()
        for (p in regelGrunnlag.søktePerioder) {
            perioder.add(Uttaksperiode(fom = p.fom, tom =  p.tom))
        }
        return Uttaksplan(perioder)
    }


}
