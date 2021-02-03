package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*

data class UttaksgradResultat(
    val restTilSøker: Prosent,
    val uttaksgrad: Prosent,
    val oppfyltÅrsak: Årsak? = null,
    val ikkeOppfyltÅrsak: Årsak? = null
) {
    init {
        //Enten oppfylt årsak eller ikke oppfylt årsak
        require(!(oppfyltÅrsak == null && ikkeOppfyltÅrsak == null) && !(oppfyltÅrsak != null && ikkeOppfyltÅrsak != null)) {"Enten oppfylt årsak eller ikke oppfylt årsak skal være satt, ikke begge eller ingen."}
    }

    fun årsak(): Årsak {
        if (oppfyltÅrsak != null) {
            return oppfyltÅrsak
        }
        return ikkeOppfyltÅrsak!!
    }
}