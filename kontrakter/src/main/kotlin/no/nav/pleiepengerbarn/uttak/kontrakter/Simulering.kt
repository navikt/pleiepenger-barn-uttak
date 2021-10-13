package no.nav.pleiepengerbarn.uttak.kontrakter

data class Simulering(val forrigeUttaksplan: Uttaksplan?, val simulertUttaksplan: Uttaksplan, val uttakplanEndret: Boolean)