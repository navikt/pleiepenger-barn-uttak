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
        val tilsynsbehov: Periodisert<TilsynsbehovPeriodeInfo>,
        val lovbestemtFerie: List<LukketPeriode>,
        val tilsyn: Periodisert<TilsynPeriodeInfo>,
        val medlemskap: Periodisert<MedlemskapPeriodeInfo>
)