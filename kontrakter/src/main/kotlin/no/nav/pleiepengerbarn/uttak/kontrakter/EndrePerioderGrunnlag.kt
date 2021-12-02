package no.nav.pleiepengerbarn.uttak.kontrakter

data class EndrePerioderGrunnlag(
    val saksnummer: Saksnummer,
    val behandlingUUID: BehandlingUUID,
    val perioderSomIkkeErInnvilget: Map<LukketPeriode, Årsak>
)