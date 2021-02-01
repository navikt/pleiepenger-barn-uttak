package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode

internal class InngangsvilkårAvslåttRegel : PeriodeRegel {

    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Regelutfall {
        grunnlag.inngangsvilkårAvslått.overlappendePeriode(periode)?.apply {
            return Avslått(årsaker = setOf(Årsak.INNGANGSVILKÅR_AVSLÅTT))
        }
        return TilBeregningAvGrad()
    }
}