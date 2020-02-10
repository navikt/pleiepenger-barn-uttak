package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan

object UttakTjeneste {

    fun uttaksplan(grunnlag: RegelGrunnlag): Uttaksplan {
        val knekkpunkter = KnekkpunktUtleder.finnKnekkpunkter(grunnlag)
        val uttaksplan = UttaksplanOppretter.opprettUttaksperioder(grunnlag, knekkpunkter)
        return UttaksplanRegler.fastsettUttaksplan(uttaksplan, grunnlag)
    }

}