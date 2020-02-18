package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*


internal object UttaksplanOppretter {

    fun opprettUttaksperioder(regelGrunnlag: RegelGrunnlag, knekkpunkter: List<Knekkpunkt>): Uttaksplan {
        val perioder = mutableListOf<Uttaksperiode>()
        regelGrunnlag.søknadsperioder.forEach { p ->
            perioder.add(Uttaksperiode(LukketPeriode(p.fom, p.tom)))
        }

        return Uttaksplan(knekkUttaksperioder(knekkpunkter, perioder))
    }

    private fun knekkUttaksperioder(knekkpunkter: List<Knekkpunkt>, perioder:List<Uttaksperiode>):List<Uttaksperiode> {
        val knektePerioder = mutableListOf<Uttaksperiode>()
        perioder.forEach {
            knektePerioder.addAll(it.knekk(knekkpunkter))
        }
        return knektePerioder
    }

}
