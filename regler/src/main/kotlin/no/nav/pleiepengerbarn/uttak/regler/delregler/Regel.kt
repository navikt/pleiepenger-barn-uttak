package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttPeriodeÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag

internal interface PeriodeRegel {
    fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag) : Utfall
}

internal interface Utfall
internal class Avslått(internal val avslagsÅrsak: AvslåttPeriodeÅrsak) : Utfall
internal class TilBeregningAvGrad : Utfall

internal interface UttaksplanRegel {
    fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag) : Uttaksplan
}