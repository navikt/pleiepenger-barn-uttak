package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.*
import java.time.Duration

typealias Uttaksperiode = Map.Entry<LukketPeriode, UttaksperiodeInfo>

enum class Utfall {
    OPPFYLT,
    IKKE_OPPFYLT
}

enum class AnnenPart {
    ALENE,
    MED_ANDRE,
    VENTER_ANDRE //TODO: skal vi ha med denne?
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
    @JsonProperty("søkersTapteArbeidstid") val søkersTapteArbeidstid: Prosent?,
    @JsonProperty("årsak") val årsaker: Set<Årsak>,
    @JsonProperty("inngangsvilkår") val inngangsvilkår: Map<String, Utfall> = mapOf(),
    @JsonProperty("graderingMotTilsyn") val graderingMotTilsyn: GraderingMotTilsyn?,
    @JsonProperty("knekkpunktTyper") val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
    @JsonProperty("kildeBehandlingUUID") val kildeBehandlingUUID: BehandlingUUID,
    @JsonProperty("annenPart") val annenPart: AnnenPart
) {

    companion object {

        fun ikkeOppfylt(utbetalingsgrader: List<Utbetalingsgrader>, søkersTapteArbeidstid: Prosent, årsaker: Set<Årsak>, knekkpunktTyper: Set<KnekkpunktType>, kildeBehandlingUUID: BehandlingUUID, annenPart: AnnenPart): UttaksperiodeInfo {

            val årsakerMedOppfylt = årsaker.filter { it.oppfylt }
            require(årsakerMedOppfylt.isEmpty()) {
                "Kan ikke avslå med årsaker for oppfylt. ($årsakerMedOppfylt)"
            }

            return UttaksperiodeInfo(
                utfall = Utfall.IKKE_OPPFYLT,
                uttaksgrad = Prosent.ZERO,
                utbetalingsgrader = utbetalingsgrader,
                søkersTapteArbeidstid = søkersTapteArbeidstid,
                årsaker = årsaker,
                graderingMotTilsyn = null,
                knekkpunktTyper = knekkpunktTyper,
                kildeBehandlingUUID = kildeBehandlingUUID,
                annenPart = annenPart
            )
        }

        fun oppfylt(uttaksgrad: Prosent, utbetalingsgrader: List<Utbetalingsgrader>, søkersTapteArbeidstid: Prosent, årsak: Årsak, graderingMotTilsyn: GraderingMotTilsyn? = null, knekkpunktTyper: Set<KnekkpunktType>, kildeBehandlingUUID: BehandlingUUID, annenPart: AnnenPart): UttaksperiodeInfo {

            require(årsak.oppfylt) {
                "Kan ikke sette periode til oppfylt med årsak som ikke er for oppfylt. ($årsak)"
            }

            return UttaksperiodeInfo(
                utfall = Utfall.OPPFYLT,
                uttaksgrad = uttaksgrad,
                utbetalingsgrader = utbetalingsgrader,
                søkersTapteArbeidstid = søkersTapteArbeidstid,
                årsaker = setOf(årsak),
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
