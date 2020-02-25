package no.nav.pleiepengerbarn.uttak.kontrakter

data class Periodisert<PeriodeInfo>(
        val perioder: Map<LukketPeriode, PeriodeInfo>
)

data class Uttaksgrunnlag (
        val sakId: SakId,
        val behandlingId: BehandlingId,
        val søknadsperioder: List<LukketPeriode>,
        val andrePartersBehandlinger: List<BehandlingId>,
        val arbeidsforhold: List<Arbeidsforhold>,
        val tilsynsbehov: Periodisert<Tilsynsbehov>,
        val lovbestemtFerie: List<LukketPeriode>,
//TODO løses senere:        val tilsyn: Periodisert<TilsynPeriodeInfo>,
        val medlemskap: Periodisert<MedlemskapPeriodeInfo>
)