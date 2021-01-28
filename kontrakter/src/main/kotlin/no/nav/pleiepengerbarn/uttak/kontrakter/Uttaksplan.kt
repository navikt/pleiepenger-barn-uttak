package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.*

typealias Uttaksperiode = Map.Entry<LukketPeriode, UttaksperiodeInfo>

enum class Utfall {
    INNVILGET,AVSLÅTT
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Uttaksplan(
        @JsonProperty("perioder") val perioder: Map<LukketPeriode, UttaksperiodeInfo> = mapOf()
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Utbetalingsgrader(
        @JsonProperty("arbeidsforhold") val arbeidsforhold: Arbeidsforhold,
        @JsonProperty("utbetalingsgrad") val utbetalingsgrad: Prosent
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UttaksperiodeInfo @JsonCreator constructor(
    @JsonProperty("utfall") val utfall: Utfall,
    @JsonProperty("uttaksgrad") val uttaksgrad: Prosent,
    @JsonProperty("utbetalingsgrader") val utbetalingsgrader: List<Utbetalingsgrader>,
    @JsonProperty("årsak") val årsaker: Set<Årsak>,
    @JsonProperty("knekkpunktTyper") val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
    @JsonProperty("kildeBehandlingUUID") val kildeBehandlingUUID: BehandlingUUID
) {

    companion object {

        fun avslag(årsaker: Set<Årsak>, knekkpunktTyper: Set<KnekkpunktType>, kildeBehandlingUUID: BehandlingUUID): UttaksperiodeInfo {

            //TODO: sjekk at alle årsaker er avslag

            return UttaksperiodeInfo(
                utfall = Utfall.AVSLÅTT,
                uttaksgrad = Prosent.ZERO,
                utbetalingsgrader = listOf(),
                årsaker = årsaker,
                knekkpunktTyper = knekkpunktTyper,
                kildeBehandlingUUID = kildeBehandlingUUID,
            )
        }

        fun innvilgelse(uttaksgrad: Prosent, utbetalingsgrader: List<Utbetalingsgrader>, årsak: Årsak? = null, knekkpunktTyper: Set<KnekkpunktType>, kildeBehandlingUUID: BehandlingUUID): UttaksperiodeInfo {

            //TODO: sjekk at årsak er innvilgelse

            return UttaksperiodeInfo(
                utfall = Utfall.INNVILGET,
                uttaksgrad = uttaksgrad,
                utbetalingsgrader = utbetalingsgrader,
                årsaker = if (årsak == null) setOf() else setOf(årsak),
                knekkpunktTyper = knekkpunktTyper,
                kildeBehandlingUUID = kildeBehandlingUUID
            )
        }

    }


}

