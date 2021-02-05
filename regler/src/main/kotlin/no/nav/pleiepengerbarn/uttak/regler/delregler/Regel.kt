package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag

internal interface PeriodeRegel {
    fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag) : Regelutfall
}

internal interface Regelutfall
internal class IkkeOppfylt(
    internal val årsaker: Set<Årsak>
) : Regelutfall
internal class TilBeregningAvGrad : Regelutfall

internal interface UttaksplanRegel {
    fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag) : Uttaksplan
}