package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeResultat

interface Regel {

    fun kjør(uttaksperiode: Uttaksperiode, grunnlag: RegelGrunnlag, uttaksperiodeResultat: UttaksperiodeResultat): UttaksperiodeResultat
}