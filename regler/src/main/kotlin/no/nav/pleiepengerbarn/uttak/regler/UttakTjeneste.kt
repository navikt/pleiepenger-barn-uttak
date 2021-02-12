package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan

object UttakTjeneste {

    fun uttaksplan(grunnlag: RegelGrunnlag): Uttaksplan {
        val knekkpunkter = KnekkpunktUtleder.finnKnekkpunkter(
                regelGrunnlag = grunnlag
        )

        val knektePerioder = PeriodeKnekker.knekk(
                søktUttak = grunnlag.søktUttak,
                knekkpunkter = knekkpunkter
        )

        return UttaksplanRegler.fastsettUttaksplan(
                grunnlag = grunnlag,
                knektePerioder = knektePerioder
        )
    }
}