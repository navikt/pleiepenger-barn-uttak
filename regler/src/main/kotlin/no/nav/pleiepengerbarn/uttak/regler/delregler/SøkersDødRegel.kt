package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag

internal class SøkersDødRegel : PeriodeRegel {

    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Regelutfall {
        val dødsdato = grunnlag.søker.dødsdato
        if (dødsdato != null) {
            val sisteDagMedRettEtterBarnetsDød = dødsdato
            if (periode.fom.isAfter(sisteDagMedRettEtterBarnetsDød)) {
                return IkkeOppfylt(årsaker = setOf(Årsak.SØKERS_DØDSFALL))
            }
        }
        return TilBeregningAvGrad()
    }
}
