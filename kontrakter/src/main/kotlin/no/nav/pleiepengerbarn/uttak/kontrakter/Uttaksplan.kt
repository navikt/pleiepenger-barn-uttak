package no.nav.pleiepengerbarn.uttak.kontrakter

data class Uttaksplan(val perioder: List<Uttaksperiode>)

data class Uttaksplaner(val uttaksplaner: Map<BehandlingId, Uttaksplan>)

typealias UttaksperiodeV2 = Map.Entry<LukketPeriode, UttaksPeriodeInfo>

data class UttaksplanV2(
        val perioder: Map<LukketPeriode, UttaksPeriodeInfo>
)

interface UttaksPeriodeInfo {
    fun knekkpunktTyper() : Set<KnekkpunktType>
}

data class InnvilgetPeriode(
        private val knekkpunktTyper: Set<KnekkpunktType>,
        val grad: Prosent
) : UttaksPeriodeInfo {
    override fun knekkpunktTyper() = knekkpunktTyper
}

data class AvslåttPeriode(
        private val knekkpunktTyper: Set<KnekkpunktType>,
        val avslagsÅrsaker: Set<AvslåttPeriodeÅrsak>
) : UttaksPeriodeInfo {
    override fun knekkpunktTyper() = knekkpunktTyper
}