package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.GradBeregner
import no.nav.pleiepengerbarn.uttak.regler.KnekkpunktUtleder
import no.nav.pleiepengerbarn.uttak.regler.delregler.*
import no.nav.pleiepengerbarn.uttak.regler.delregler.Avslått
import no.nav.pleiepengerbarn.uttak.regler.delregler.MedlemskapRegel

internal data class UttaksplanBuilder(
        internal val grunnlag: RegelGrunnlag
) {
    private companion object {
        private val NEDRE_GRENSE_FOR_UTTAK = Prosent(20)
        private val Regler = linkedSetOf(
                MedlemskapRegel(),
                FerieRegel(),
                TilsynsbehovRegel()
        )
    }

    private val knekkpunkter = KnekkpunktUtleder.finnKnekkpunkter(grunnlag)
    private val knektePerioder = knekk()

    private fun knekk() : Map<LukketPeriode, Set<KnekkpunktType>> {
        val knektePerioder = mutableMapOf<LukketPeriode, MutableSet<KnekkpunktType>>()
        grunnlag.søknadsperioder.forEach { søknadsperiode ->
            var rest = søknadsperiode
            knekkpunkter.forEach { knekkpunkt ->
                val dato = knekkpunkt.knekk
                if (!dato.isEqual(søknadsperiode.fom) && !dato.isBefore(søknadsperiode.fom) && !dato.isAfter(søknadsperiode.tom)) {
                    val periode = LukketPeriode(fom = søknadsperiode.fom, tom = knekkpunkt.knekk.minusDays(1))
                    knektePerioder.putIfAbsent(periode, mutableSetOf())
                    knektePerioder[periode]!!.addAll(knekkpunkt.typer)
                    rest = LukketPeriode(fom = dato, tom = rest.tom)
                }
            }
        }
        return knektePerioder
    }

    fun build() : UttaksplanV2 {
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
                            grad = grad
                    )
                }

            }
        }
        return UttaksplanV2(
                perioder = perioder.toMap()
        )
    }


}