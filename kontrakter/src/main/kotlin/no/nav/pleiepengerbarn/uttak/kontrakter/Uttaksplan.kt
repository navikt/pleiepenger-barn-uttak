package no.nav.pleiepengerbarn.uttak.kontrakter

data class Uttaksplan(var perioder: List<Uttaksperiode>)

data class Uttaksplaner(val uttaksplaner: Map<BehandlingId, Uttaksplan>)