package no.nav.pleiepengerbarn.uttak.kontrakter

data class UttaksperiodeResultat(val grad:Prosent, val avslåttPeriodeÅrsaker: Set<AvslåttPeriodeÅrsak> = setOf())