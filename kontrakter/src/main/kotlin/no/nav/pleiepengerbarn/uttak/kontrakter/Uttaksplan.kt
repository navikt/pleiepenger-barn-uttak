package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

data class Uttaksplaner(val uttaksplaner: Map<BehandlingId, Uttaksplan>)

typealias Uttaksperiode = Map.Entry<LukketPeriode, UttaksPeriodeInfo>

data class Uttaksplan(
        val perioder: Map<LukketPeriode, UttaksPeriodeInfo> = mapOf()
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "utfall")
@JsonSubTypes(
        JsonSubTypes.Type(value = InnvilgetPeriode::class, name = "Innvilget"),
        JsonSubTypes.Type(value = AvslåttPeriode::class, name = "Avslått")
)
interface UttaksPeriodeInfo {
    fun knekkpunktTyper() : Set<KnekkpunktType>
}

@JsonTypeName("Innvilget")
data class InnvilgetPeriode(
        private val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
        val grad: Prosent,
        val utbetalingsgrader: Map<ArbeidsforholdRef, Prosent>
) : UttaksPeriodeInfo {
    override fun knekkpunktTyper() = knekkpunktTyper
}

@JsonTypeName("Avslått")
data class AvslåttPeriode(
        private val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
        val avslagsÅrsaker: Set<AvslåttPeriodeÅrsak>
) : UttaksPeriodeInfo {
    override fun knekkpunktTyper() = knekkpunktTyper
}