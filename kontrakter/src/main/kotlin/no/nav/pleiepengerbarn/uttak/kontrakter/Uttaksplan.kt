package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.*
import java.time.Duration

typealias Uttaksperiode = Map.Entry<LukketPeriode, UttaksperiodeInfo>

enum class Utfall {
    INNVILGET,AVSLÅTT
}

enum class AnnenPart {
    ALENE,
    MED_ANDRE,
    VENTER_ANDRE
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
    @JsonProperty("normalArbeidstid") val normalArbeidstid: Duration,
    @JsonProperty("faktiskArbeidstid") val faktiskArbeidstid: Duration?,
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
    @JsonProperty("graderingMotTilsyn") val graderingMotTilsyn: GraderingMotTilsyn?,
    @JsonProperty("knekkpunktTyper") val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
    @JsonProperty("kildeBehandlingUUID") val kildeBehandlingUUID: BehandlingUUID,
    @JsonProperty("annenPart") val annenPart: AnnenPart
) {

    companion object {

        fun avslag(årsaker: Set<Årsak>, knekkpunktTyper: Set<KnekkpunktType>, kildeBehandlingUUID: BehandlingUUID, annenPart: AnnenPart): UttaksperiodeInfo {

            //TODO: sjekk at alle årsaker er avslag

            return UttaksperiodeInfo(
                utfall = Utfall.AVSLÅTT,
                uttaksgrad = Prosent.ZERO,
                utbetalingsgrader = listOf(),
                årsaker = årsaker,
                graderingMotTilsyn = null,
                knekkpunktTyper = knekkpunktTyper,
                kildeBehandlingUUID = kildeBehandlingUUID,
                annenPart = annenPart
            )
        }

        fun innvilgelse(uttaksgrad: Prosent, utbetalingsgrader: List<Utbetalingsgrader>, årsak: Årsak? = null, graderingMotTilsyn: GraderingMotTilsyn? = null, knekkpunktTyper: Set<KnekkpunktType>, kildeBehandlingUUID: BehandlingUUID, annenPart: AnnenPart): UttaksperiodeInfo {

            //TODO: sjekk at årsak er innvilgelse

            return UttaksperiodeInfo(
                utfall = Utfall.INNVILGET,
                uttaksgrad = uttaksgrad,
                utbetalingsgrader = utbetalingsgrader,
                årsaker = if (årsak == null) setOf() else setOf(årsak),
                graderingMotTilsyn = graderingMotTilsyn,
                knekkpunktTyper = knekkpunktTyper,
                kildeBehandlingUUID = kildeBehandlingUUID,
                annenPart = annenPart
            )
        }

    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class GraderingMotTilsyn(
    @JsonProperty("pleiebehov") val pleiebehov: Prosent,
    @JsonProperty("etablertTilsyn") val etablertTilsyn: Prosent,
    @JsonProperty("andreSøkeresTilsyn") val andreSøkeresTilsyn: Prosent,
    @JsonProperty("tilgjengeligForSøker") val tilgjengeligForSøker: Prosent
)
