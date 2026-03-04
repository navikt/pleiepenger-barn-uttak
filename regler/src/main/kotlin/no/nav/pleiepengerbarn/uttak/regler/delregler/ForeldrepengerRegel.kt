package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperHelt

internal class ForeldrepengerRegel : PeriodeRegel {

    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Regelutfall {
        grunnlag.foreldrepengeperioder.overlappendePeriode(periode)?.apply {
            return IkkeOppfylt(årsaker = setOf(Årsak.INGEN_TAPT_INNTEKT_PGA_FP))
        }
        return TilBeregningAvGrad()
    }
}

private fun Collection<LukketPeriode>.overlappendePeriode(periode: LukketPeriode) = find {
    it.overlapperHelt(periode)
}

