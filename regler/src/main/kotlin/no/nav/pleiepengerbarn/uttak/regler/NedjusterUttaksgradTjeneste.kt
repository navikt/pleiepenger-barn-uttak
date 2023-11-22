package no.nav.pleiepengerbarn.uttak.regler

import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.stream.Collectors

object NedjusterUttaksgradTjeneste {

    fun nedjusterUttaksgrad(uttaksgrunnlag: Uttaksgrunnlag, uttaksplan: Uttaksplan): Uttaksplan {
        val nedjustertUttaksgradTidslinje = lagTimeline(uttaksgrunnlag)
        if (nedjustertUttaksgradTidslinje.isEmpty) {
            return uttaksplan
        }

        val uttaksplanTidslinje = lagTimeline(uttaksplan)
        val resultatTidslinje = uttaksplanTidslinje.combine(
            nedjustertUttaksgradTidslinje,
            nedjusterSøkersUttaksgradCombinator,
            LocalDateTimeline.JoinStyle.LEFT_JOIN
        )
        val resultatPerioder = resultatTidslinje.toSegments().stream()
            .collect(Collectors.toMap({ s -> LukketPeriode(s.fom, s.tom) }, { s -> s.value }));
        return uttaksplan.copy(perioder = resultatPerioder)
    }

}

private val nedjusterSøkersUttaksgradCombinator = fun(
    di: LocalDateInterval,
    uttaksperiodeInfoSegment: LocalDateSegment<UttaksperiodeInfo>,
    uttaksgradSegment: LocalDateSegment<BigDecimal>?
): LocalDateSegment<UttaksperiodeInfo> {
    return if (uttaksgradSegment != null) {
        LocalDateSegment(
            di,
            uttaksperiodeInfoSegment.value.copy(
                uttaksgradMedReduksjonGrunnetInntektsgradering = uttaksgradSegment.value,
                uttaksgrad = uttaksgradSegment.value,
                uttaksgradUtenReduksjonGrunnetInntektsgradering = uttaksperiodeInfoSegment.value.uttaksgrad
            )
        )
    } else {
        LocalDateSegment(di, uttaksperiodeInfoSegment.value.copy())
    }
}

private fun lagTimeline(uttaksplan: Uttaksplan): LocalDateTimeline<UttaksperiodeInfo> {
    val segmenter = uttaksplan.perioder.map { (periode, info) -> LocalDateSegment(periode.fom, periode.tom, info) }
    return LocalDateTimeline(segmenter)
}

private fun lagTimeline(uttaksgrunnlag: Uttaksgrunnlag): LocalDateTimeline<BigDecimal> {
    val segmenter = uttaksgrunnlag.nedjustertSøkersUttaksgrad.map { (periode, info) ->
        LocalDateSegment(
            periode.fom,
            periode.tom,
            info.uttaksgrad.setScale(2, RoundingMode.HALF_UP)
        )
    }
    return LocalDateTimeline(segmenter)
}


