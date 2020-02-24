package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttPeriodeÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeResultat

internal interface Regel {

    fun kjør(uttaksperiode: Uttaksperiode, grunnlag: RegelGrunnlag, uttaksperiodeResultat: UttaksperiodeResultat): UttaksperiodeResultat
    fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag) : Utfall
}

internal interface Utfall
internal class Avslått(internal val avslagsÅrsak: AvslåttPeriodeÅrsak) : Utfall
internal class TilBeregningAvGrad : Utfall