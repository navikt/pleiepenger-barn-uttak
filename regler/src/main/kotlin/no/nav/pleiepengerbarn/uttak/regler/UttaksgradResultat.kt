package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*

data class UttaksgradResultat(
        val uttaksgrad: Prosent,
        val innvilgetÅrsak: Årsak? = null,
        val avslåttÅrsak: Årsak? = null
) {
    init {
        //Enten innvilget årsak eller avslått årsak
        require(!(innvilgetÅrsak == null && avslåttÅrsak == null) && !(innvilgetÅrsak != null && avslåttÅrsak != null)) {"Enten innvilgelseårsak eller avslåttårsak skal være satt, ikke begge eller ingen."}
    }

    fun årsak(): Årsak {
        if (innvilgetÅrsak != null) {
            return innvilgetÅrsak
        }
        return avslåttÅrsak!!
    }
}