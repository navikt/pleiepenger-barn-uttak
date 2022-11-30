package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.RettVedDød
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import java.time.temporal.ChronoUnit

internal class BarnsDødPeriodeRegel : PeriodeRegel {

    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Regelutfall {
        val dødsdato = grunnlag.barn.dødsdato
        if (dødsdato != null) {
            val sisteDagMedRettEtterBarnetsDød = dødsdato.plusDays(1).plus(grunnlag.barn.rettVedDød?.lengde ?: 0, grunnlag.barn.rettVedDød?.enhet ?: ChronoUnit.DAYS)
            if (periode.fom.isAfter(sisteDagMedRettEtterBarnetsDød)) {
                return IkkeOppfylt(årsaker = setOf(Årsak.BARNETS_DØDSFALL))
            }
            if (periode.fom.isAfter(dødsdato)) {
                val årsak = when(grunnlag.barn.rettVedDød) {
                    RettVedDød.RETT_6_UKER -> Årsak.OPPFYLT_PGA_BARNETS_DØDSFALL_6_UKER
                    RettVedDød.RETT_12_UKER -> Årsak.OPPFYLT_PGA_BARNETS_DØDSFALL_12_UKER
                    null -> null
                }
                return TilBeregningAvGrad(årsak)
            }
        }
        return TilBeregningAvGrad()
    }
}
