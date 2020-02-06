package no.nav.pleiepengerbarn.uttak.kontrakter

data class UttaksperiodeResultat(val arbeidsforhold: Arbeidsforhold, val utbetalingsgrad:Prosent, val avslåttPeriodeÅrsak: AvslåttPeriodeÅrsak? = null)