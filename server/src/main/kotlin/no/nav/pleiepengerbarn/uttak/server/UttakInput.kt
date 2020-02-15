package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*

data class UttakInput(
        val sakId:SakId,
        val behandlingId: BehandlingId,
        val tilsynsbehov: List<Tilsynsbehov>,
        val søktePerioder:List<LukketPeriode>,
        val arbeidsforhold: Map<Arbeidsforhold, List<Arbeid>> = mapOf(),
        val tilsynPerioder:List<Tilsyn> = listOf(),
        val ferier:List<LukketPeriode> = listOf(),
        val andrePartersBehandling: List<BehandlingId> = listOf()
)