package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class TilsynsbehovStørrelse(
        @get:JsonValue val prosent: Prosent) {
    PROSENT_100(Prosent(100)),
    PROSENT_200(Prosent(200));
    // TODO: HMM 1000 prosent.. ?

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraProsent(prosent: Prosent): TilsynsbehovStørrelse =
                values()
                .firstOrNull { it.prosent.compareTo(prosent) == 0 }
                ?: throw IllegalArgumentException("Ikke støttet størrelse på tilsyn ${prosent.toPlainString()}")
    }
}

data class Tilsynsbehov(
        val prosent: TilsynsbehovStørrelse
)