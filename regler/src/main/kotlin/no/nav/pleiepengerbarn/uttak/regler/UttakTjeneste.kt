package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksplanV2

object UttakTjeneste {

    fun uttaksplan(grunnlag: RegelGrunnlag): UttaksplanV2 {
        val knekkpunkter = KnekkpunktUtleder.finnKnekkpunkter(
                regelGrunnlag = grunnlag
        )

        val knektePerioder = PeriodeKnekker.knekk(
                søknadsperioder = grunnlag.søknadsperioder,
                knekkpunkter = knekkpunkter
        )
        return UttaksplanRegler.fastsettUtaksplan(
                grunnlag = grunnlag,
                knektePerioder = knektePerioder
        )
    }
}