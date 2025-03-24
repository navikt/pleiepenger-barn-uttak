package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.regler.domene.GraderBeregnet

interface IBeregnGrader {

    fun beregn(beregnGraderGrunnlag: BeregnGraderGrunnlag): GraderBeregnet

    fun beregnMedMaksGrad(
        beregnGraderGrunnlag: BeregnGraderGrunnlag,
        maksGradIProsent: Prosent
    ): GraderBeregnet
}