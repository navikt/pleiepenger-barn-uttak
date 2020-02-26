package no.nav.pleiepengerbarn.uttak.kontrakter

data class Uttaksgrunnlag (
        val sakId: SakId,
        val behandlingId: BehandlingId,
        val søknadsperioder: List<LukketPeriode>,
        val andrePartersBehandlinger: List<BehandlingId> = listOf(),
        val arbeidsforhold: List<Arbeidsforhold>,
        val tilsynsbehov: Map<LukketPeriode, Tilsynsbehov>,
        val lovbestemtFerie: List<LukketPeriode> = listOf(),
//TODO løses senere:        val tilsyn: Periodisert<TilsynPeriodeInfo>,
        val medlemskap: Map<LukketPeriode, MedlemskapPeriodeInfo>
)