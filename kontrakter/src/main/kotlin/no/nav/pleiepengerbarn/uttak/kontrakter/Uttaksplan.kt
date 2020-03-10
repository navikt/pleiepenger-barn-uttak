package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.*

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
data class InnvilgetPeriode @JsonCreator constructor(
        private val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
        val grad: Prosent,
        val utbetalingsgrader: Map<ArbeidsforholdRef, Prosent>,
        val årsak: InnvilgetÅrsaker,
        val hjemler: Set<Hjemmel>

) : UttaksPeriodeInfo {
    constructor(knekkpunktTyper: Set<KnekkpunktType> = setOf(),
                grad: Prosent,
                utbetalingsgrader: Map<ArbeidsforholdRef, Prosent>,
                årsak: InnvilgetÅrsak) : this(
            knekkpunktTyper = knekkpunktTyper,
            grad = grad,
            utbetalingsgrader = utbetalingsgrader,
            årsak =
            årsak.årsak, hjemler = årsak.hjemler
    )

    @JsonProperty("knekkpunkter") override fun knekkpunktTyper() = knekkpunktTyper
}

@JsonTypeName("Avslått")
data class AvslåttPeriode(
        private val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
        val årsaker: Set<AvslåttÅrsak>
) : UttaksPeriodeInfo {
    @JsonProperty("knekkpunkter") override fun knekkpunktTyper() = knekkpunktTyper
}