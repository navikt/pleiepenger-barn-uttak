package no.nav.pleiepengerbarn.uttak.kontrakter

data class Uttak(
    val behandlingId: BehandlingId,
    val perioder: Map<Periode, PeriodeInnhold>
)
