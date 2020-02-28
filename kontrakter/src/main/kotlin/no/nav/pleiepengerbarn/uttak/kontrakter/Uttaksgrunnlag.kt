package no.nav.pleiepengerbarn.uttak.kontrakter

data class Uttaksgrunnlag (
        val søker: Søker = Søker(),
        val sakId: SakId,
        val behandlingId: BehandlingId,
        val søknadsperioder: List<LukketPeriode>,
        val andrePartersBehandlinger: List<BehandlingId> = listOf(),
        val arbeid: List<ArbeidsforholdOgArbeidsperioder>,
        val tilsynsbehov: Map<LukketPeriode, Tilsynsbehov>,
        val lovbestemtFerie: List<LukketPeriode> = listOf(),
        val tilsynsperioder: Map<LukketPeriode, Tilsyn> = mapOf(),
        val medlemskap: Map<LukketPeriode, Medlemskap>
)