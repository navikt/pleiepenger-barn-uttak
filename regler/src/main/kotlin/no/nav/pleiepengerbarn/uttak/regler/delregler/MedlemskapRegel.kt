package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode

internal class MedlemskapRegel : PeriodeRegel {
    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Regelutfall {

        grunnlag.ikkeMedlem.overlappendePeriode(periode)?.apply {
            return IkkeOppfylt(årsaker = setOf(Årsak.IKKE_MEDLEM_I_FOLKETRYGDEN))
        }
        return TilBeregningAvGrad()
    }
}