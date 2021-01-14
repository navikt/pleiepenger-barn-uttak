package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.*

typealias Uttaksperiode = Map.Entry<LukketPeriode, UttaksPeriodeInfo>

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Uttaksplan(
        @JsonProperty("perioder") val perioder: Map<LukketPeriode, UttaksPeriodeInfo> = mapOf()
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Utbetalingsgrader(
        @JsonProperty("arbeidsforhold") val arbeidsforhold: Arbeidsforhold,
        @JsonProperty("utbetalingsgrad") val utbetalingsgrad: Prosent
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "utfall")
@JsonSubTypes(
        JsonSubTypes.Type(value = InnvilgetPeriode::class, name = "INNVILGET"),
        JsonSubTypes.Type(value = AvslåttPeriode::class, name = "AVSLÅTT")
)
interface UttaksPeriodeInfo {
    fun knekkpunktTyper() : Set<KnekkpunktType>
    fun kildeBehandlingUUID(): BehandlingUUID
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName("INNVILGET")
data class InnvilgetPeriode @JsonCreator constructor(
    private val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
    private val kildeBehandlingUUID: BehandlingUUID,
    @JsonProperty("uttaksgrad") val uttaksgrad: Prosent,
    @JsonProperty("utbetalingsgrader") val utbetalingsgrader: List<Utbetalingsgrader>,
    @JsonProperty("årsak") val årsak: InnvilgetÅrsaker,
    @JsonProperty("hjemler") val hjemler: Set<Hjemmel>
) : UttaksPeriodeInfo {
    constructor(knekkpunktTyper: Set<KnekkpunktType> = setOf(),
                kildeBehandlingUUID: BehandlingUUID,
                uttaksgrad: Prosent,
                utbetalingsgrader: List<Utbetalingsgrader>,
                årsak: InnvilgetÅrsak) : this(
            knekkpunktTyper = knekkpunktTyper,
            kildeBehandlingUUID = kildeBehandlingUUID,
            uttaksgrad = uttaksgrad,
            utbetalingsgrader = utbetalingsgrader,
            årsak = årsak.årsak,
            hjemler = årsak.hjemler
    )

    @JsonProperty("knekkpunkter") override fun knekkpunktTyper() = knekkpunktTyper
    @JsonProperty("kildeBehandlingUUID") override fun kildeBehandlingUUID() = kildeBehandlingUUID
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName("AVSLÅTT")
data class AvslåttPeriode(
        private val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
        private val kildeBehandlingUUID: BehandlingUUID,
        val årsaker: Set<AvslåttÅrsak>
) : UttaksPeriodeInfo {
    @JsonProperty("knekkpunkter") override fun knekkpunktTyper() = knekkpunktTyper
    @JsonProperty("kildeBehandlingUUID") override fun kildeBehandlingUUID() = kildeBehandlingUUID
}