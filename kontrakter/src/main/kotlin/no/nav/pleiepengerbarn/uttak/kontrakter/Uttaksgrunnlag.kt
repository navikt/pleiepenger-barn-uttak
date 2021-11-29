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

    @JsonProperty("søktUttak") val søktUttak: List<SøktUttak>,
    @JsonProperty("trukketUttak") val trukketUttak: List<LukketPeriode> = listOf(),
    @JsonProperty("arbeid") val arbeid: List<Arbeid>,
    @JsonProperty("pleiebehov") val pleiebehov: Map<LukketPeriode, Pleiebehov>,

    @JsonProperty("lovbestemtFerie") val lovbestemtFerie: List<LukketPeriode> = listOf(),
    @JsonProperty("inngangsvilkår") val inngangsvilkår: Map<String, List<Vilkårsperiode>> = mapOf(),
    @JsonProperty("tilsynsperioder") val tilsynsperioder: Map<LukketPeriode, Duration> = mapOf(),
    @JsonProperty("beredskapsperioder") val beredskapsperioder: Map<LukketPeriode, Utfall> = mapOf(),
    @JsonProperty("nattevåksperioder") val nattevåksperioder: Map<LukketPeriode, Utfall> = mapOf(),
    @JsonProperty("kravprioritetForBehandlinger") val kravprioritetForBehandlinger: Map<LukketPeriode, List<BehandlingUUID>> = mapOf()

)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class UttaksgrunnlagLivetsSluttfase (
        @JsonProperty("barn") val barn: Barn, // pleietrengende
        @JsonProperty("søker") val søker: Søker,
        @JsonProperty("saksnummer") val saksnummer: Saksnummer,
        @JsonProperty("behandlingUUID") val behandlingUUID: BehandlingUUID,

        @JsonProperty("søktUttak") val søktUttak: List<SøktUttak>,
        @JsonProperty("trukketUttak") val trukketUttak: List<LukketPeriode> = listOf(),
        @JsonProperty("arbeid") val arbeid: List<Arbeid>,

        @JsonProperty("lovbestemtFerie") val lovbestemtFerie: List<LukketPeriode> = listOf(),
        @JsonProperty("inngangsvilkår") val inngangsvilkår: Map<String, List<Vilkårsperiode>> = mapOf(),
        @JsonProperty("kravprioritetForBehandlinger") val kravprioritetForBehandlinger: Map<LukketPeriode, List<BehandlingUUID>> = mapOf()

) {
    companion object {
        fun UttaksgrunnlagLivetsSluttfase.tilUttaksgrunnlag(): Uttaksgrunnlag {
            return Uttaksgrunnlag(
                    barn = this.barn,
                    søker = this.søker,
                    saksnummer = this.saksnummer,
                    behandlingUUID = this.behandlingUUID,
                    søktUttak = this.søktUttak,
                    trukketUttak = this.trukketUttak,
                    arbeid = this.arbeid,
                    pleiebehov =  this.søktUttak.tilPleiebehov(), // 6000 prosent // emptyMap(),
                    lovbestemtFerie = this.lovbestemtFerie,
                    inngangsvilkår = this.inngangsvilkår,
                    tilsynsperioder = emptyMap(),
                    beredskapsperioder = emptyMap(),
                    nattevåksperioder = emptyMap(),
                    kravprioritetForBehandlinger = this.kravprioritetForBehandlinger
            )
        }
        private fun List<SøktUttak>.tilPleiebehov(): Map<LukketPeriode, Pleiebehov> {
            val pleiebehov = mutableMapOf<LukketPeriode, Pleiebehov>()
            this.forEach {
                pleiebehov[it.periode] = Pleiebehov.PROSENT_6000
            }
            return pleiebehov
        }
    }

}

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