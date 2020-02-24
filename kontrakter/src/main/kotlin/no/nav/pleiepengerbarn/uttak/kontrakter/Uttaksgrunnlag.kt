package no.nav.pleiepengerbarn.uttak.kontrakter

data class Periodisert<PeriodeInfo>(
        val perioder: Map<LukketPeriode, PeriodeInfo>
)

data class Uttaksgrunnlag<BehandlingType> (
        val sakId: SakId,
        val behandlingId: BehandlingId,
        val søknadsperioder: Periodisert<Any>,
        val andrePartersBehandlinger: List<BehandlingType>,
        val arbeidsforhold: List<ArbeidsforholdV2>,
        val tilsynsbehov: Periodisert<TilsynsbehovPeriodeInfo>,
        val lovbestemtFerie: Periodisert<Any>,
        val tilsyn: Periodisert<TilsynPeriodeInfo>,
        val medlemskap: Periodisert<MedlemskapPeriodeInfo>
)