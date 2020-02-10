package no.nav.pleiepengerbarn.uttak.kontrakter

data class UttaksperiodeResultat(val utbetalingsgrad:Prosent, val avslåttPeriodeÅrsaker: Set<AvslåttPeriodeÅrsak> = setOf())