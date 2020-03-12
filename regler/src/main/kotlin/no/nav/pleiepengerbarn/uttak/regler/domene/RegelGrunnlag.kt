package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.*

data class RegelGrunnlag(
        val barn: Barn = Barn(),
        val søker: Søker,
        val tilsynsbehov: Map<LukketPeriode, Tilsynsbehov>,
        val søknadsperioder:List<LukketPeriode>,
        val arbeid: Arbeid = listOf(),
        val tilsynsperioder:Map<LukketPeriode, Tilsyn> = mapOf(),
        val ferier:List<LukketPeriode> = listOf(),
        val andrePartersUttaksplan: List<Uttaksplan> = listOf(),
        val ikkeMedlem: List<LukketPeriode> = listOf()
)
