package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttPeriodeÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeResultat

class FerieRegel : Regel {

    override fun kjør(uttaksperiode: Uttaksperiode, grunnlag: RegelGrunnlag,uttaksperiodeResultat: UttaksperiodeResultat):UttaksperiodeResultat {

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