package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeResultat
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import java.math.BigDecimal


object UttaksplanRegler {

    private val FULL_UTBETALING = BigDecimal(100)

    fun fastsettUttaksplan(uttaksplan: Uttaksplan, arbeidsforholdListe:Set<Arbeidsforhold>):Uttaksplan {
        uttaksplan.perioder.forEach { periode ->
            arbeidsforholdListe.forEach {
                periode.uttaksperiodeResultat = UttaksperiodeResultat(arbeidsforhold = it, utbetalingsgrad = FULL_UTBETALING)
            }
        }
        return uttaksplan
    }

}