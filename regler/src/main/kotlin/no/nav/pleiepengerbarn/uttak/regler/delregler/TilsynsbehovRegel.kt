package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode

internal class TilsynsbehovRegel : PeriodeRegel {
    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Regelutfall {
        grunnlag.pleiebehov.entries.map { it.key }.overlappendePeriode(periode)?.apply {
            return TilBeregningAvGrad()
        }
        return Avslått(årsaker = setOf(Årsak.UTENOM_TILSYNSBEHOV))
    }
}