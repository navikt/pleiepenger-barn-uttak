package no.nav.pleiepengerbarn.uttak.kontrakter

data class Uttaksplan(val perioder: List<Uttaksperiode>)

data class Uttaksplaner(val uttaksplaner: Map<BehandlingId, Uttaksplan>)