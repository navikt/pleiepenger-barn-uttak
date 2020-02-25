package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttPeriodeÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode

internal class MedlemskapRegel : Regel {
    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Utfall {
        grunnlag.ikkeMedlem.overlappendePeriode(periode)?.apply {
            return Avslått(avslagsÅrsak = AvslåttPeriodeÅrsak.IKKE_MEDLEM)
        }
        return TilBeregningAvGrad()
    }
}