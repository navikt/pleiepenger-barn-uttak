package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.delregler.Avslått
import no.nav.pleiepengerbarn.uttak.regler.delregler.FerieRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.MedlemskapRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.TilsynsbehovRegel
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag

internal object UttaksplanRegler {

    private val NEDRE_GRENSE_FOR_UTTAK = Prosent(20)

    private val Regler = linkedSetOf(
            MedlemskapRegel(),
            FerieRegel(),
            TilsynsbehovRegel()
    )

    internal fun fastsettUtaksplan(
            grunnlag: RegelGrunnlag,
            knektePerioder: Map<LukketPeriode,Set<KnekkpunktType>>) : Uttaksplan {

        val perioder = mutableMapOf<LukketPeriode, UttaksPeriodeInfo>()

        knektePerioder.forEach { (periode, knekkpunktTyper) ->
            val avslagsÅrsaker = mutableSetOf<AvslåttPeriodeÅrsak>()
            Regler.forEach { regel ->
                val utfall = regel.kjør(periode = periode, grunnlag = grunnlag)
                if (utfall is Avslått) {
                    avslagsÅrsaker.add(utfall.avslagsÅrsak)
                }
            }
            if (avslagsÅrsaker.isNotEmpty()) {
                perioder[periode] = AvslåttPeriode(
                        knekkpunktTyper = knekkpunktTyper,
                        avslagsÅrsaker = avslagsÅrsaker.toSet()
                )
            } else {
                val grad = GradBeregner.beregnGrad(periode, grunnlag)

                if (grad < NEDRE_GRENSE_FOR_UTTAK) {
                    perioder[periode] = AvslåttPeriode(
                            knekkpunktTyper = knekkpunktTyper,
                            avslagsÅrsaker = setOf(AvslåttPeriodeÅrsak.FOR_LAV_UTTAKSGRAD)
                    )
                } else {
                    perioder[periode] = InnvilgetPeriode(
                            knekkpunktTyper = knekkpunktTyper,
                            grad = grad,
                            utbetalingsgrader = listOf() //TODO

                    )
                }
            }
        }
        return Uttaksplan(perioder = perioder)
    }
}