package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttPeriodeÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeResultat
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode

internal class MedlemskapRegel : Regel {
    override fun kjør(
            uttaksperiode: Uttaksperiode,
            grunnlag: RegelGrunnlag, uttaksperiodeResultat: UttaksperiodeResultat): UttaksperiodeResultat {

        grunnlag.ikkeMedlem.overlappendePeriode(uttaksperiode)?.apply {
            val årsaker = mutableSetOf<AvslåttPeriodeÅrsak>()
            årsaker.addAll(uttaksperiodeResultat.avslåttPeriodeÅrsaker)
            årsaker.add(AvslåttPeriodeÅrsak.IKKE_MEDLEM)
            return uttaksperiodeResultat.copy(avslåttPeriodeÅrsaker = årsaker)
        }

        return uttaksperiodeResultat.copy()
    }

}