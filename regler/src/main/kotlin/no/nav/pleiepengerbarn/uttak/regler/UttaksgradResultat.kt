package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*

data class UttaksgradResultat(
    val restTilSøker: Prosent,
    val uttaksgrad: Prosent,
    val overstyrtUttaksgrad: Prosent? = null,
    val oppfyltÅrsak: Årsak? = null,
    val ikkeOppfyltÅrsak: Årsak? = null,
    val overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?
) {
    init {
        //Enten oppfylt årsak eller ikke oppfylt årsak
        require(!(oppfyltÅrsak == null && ikkeOppfyltÅrsak == null)) {"Både oppfylt årsak eller ikke oppfylt årsak kan ikke være null."}
        require(!(oppfyltÅrsak != null && ikkeOppfyltÅrsak != null)) {"Enten må oppfylt årsak eller ikke oppfylt årsak være satt."}
    }

    fun årsak(): Årsak {
        if (oppfyltÅrsak != null) {
            return oppfyltÅrsak
        }
        return ikkeOppfyltÅrsak!!
    }
}