package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.MedlemskapPeriodeInfo

internal fun Map<LukketPeriode, MedlemskapPeriodeInfo>.ikkeMedlem(
        søknadsperioder: Map<LukketPeriode, *>
) : List<LukketPeriode> {
    val uttaksperiode = søknadsperioder.overordnetPeriode()
    return uttaksperiode.perioderSomIkkeInngårI(this)
}


