package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.*

data class RegelGrunnlag(
        val barn: Barn = Barn(),
        val søker: Søker,
        val tilsynsbehov: Map<LukketPeriode, Tilsynsbehov>,
        val søknadsperioder: List<LukketPeriode>,
        val arbeid: List<Arbeid> = listOf(),
        val tilsynsperioder:Map<LukketPeriode, TilsynPeriodeInfo> = mapOf(),
        val lovbestemtFerie:List<LukketPeriode> = listOf(),
        val andrePartersUttaksplan: Map<Saksnummer, Uttaksplan> = mapOf(),
        val ikkeMedlem: List<LukketPeriode> = listOf()
)
