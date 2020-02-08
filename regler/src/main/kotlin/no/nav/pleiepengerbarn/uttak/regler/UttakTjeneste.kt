package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan

object UttakTjeneste {

    fun uttaksplan(regelGrunnlag: RegelGrunnlag): Uttaksplan {
        val knekkpunkter = KnekkpunktUtleder.finnKnekkpunkter(regelGrunnlag)
        val uttaksplan = UttaksplanOppretter.opprettUttaksperioder(regelGrunnlag, knekkpunkter)
        return UttaksplanRegler.fastsettUttaksplan(uttaksplan, regelGrunnlag.arbeidsforhold.keys)
    }

}