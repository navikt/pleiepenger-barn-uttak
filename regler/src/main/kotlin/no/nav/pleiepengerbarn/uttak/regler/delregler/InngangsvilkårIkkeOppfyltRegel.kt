package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode

internal class InngangsvilkårIkkeOppfyltRegel : PeriodeRegel {

    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Regelutfall {
        grunnlag.inngangsvilkårIkkeOppfylt.overlappendePeriode(periode)?.apply {
            return IkkeOppfylt(årsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT))
        }
        return TilBeregningAvGrad()
    }
}