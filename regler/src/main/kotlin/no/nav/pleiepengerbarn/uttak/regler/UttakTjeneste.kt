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

    fun erResultatEndret(forrigeUttaksplan: Uttaksplan?, simulertUttaksplan: Uttaksplan): Boolean {
        if (forrigeUttaksplan == null) {
            throw IllegalStateException("Fant ingen uttaksplan å sammenligne med")
        }
        return simulertUttaksplan.perioder.any { (simulertPeriode, simulertInfo) ->
            val forrigeInfo = forrigeUttaksplan.perioder[simulertPeriode]
                ?: return true // Perioden fantes ikke i forrige uttaksplan
            if (!simulertInfo.sammenlign(forrigeInfo)) {
                return true // Perioden har forskjellig resultat
            }
            return false // Resultatet er uendret
        }
    }

}

private fun UttaksperiodeInfo.sammenlign(annenUttaksperiodeInfo: UttaksperiodeInfo): Boolean {
    if (this.utfall != annenUttaksperiodeInfo.utfall) {
        return false
    }
    if (this.uttaksgrad.compareTo(annenUttaksperiodeInfo.uttaksgrad) != 0) {
        return false
    }
    val andreUtbetalingsgrader = annenUttaksperiodeInfo.utbetalingsgrader.tilMap()
    val utbetalingsgrader = this.utbetalingsgrader.tilMap()
    for (arbeidsforhold in utbetalingsgrader.keys) {
        if (utbetalingsgrader[arbeidsforhold]!!.compareTo(andreUtbetalingsgrader[arbeidsforhold]) != 0) {
            return false
        }
    }
    if (this.årsaker != annenUttaksperiodeInfo.årsaker) {
        return false
    }

    return true // Ingen forskjeller
}


private fun List<Utbetalingsgrader>.tilMap(): Map<Arbeidsforhold, Prosent> {
    val utbetalingsgradMap = mutableMapOf<Arbeidsforhold, Prosent>()
    this.forEach { utbetalingsgradMap[it.arbeidsforhold] = it.utbetalingsgrad }
    return utbetalingsgradMap
}
