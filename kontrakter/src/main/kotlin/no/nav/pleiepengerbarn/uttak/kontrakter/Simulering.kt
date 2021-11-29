package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.*
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class Simulering @JsonCreator constructor(
    @JsonProperty("forrigeUttaksplan") val forrigeUttaksplan: Uttaksplan?,
    @JsonProperty("simulertUttaksplan") val simulertUttaksplan: Uttaksplan,
    @JsonProperty("uttakplanEndret") val uttakplanEndret: Boolean
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SimuleringLivetsSluttfase @JsonCreator constructor(
        @JsonProperty("forrigeUttaksplan") val forrigeUttaksplan: Uttaksplan?,
        @JsonProperty("simulertUttaksplan") val simulertUttaksplan: Uttaksplan,
        @JsonProperty("uttakplanEndret") val uttakplanEndret: Boolean,
        @JsonProperty("overstigerKvoten") val overstigerKvoten: Boolean,
        @JsonProperty("antallBrukteDager") val antallBrukteDager: BigDecimal
)
