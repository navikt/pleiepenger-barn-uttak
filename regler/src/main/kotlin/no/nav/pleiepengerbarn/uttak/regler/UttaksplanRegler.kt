package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.delregler.FerieRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.TilsynsbehovRegel

internal object UttaksplanRegler {

    private val INGEN_UTBETALING = Prosent.ZERO

    fun fastsettUttaksplan(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag):Uttaksplan {
        val resultatPerioder = mutableListOf<Uttaksperiode>()
        uttaksplan.perioder.forEach { periode ->
                val resultat = kjørAlleRegler(uttaksperiode = periode, grunnlag = grunnlag)
                var oppdatertPeriode = periode.copy(uttaksperiodeResultat = resultat)
                oppdatertPeriode = oppdaterUtbetalingsgrad(oppdatertPeriode, grunnlag)
                resultatPerioder.add(oppdatertPeriode)
        }
        return uttaksplan.copy(perioder = resultatPerioder)
    }

    private fun kjørAlleRegler(uttaksperiode: Uttaksperiode, grunnlag: RegelGrunnlag):UttaksperiodeResultat {

        var oppdatertResultat = FerieRegel().kjør(uttaksperiode, grunnlag, uttaksperiode.uttaksperiodeResultat)
        oppdatertResultat = TilsynsbehovRegel().kjør(uttaksperiode, grunnlag, oppdatertResultat)

        return oppdatertResultat
    }

    private fun oppdaterUtbetalingsgrad(uttaksperiode: Uttaksperiode, grunnlag: RegelGrunnlag):Uttaksperiode {
        if (uttaksperiode.uttaksperiodeResultat.avslåttPeriodeÅrsaker.isEmpty()) {
            val grad = GradBeregner.beregnGrad(uttaksperiode, grunnlag)
            return uttaksperiode.copy(uttaksperiodeResultat = uttaksperiode.uttaksperiodeResultat.copy(grad = grad))
        }
        return uttaksperiode.copy(uttaksperiodeResultat = uttaksperiode.uttaksperiodeResultat.copy(grad = INGEN_UTBETALING))
    }

}