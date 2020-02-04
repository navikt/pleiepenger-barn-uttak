package no.nav.pleiepengerbarn.uttak.kontrakter

data class Søknad(
        val saksnummer:SakId,
        val behandlingId: BehandlingId,
        val oppgittePerioder: OppgittePerioder
)