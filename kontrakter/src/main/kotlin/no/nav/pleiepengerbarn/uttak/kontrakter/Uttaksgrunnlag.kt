package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Uttaksgrunnlag (
    @JsonProperty("ytelseType") val ytelseType: YtelseType = YtelseType.PSB,
    @JsonProperty("barn") val barn: Barn,
    @JsonProperty("søker") val søker: Søker,
    @JsonProperty("saksnummer") val saksnummer: Saksnummer,
    @JsonProperty("behandlingUUID") val behandlingUUID: BehandlingUUID,

    @JsonProperty("søktUttak") val søktUttak: List<SøktUttak>,
    @JsonProperty("trukketUttak") val trukketUttak: List<LukketPeriode> = listOf(),
    @JsonProperty("arbeid") val arbeid: List<Arbeid>,
    @JsonProperty("pleiebehov") val pleiebehov: Map<LukketPeriode, Pleiebehov>,
    @JsonProperty("nyeReglerUtbetalingsgrad") val nyeReglerUtbetalingsgrad: LocalDate? = null,

    @JsonProperty("overstyrtInput") val overstyrtInput: Map<LukketPeriode, OverstyrtInput> = mapOf(),
    @JsonProperty("lovbestemtFerie") val lovbestemtFerie: List<LukketPeriode> = listOf(),
    @JsonProperty("inngangsvilkår") val inngangsvilkår: Map<String, List<Vilkårsperiode>> = mapOf(),
    @JsonProperty("tilsynsperioder") val tilsynsperioder: Map<LukketPeriode, Duration> = mapOf(),
    @JsonProperty("beredskapsperioder") val beredskapsperioder: Map<LukketPeriode, Utfall> = mapOf(),
    @JsonProperty("nattevåksperioder") val nattevåksperioder: Map<LukketPeriode, Utfall> = mapOf(),
    @JsonProperty("kravprioritetForBehandlinger") val kravprioritetForBehandlinger: Map<LukketPeriode, List<BehandlingUUID>> = mapOf(),
    @JsonProperty("sisteVedtatteUttaksplanForBehandling") val sisteVedtatteUttaksplanForBehandling: Map<BehandlingUUID, BehandlingUUID?> = mapOf(),
    @JsonProperty("utenlandsoppholdperioder") val utenlandsoppholdperioder: Map<LukketPeriode, UtenlandsoppholdInfo> = mapOf()
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Vilkårsperiode(
    @JsonProperty("periode") val periode: LukketPeriode,
    @JsonProperty("utfall") val utfall: Utfall
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SøktUttak(
    @JsonProperty("periode") val periode: LukketPeriode,
    @JsonProperty("oppgittTilsyn") val oppgittTilsyn: Duration? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UtenlandsoppholdInfo(
    @JsonProperty("utenlandsoppholdÅrsak") val utenlandsoppholdÅrsak: UtenlandsoppholdÅrsak,
    @JsonProperty("landkode") val landkode: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class OverstyrtInput(
    @JsonProperty("overstyrtUttaksgrad") val overstyrtUttaksgrad: BigDecimal,
    @JsonProperty("arbeidsforhold") val arbeidsforhold: Arbeidsforhold
)
