package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag

internal interface PeriodeRegel {
    fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag) : Utfall
}

internal interface Utfall
internal class Avslått(internal val årsaker: Set<AvslåttÅrsak>) : Utfall
internal class TilBeregningAvGrad(internal val hjemler: Set<Hjemmel>) : Utfall

internal interface UttaksplanRegel {
    fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag) : Uttaksplan
}