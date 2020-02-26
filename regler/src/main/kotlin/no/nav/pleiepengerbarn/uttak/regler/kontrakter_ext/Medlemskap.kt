package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Medlemskap

internal fun Map<LukketPeriode, Medlemskap>.ikkeMedlem(
        søknadsperioder: List<LukketPeriode>
) : List<LukketPeriode> {
    val uttaksperiode = søknadsperioder.overordnetPeriode()
    return uttaksperiode.perioderSomIkkeInngårI(this)
}
