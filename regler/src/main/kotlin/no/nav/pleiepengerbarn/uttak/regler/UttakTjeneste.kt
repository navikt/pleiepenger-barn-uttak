package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan

object UttakTjeneste {

    fun uttaksplan(regelGrunnlag: RegelGrunnlag): Uttaksplan {
        val uttaksplan = UttaksplanOppretter.opprettUttaksperioder(regelGrunnlag)
        val knekkpunkter = KnekkpunktOppretter.finnKnekkpunkter(regelGrunnlag)
        //TODO bruk knekkpunkt på uttaksplan
        val avklartUttaksplan = UttaksplanRegler.fastsettUttaksplan(uttaksplan,regelGrunnlag.arbeidsforhold.keys)
        return avklartUttaksplan
    }

}