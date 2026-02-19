package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.*
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate

enum class Utfall {
    OPPFYLT,
    IKKE_OPPFYLT
}

enum class YtelseType {
    PSB,
    PLS,
    OLP
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

enum class Endringsstatus {
    NY, ENDRET, UENDRET, UENDRET_RESULTAT
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Uttaksplan @JsonCreator constructor(
    @JsonProperty("perioder") val perioder: Map<LukketPeriode, UttaksperiodeInfo> = mapOf(),
    @JsonProperty("trukketUttak") val trukketUttak: List<LukketPeriode> = listOf(),
    @JsonProperty("kvoteInfo") val kvoteInfo: KvoteInfo? = null,
    @JsonProperty("commitId") val commitId: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class KvoteInfo @JsonCreator constructor(
        @JsonProperty("maxDato") val maxDato: LocalDate?,
        @JsonProperty("totaltForbruktKvote") val totaltForbruktKvote: BigDecimal
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Utbetalingsgrader @JsonCreator constructor(
    @JsonProperty("arbeidsforhold") val arbeidsforhold: Arbeidsforhold,
    @JsonProperty("normalArbeidstid") val normalArbeidstid: Duration,
    @JsonProperty("faktiskArbeidstid") val faktiskArbeidstid: Duration?,
    @JsonProperty("utbetalingsgrad") val utbetalingsgrad: Prosent,
    @JsonProperty("tilkommet") val tilkommet: Boolean? = null
) {
    constructor(arbeidsforhold: Arbeidsforhold, normalArbeidstid: Duration, faktiskArbeidstid: Duration?, utbetalingsgrad: Prosent) : this(arbeidsforhold, normalArbeidstid, faktiskArbeidstid, utbetalingsgrad, false)
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UttaksperiodeInfo @JsonCreator constructor(
    @JsonProperty("utfall") val utfall: Utfall,
    @JsonProperty("uttaksgrad") val uttaksgrad: Prosent,
    @JsonProperty("uttaksgradMedReduksjonGrunnetInntektsgradering") val uttaksgradMedReduksjonGrunnetInntektsgradering: Prosent?,
    @JsonProperty("uttaksgradUtenReduksjonGrunnetInntektsgradering") val uttaksgradUtenReduksjonGrunnetInntektsgradering: Prosent?,
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
    @JsonProperty("beredskap") val beredskap: Utfall?,
    @JsonProperty("endringsstatus") val endringsstatus: Endringsstatus? = null,
    @JsonProperty("utenlandsoppholdUtenÅrsak") val utenlandsoppholdUtenÅrsak: Boolean = false,
    @JsonProperty("utenlandsopphold") val utenlandsopphold: Utenlandsopphold? = null,
    @JsonProperty("manueltOverstyrt") val manueltOverstyrt: Boolean = false
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
            beredskap: Utfall?,
            utenlandsopphold: Utenlandsopphold?,
            manueltOverstyrt: Boolean = false): UttaksperiodeInfo {

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
                beredskap = beredskap,
                utenlandsopphold = utenlandsopphold,
                manueltOverstyrt = manueltOverstyrt,
                uttaksgradMedReduksjonGrunnetInntektsgradering = null,
                uttaksgradUtenReduksjonGrunnetInntektsgradering = null
                )
        }

        fun oppfylt(
            uttaksgrad: Prosent,
            uttaksgradUtenReduksjonGrunnetInntektsgradering: Prosent?,
            uttaksgradMedReduksjonGrunnetInntektsgradering: Prosent?,
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
            beredskap: Utfall?,
            utenlandsopphold: Utenlandsopphold?,
            manueltOverstyrt: Boolean = false): UttaksperiodeInfo {

            require(årsak.oppfylt) {
                "Kan ikke sette periode til oppfylt med årsak som ikke er for oppfylt. ($årsak)"
            }

            return UttaksperiodeInfo(
                utfall = Utfall.OPPFYLT,
                uttaksgrad = uttaksgrad,
                uttaksgradUtenReduksjonGrunnetInntektsgradering = uttaksgradUtenReduksjonGrunnetInntektsgradering,
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
                beredskap = beredskap,
                utenlandsopphold = utenlandsopphold,
                manueltOverstyrt = manueltOverstyrt,
                uttaksgradMedReduksjonGrunnetInntektsgradering = uttaksgradMedReduksjonGrunnetInntektsgradering,
            )
        }

    }

    @JsonProperty("søkersTapteTimer")
    fun getSøkersTapteTimer(): Duration {
        var sumNormalTid = Duration.ZERO
        var sumFaktiskTid = Duration.ZERO
        utbetalingsgrader.forEach { sumNormalTid += it.normalArbeidstid }
        utbetalingsgrader.forEach {
            if (it.faktiskArbeidstid != null) {
                sumFaktiskTid += if (it.faktiskArbeidstid > it.normalArbeidstid) {
                    it.normalArbeidstid
                } else {
                    it.faktiskArbeidstid
                }
            }
        }
        return sumNormalTid - sumFaktiskTid
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class GraderingMotTilsyn @JsonCreator constructor(
    @JsonProperty("etablertTilsyn") val etablertTilsyn: Prosent,
    @JsonProperty("overseEtablertTilsynÅrsak") val overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?,
    @JsonProperty("andreSøkeresTilsyn") val andreSøkeresTilsyn: Prosent,
    @JsonProperty("andreSøkeresTilsynReberegnet") val andreSøkeresTilsynReberegnet: Boolean,
    @JsonProperty("tilgjengeligForSøker") val tilgjengeligForSøker: Prosent
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Utenlandsopphold @JsonCreator constructor(
    @JsonProperty("erEøsLand") val erEøsLand: Boolean?,
    @JsonProperty("landkode") val landkode: String? = null,
    @JsonProperty("årsak") val årsak: UtenlandsoppholdÅrsak = UtenlandsoppholdÅrsak.INGEN
) {
    constructor(landkode: String?, utenlandsoppholdÅrsak: UtenlandsoppholdÅrsak) : this(
            RegionUtil().erIEØS(landkode),
            landkode,
            utenlandsoppholdÅrsak
    )
}
