package no.nav.pleiepengerbarn.uttak.regler

import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator
import no.nav.fpsak.tidsserie.LocalDateTimeline
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

    fun endreUttaksplan(eksisterendeUttaksplan: Uttaksplan, perioderSomErIkkeOppfylt: Map<LukketPeriode, Årsak>): Uttaksplan {
        val nyePerioder = eksisterendeUttaksplan.perioder.oppdaterPerioder(perioderSomErIkkeOppfylt)
        return eksisterendeUttaksplan.copy(perioder = nyePerioder)
    }


}

private fun Map<LukketPeriode, UttaksperiodeInfo>.oppdaterPerioder(perioderSomErIkkeOppfylt: Map<LukketPeriode, Årsak>): Map<LukketPeriode, UttaksperiodeInfo> {
    val uttakTimeline = LocalDateTimeline(map {LocalDateSegment(it.key.fom, it.key.tom, it.value)})
    val ikkeOppfyltTimeline = LocalDateTimeline(perioderSomErIkkeOppfylt.map {LocalDateSegment(it.key.fom, it.key.tom, it.value)})

    if (uttakTimeline.intersects(ikkeOppfyltTimeline)) {
        val nyePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()

        val uberørtePerioder = uttakTimeline.disjoint(ikkeOppfyltTimeline)
        uberørtePerioder.toSegments().forEach {
            nyePerioder[LukketPeriode(it.fom, it.tom)] = it.value
        }


        val ikkeOppfyltePerioder = uttakTimeline.intersection(ikkeOppfyltTimeline, UttaksperiodeCombinator::combine)
        ikkeOppfyltePerioder.toSegments().forEach {
            nyePerioder[LukketPeriode(it.fom, it.tom)] = it.value
        }

        return nyePerioder
    }
    return this
}

object UttaksperiodeCombinator : LocalDateSegmentCombinator<UttaksperiodeInfo, Årsak, UttaksperiodeInfo> {
    override fun combine(
        interval: LocalDateInterval,
        infoSegment: LocalDateSegment<UttaksperiodeInfo>,
        årsakSegment: LocalDateSegment<Årsak>
    ): LocalDateSegment<UttaksperiodeInfo> {
        val nyInfo = infoSegment.value.oppdaterÅrsaker(årsakSegment.value)
        return LocalDateSegment(interval, nyInfo)
    }
}



private fun UttaksperiodeInfo.oppdaterÅrsaker(årsak: Årsak): UttaksperiodeInfo {
    if (this.utfall == Utfall.IKKE_OPPFYLT) {
        val oppdaterteÅrsaker = this.årsaker.toMutableSet()
        oppdaterteÅrsaker.add(årsak)
        return this.copy(årsaker = oppdaterteÅrsaker)
    }
    val årsaker = setOf(årsak)
    val oppdaterteUtbetalingsgrader = this.utbetalingsgrader.map {it.copy(utbetalingsgrad = NULL_PROSENT)}
    return this.copy(utfall = Utfall.IKKE_OPPFYLT, uttaksgrad = NULL_PROSENT, årsaker = årsaker, utbetalingsgrader = oppdaterteUtbetalingsgrader)
}