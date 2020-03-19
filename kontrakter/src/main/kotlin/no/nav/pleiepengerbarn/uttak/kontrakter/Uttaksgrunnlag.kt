package no.nav.pleiepengerbarn.uttak.kontrakter

data class Uttaksgrunnlag (
        val barn: Barn = Barn(),
        val søker: Søker,
        val saksnummer: Saksnummer,
        val behandlingId: BehandlingId,
        val søknadsperioder: List<LukketPeriode>,
        val andrePartersSaksnummer: List<Saksnummer> = listOf(),
        val arbeid: Arbeid,
        val tilsynsbehov: Map<LukketPeriode, Tilsynsbehov>,
        val lovbestemtFerie: List<LukketPeriode> = listOf(),
        val tilsynsperioder: Map<LukketPeriode, TilsynPeriodeInfo> = mapOf(),
        val medlemskap: Map<LukketPeriode, Medlemskap>
)