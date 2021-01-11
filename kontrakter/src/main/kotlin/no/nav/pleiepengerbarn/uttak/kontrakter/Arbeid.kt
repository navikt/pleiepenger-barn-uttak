package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Duration

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Arbeid(
    @JsonProperty("arbeidsforhold") val arbeidsforhold: Arbeidsforhold,
    @JsonProperty("perioder") val perioder: Map<LukketPeriode, ArbeidsforholdPeriodeInfo>
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Arbeidsforhold(
    @JsonProperty("type") val type: String,
    @JsonProperty("organisasjonsnummer") val organisasjonsnummer: String? = null,
    @JsonProperty("aktørId") val aktørId: String? = null,
    @JsonProperty("arbeidsforholdId") val arbeidsforholdId: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class ArbeidsforholdPeriodeInfo(
    @JsonProperty("jobberNormalt") val jobberNormalt: Duration,
    @JsonProperty("taptArbeidstid") val taptArbeidstid: Duration,
    @JsonProperty("søkersTilsyn") val søkersTilsyn: Duration
) {
    init {
        require(taptArbeidstid <= jobberNormalt) {"Tapt arbeidstid ($taptArbeidstid) kan ikke være mer en jobber normalt ($jobberNormalt)."}
        require(søkersTilsyn <= Duration.ofHours(7).plusMinutes(30)) {"Søkers tilsyn ($søkersTilsyn) kan ikke være mer enn 7 timer og 30 minutter."}
    }
}