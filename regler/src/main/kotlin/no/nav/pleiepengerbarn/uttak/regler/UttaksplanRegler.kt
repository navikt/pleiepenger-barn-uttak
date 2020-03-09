package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.delregler.Avslått
import no.nav.pleiepengerbarn.uttak.regler.delregler.FerieRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.MedlemskapRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.SøkersDødRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.TilBeregningAvGrad
import no.nav.pleiepengerbarn.uttak.regler.delregler.TilsynsbehovRegel
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.domene.Årsaksbygger

internal object UttaksplanRegler {

    private val PeriodeRegler = linkedSetOf(
            MedlemskapRegel(),
            FerieRegel(),
            TilsynsbehovRegel()
    )

    private val UttaksplanRegler = linkedSetOf(
            SøkersDødRegel()
    )

    internal fun fastsettUtaksplan(
            grunnlag: RegelGrunnlag,
            knektePerioder: Map<LukketPeriode,Set<KnekkpunktType>>) : Uttaksplan {

        val perioder = mutableMapOf<LukketPeriode, UttaksPeriodeInfo>()

        knektePerioder.forEach { (periode, knekkpunktTyper) ->
            val avslåttÅrsaker = mutableSetOf<AvslåttÅrsak>()
            val årsakbygger = Årsaksbygger()
            PeriodeRegler.forEach { regel ->
                val utfall = regel.kjør(periode = periode, grunnlag = grunnlag)
                if (utfall is Avslått) {
                    avslåttÅrsaker.addAll(utfall.årsaker)
                } else if (utfall is TilBeregningAvGrad) {
                    årsakbygger.startBeregningAvGraderMed(utfall.hjemler)
                }
            }
            if (avslåttÅrsaker.isNotEmpty()) {
                perioder[periode] = AvslåttPeriode(
                        knekkpunktTyper = knekkpunktTyper,
                        årsaker = avslåttÅrsaker
                )
            } else {
                val grader = GradBeregner.beregnGrader(
                        periode = periode,
                        grunnlag = grunnlag,
                        årsakbygger = årsakbygger
                )

                if (grader.årsak is AvslåttÅrsak) {
                    perioder[periode] = AvslåttPeriode(
                            knekkpunktTyper = knekkpunktTyper,
                            årsaker = setOf(grader.årsak)
                    )
                } else if (grader.årsak is InnvilgetÅrsak) {
                    perioder[periode] = InnvilgetPeriode(
                            knekkpunktTyper = knekkpunktTyper,
                            grad = grader.grad,
                            utbetalingsgrader = grader.utbetalingsgrader,
                            årsak = grader.årsak
                    )
                }
            }
        }

        var uttaksplan = Uttaksplan(perioder = perioder)

        UttaksplanRegler.forEach {uttaksplanRegler ->
            uttaksplan = uttaksplanRegler.kjør(
                    uttaksplan = uttaksplan,
                    grunnlag = grunnlag
            )
        }
        return uttaksplan
    }

}