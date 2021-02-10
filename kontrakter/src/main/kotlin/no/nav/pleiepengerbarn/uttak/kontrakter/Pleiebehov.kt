package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class Pleiebehov(
        @get:JsonValue val prosent: Prosent) {
    PROSENT_0(Prosent.ZERO),
    PROSENT_100(Prosent(100)),
    PROSENT_200(Prosent(200));

    fun prosent() = prosent

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraProsent(prosent: Prosent): Pleiebehov =
                values()
                .firstOrNull { it.prosent.compareTo(prosent) == 0 }
                ?: throw IllegalArgumentException("Ikke støttet størrelse på tilsyn ${prosent.toPlainString()}")
    }
}
