package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperHelt

internal class FerieRegel : PeriodeRegel {

    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Regelutfall {
        grunnlag.lovbestemtFerie.overlappendePeriode(periode)?.apply {
            return IkkeOppfylt(årsaker = setOf(Årsak.LOVBESTEMT_FERIE))
        }
        return TilBeregningAvGrad()
    }
}


private fun Collection<LukketPeriode>.overlappendePeriode(periode: LukketPeriode) = find {
    it.overlapperHelt(periode)
}