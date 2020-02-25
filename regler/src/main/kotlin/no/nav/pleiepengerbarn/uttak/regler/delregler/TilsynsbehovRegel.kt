package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttPeriodeÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode

internal class TilsynsbehovRegel : Regel {
    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Utfall {
        grunnlag.tilsynsbehov.entries.map { it.key }.overlappendePeriode(periode)?.apply {
            return TilBeregningAvGrad()
        }
        return Avslått(avslagsÅrsak = AvslåttPeriodeÅrsak.PERIODE_ETTER_TILSYNSBEHOV)
    }
}