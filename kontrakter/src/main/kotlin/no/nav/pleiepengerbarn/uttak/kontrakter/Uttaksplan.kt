package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.JsonProperty
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

@JsonTypeName("Innvilget") // TODO: Skille på det som er på POST og GET response..
data class InnvilgetPeriode(
        private val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
        val grad: Prosent,
        val utbetalingsgrader: Map<ArbeidsforholdRef, Prosent>,
        private val årsak: InnvilgetÅrsak
) : UttaksPeriodeInfo {
    override fun knekkpunktTyper() = knekkpunktTyper
    @get:JsonProperty("årsak") val innvilgetÅrsak = årsak
    @get:JsonProperty("hjemler") val hjemler = årsak.hjemler
}

@JsonTypeName("Avslått")
data class AvslåttPeriode(
        private val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
        val årsaker: Set<AvslåttÅrsak>
) : UttaksPeriodeInfo {
    override fun knekkpunktTyper() = knekkpunktTyper
}