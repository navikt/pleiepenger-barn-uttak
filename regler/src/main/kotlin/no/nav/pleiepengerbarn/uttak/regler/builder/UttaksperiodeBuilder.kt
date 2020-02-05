package no.nav.pleiepengerbarn.uttak.regler.builder

import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode
import java.time.LocalDate

class UttaksperiodeBuilder {
    var fom:LocalDate? = null
        private

    fun fom(fom:LocalDate):UttaksperiodeBuilder {
        this.fom = fom
        return this
    }
/*
    fun build():Uttaksperiode {
        return Uttaksperiode(fom, tom)
    }
*/
}