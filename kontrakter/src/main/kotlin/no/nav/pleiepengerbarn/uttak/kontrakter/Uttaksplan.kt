package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.*
import java.time.Duration

typealias Uttaksperiode = Map.Entry<LukketPeriode, UttaksperiodeInfo>

enum class Utfall {
    OPPFYLT,
    IKKE_OPPFYLT
}

/**
 * Angir om det finnes uttak fra andre parter.
 */
enum class AnnenPart {
    ALENE,
    MED_ANDRE,
    VENTER_ANDRE //TODO: skal vi ha med denne?
}

/**
 * Årsaker til at etablert tilsyn skal overses.
 */
enum class OverseEtablertTilsynÅrsak {
    FOR_LAVT,
    NATTEVÅK,
    BEREDSKAP,
    NATTEVÅK_OG_BEREDSKAP
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
    @JsonProperty("oppgittTilsyn") val oppgittTilsyn: Duration?,
    @JsonProperty("årsaker") val årsaker: Set<Årsak>,
    @JsonProperty("inngangsvilkår") val inngangsvilkår: Map<String, Utfall> = mapOf(),
    @JsonProperty("pleiebehov") val pleiebehov: Prosent,
    @JsonProperty("graderingMotTilsyn") val graderingMotTilsyn: GraderingMotTilsyn?,
    @JsonProperty("knekkpunktTyper") val knekkpunktTyper: Set<KnekkpunktType> = setOf(),
    @JsonProperty("kildeBehandlingUUID") val kildeBehandlingUUID: BehandlingUUID,
    @JsonProperty("annenPart") val annenPart: AnnenPart,
    @JsonProperty("nattevåk") val nattevåk: Utfall?,
    @JsonProperty("beredskap") val beredskap: Utfall?
) {

    companion object {

        fun ikkeOppfylt(
            utbetalingsgrader: List<Utbetalingsgrader>,
            søkersTapteArbeidstid: Prosent,
            oppgittTilsyn: Duration?,
            årsaker: Set<Årsak>,
            pleiebehov: Prosent,
            graderingMotTilsyn: GraderingMotTilsyn? = null,
            knekkpunktTyper: Set<KnekkpunktType>,
            kildeBehandlingUUID: BehandlingUUID,
            annenPart: AnnenPart,
            nattevåk: Utfall?,
            beredskap: Utfall?): UttaksperiodeInfo {

            val årsakerMedOppfylt = årsaker.filter { it.oppfylt }
            require(årsakerMedOppfylt.isEmpty()) {
                "Kan ikke avslå med årsaker for oppfylt. ($årsakerMedOppfylt)"
            }

            return UttaksperiodeInfo(
                utfall = Utfall.IKKE_OPPFYLT,
                uttaksgrad = Prosent.ZERO,
                utbetalingsgrader = utbetalingsgrader,
                søkersTapteArbeidstid = søkersTapteArbeidstid,
                oppgittTilsyn = oppgittTilsyn,
                årsaker = årsaker,
                pleiebehov = pleiebehov,
                graderingMotTilsyn = graderingMotTilsyn,
                knekkpunktTyper = knekkpunktTyper,
                kildeBehandlingUUID = kildeBehandlingUUID,
                annenPart = annenPart,
                nattevåk = nattevåk,
                beredskap = beredskap
            )
        }

        fun oppfylt(
            uttaksgrad: Prosent,
            utbetalingsgrader: List<Utbetalingsgrader>,
            søkersTapteArbeidstid: Prosent,
            oppgittTilsyn: Duration?,
            årsak: Årsak,
            pleiebehov: Prosent,
            graderingMotTilsyn: GraderingMotTilsyn? = null,
            knekkpunktTyper: Set<KnekkpunktType>,
            kildeBehandlingUUID: BehandlingUUID,
            annenPart: AnnenPart,
            nattevåk: Utfall?,
            beredskap: Utfall?): UttaksperiodeInfo {

            require(årsak.oppfylt) {
                "Kan ikke sette periode til oppfylt med årsak som ikke er for oppfylt. ($årsak)"
            }

            return UttaksperiodeInfo(
                utfall = Utfall.OPPFYLT,
                uttaksgrad = uttaksgrad,
                utbetalingsgrader = utbetalingsgrader,
                søkersTapteArbeidstid = søkersTapteArbeidstid,
                oppgittTilsyn = oppgittTilsyn,
                årsaker = setOf(årsak),
                pleiebehov = pleiebehov,
                graderingMotTilsyn = graderingMotTilsyn,
                knekkpunktTyper = knekkpunktTyper,
                kildeBehandlingUUID = kildeBehandlingUUID,
                annenPart = annenPart,
                nattevåk = nattevåk,
                beredskap = beredskap
            )
        }

    }

    @JsonProperty("søkersTapteTimer")
    fun getSøkersTapteTimer(): Duration {
        var sumNormalTid = Duration.ZERO
        var sumFaktiskTid = Duration.ZERO
        utbetalingsgrader.forEach { sumNormalTid += it.normalArbeidstid }
        utbetalingsgrader.forEach { if (it.faktiskArbeidstid != null) sumFaktiskTid += it.faktiskArbeidstid }
        return sumNormalTid - sumFaktiskTid
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class GraderingMotTilsyn(
    @JsonProperty("etablertTilsyn") val etablertTilsyn: Prosent,
    @JsonProperty("overseEtablertTilsynÅrsak") val overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?,
    @JsonProperty("andreSøkeresTilsyn") val andreSøkeresTilsyn: Prosent,
    @JsonProperty("andreSøkeresTilsynReberegnet") val andreSøkeresTilsynReberegnet: Boolean,
    @JsonProperty("tilgjengeligForSøker") val tilgjengeligForSøker: Prosent
)
