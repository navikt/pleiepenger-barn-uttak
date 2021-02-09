package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Duration

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Uttaksgrunnlag (
    @JsonProperty("barn") val barn: Barn,
    @JsonProperty("søker") val søker: Søker,
    @JsonProperty("saksnummer") val saksnummer: Saksnummer,
    @JsonProperty("behandlingUUID") val behandlingUUID: BehandlingUUID,
    @JsonProperty("andrePartersSaksnummer") val andrePartersSaksnummer: List<Saksnummer> = listOf(),

    @JsonProperty("søknadsperioder") val søknadsperioder: List<LukketPeriode>,
    @JsonProperty("arbeid") val arbeid: List<Arbeid>,
    @JsonProperty("tilsynsbehov") val pleiebehov: Map<LukketPeriode, Pleiebehov>,

    @JsonProperty("lovbestemtFerie") val lovbestemtFerie: List<LukketPeriode> = listOf(),
    @JsonProperty("inngangsvilkår") val inngangsvilkår: Map<String, List<Vilkårsperiode>> = mapOf(),
    @JsonProperty("tilsynsperioder") val tilsynsperioder: Map<LukketPeriode, Duration> = mapOf()
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Vilkårsperiode(
    @JsonProperty("periode") val periode: LukketPeriode,
    @JsonProperty("utfall") val utfall: Utfall
)