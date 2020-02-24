package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttPeriodeÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeResultat
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode

internal class TilsynsbehovRegel : Regel {
    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Utfall {
        grunnlag.tilsynsbehov.map { it.periode }.overlappendePeriode(periode)?.apply {
            return TilBeregningAvGrad()
        }
        return Avslått(avslagsÅrsak = AvslåttPeriodeÅrsak.PERIODE_ETTER_TILSYNSBEHOV)
    }


    override fun kjør(uttaksperiode: Uttaksperiode, grunnlag: RegelGrunnlag, uttaksperiodeResultat: UttaksperiodeResultat):UttaksperiodeResultat {

        val tilsynsbehovSomOverlapperMedPeriode = grunnlag.tilsynsbehov.find {
            (it.periode.fom == uttaksperiode.periode.fom || it.periode.fom.isBefore(uttaksperiode.periode.fom)) &&
            (it.periode.tom == uttaksperiode.periode.tom || it.periode.tom.isAfter(uttaksperiode.periode.tom))
        }

        if (tilsynsbehovSomOverlapperMedPeriode == null) {
            val årsaker = mutableSetOf<AvslåttPeriodeÅrsak>()
            årsaker.addAll(uttaksperiodeResultat.avslåttPeriodeÅrsaker)
            årsaker.add(AvslåttPeriodeÅrsak.PERIODE_ETTER_TILSYNSBEHOV)
            return uttaksperiodeResultat.copy(avslåttPeriodeÅrsaker = årsaker)
        }
        return uttaksperiodeResultat.copy()
    }


}