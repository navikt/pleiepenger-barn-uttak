package no.nav.pleiepengerbarn.uttak.kontrakter

data class Uttaksplan(
        val saksnummer: SakId,
        val behandlingId: BehandlingId,
        val perioder:List<Uttaksperiode>
)