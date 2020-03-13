package no.nav.pleiepengerbarn.uttak.kontrakter

data class Uttaksgrunnlag (
        val barn: Barn = Barn(),
        val søker: Søker,
        val saksnummer: Saksnummer,
        val behandlingId: BehandlingId,
        val søknadsperioder: List<LukketPeriode>,
        val andrePartersBehandlinger: List<BehandlingId> = listOf(),
        val arbeid: Arbeid,
        val tilsynsbehov: Map<LukketPeriode, Tilsynsbehov>,
        val lovbestemtFerie: List<LukketPeriode> = listOf(),
        val tilsynsperioder: Map<LukketPeriode, Tilsyn> = mapOf(),
        val medlemskap: Map<LukketPeriode, Medlemskap>
)