package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import java.math.BigDecimal
import java.math.RoundingMode

internal object GradBeregner {

    internal fun beregnGrad(periode: LukketPeriode, grunnlag: RegelGrunnlag):Prosent{

        return Prosent(100)
    }

    private fun finnSumAndreParter(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        var sumAndreParter = Prosent.ZERO
        grunnlag.andrePartersUttaksplan.forEach {uttaksplan ->

        val annenPartsPeriode = uttaksplan.perioder
                    .filter { overlapper(it.key, periode) }
                    .filter { it.value is InnvilgetPeriode }
                    .values.firstOrNull() as InnvilgetPeriode?

            if (annenPartsPeriode != null) {
                sumAndreParter += annenPartsPeriode.grad
            }
        }
        return sumAndreParter
    }


    private fun finnTilsyn(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        val tilsyn = grunnlag.tilsynsperioder.entries.find { overlapper(it.key, periode) }
        if (tilsyn != null) {
            return tilsyn.value.grad.setScale(2, RoundingMode.HALF_EVEN)
        }
        return Prosent.ZERO.setScale(2, RoundingMode.HALF_EVEN)
    }

    private fun finnTilsynsbehov(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        val tilsynsbehovSomOverlapperMedPeriode = grunnlag.tilsynsbehov.entries.find { overlapper(it.key, periode) }
        return when (tilsynsbehovSomOverlapperMedPeriode?.value?.prosent) {
            TilsynsbehovStørrelse.PROSENT_100 -> Prosent(100)
            TilsynsbehovStørrelse.PROSENT_200 -> Prosent(200)
            else -> Prosent.ZERO
        }.setScale(2, RoundingMode.HALF_EVEN)
    }

    private fun max(a:Prosent, b:Prosent):Prosent {
        if (b > a) {
            return b
        }
        return a
    }

    private fun overlapper(periode1: LukketPeriode, periode2: LukketPeriode) =
            (periode1.fom == periode2.fom || periode1.fom.isBefore(periode2.fom)) &&
                    (periode1.tom == periode2.tom || periode1.tom.isAfter(periode2.tom))



}