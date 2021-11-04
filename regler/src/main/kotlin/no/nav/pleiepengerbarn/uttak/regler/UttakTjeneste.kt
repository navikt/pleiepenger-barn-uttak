package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag

object UttakTjeneste {

    fun uttaksplan(grunnlag: RegelGrunnlag): Uttaksplan {
        val knekkpunkter = KnekkpunktUtleder.finnKnekkpunkter(
                regelGrunnlag = grunnlag
        )

        val søktUttakUtenHelger = Helger.fjern(grunnlag.søktUttak)
        val oppdatertGrunnlag = grunnlag.copy(søktUttak = søktUttakUtenHelger)

        val knektePerioder = PeriodeKnekker.knekk(
                søktUttak = søktUttakUtenHelger,
                knekkpunkter = knekkpunkter
        )

        return UttaksplanRegler.fastsettUttaksplan(
                grunnlag = oppdatertGrunnlag,
                knektePerioder = knektePerioder
        )
    }

}
