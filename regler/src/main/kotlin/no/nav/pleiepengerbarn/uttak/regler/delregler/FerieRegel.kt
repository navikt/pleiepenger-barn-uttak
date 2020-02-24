package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttPeriodeÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeResultat
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode

internal class FerieRegel : Regel {

    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Utfall {
        grunnlag.ferier.overlappendePeriode(periode)?.apply {
            return Avslått(avslagsÅrsak = AvslåttPeriodeÅrsak.OVERLAPPER_MED_FERIE)
        }
        return TilBeregningAvGrad()
    }

    override fun kjør(uttaksperiode: Uttaksperiode, grunnlag: RegelGrunnlag, uttaksperiodeResultat: UttaksperiodeResultat):UttaksperiodeResultat {

        val ferieSomOverlapperMedPeriode = grunnlag.ferier.find {
            (it.fom == uttaksperiode.periode.fom || it.fom.isBefore(uttaksperiode.periode.fom)) &&
            (it.tom == uttaksperiode.periode.tom || it.tom.isAfter(uttaksperiode.periode.tom))
        }

        if (ferieSomOverlapperMedPeriode != null) {
            val årsaker = mutableSetOf<AvslåttPeriodeÅrsak>()
            årsaker.addAll(uttaksperiodeResultat.avslåttPeriodeÅrsaker)
            årsaker.add(AvslåttPeriodeÅrsak.OVERLAPPER_MED_FERIE)
            return uttaksperiodeResultat.copy(avslåttPeriodeÅrsaker = årsaker)
        }
        return uttaksperiodeResultat.copy()
    }


}