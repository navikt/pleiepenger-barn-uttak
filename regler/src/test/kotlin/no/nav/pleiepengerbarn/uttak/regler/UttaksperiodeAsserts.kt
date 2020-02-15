package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttPeriodeÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode
import org.assertj.core.api.Assertions.assertThat
import java.math.RoundingMode

object UttaksperiodeAsserts {

    internal fun sjekkInnvilget(uttaksperiode: Uttaksperiode, forventetPeriode: LukketPeriode, utbetalingsgrad:Prosent) {
        sjekkPeriode(uttaksperiode, forventetPeriode)
        assertThat(uttaksperiode.uttaksperiodeResultat.grad).isEqualByComparingTo(utbetalingsgrad.setScale(2, RoundingMode.HALF_EVEN))
        assertThat(uttaksperiode.uttaksperiodeResultat.avslåttPeriodeÅrsaker).isEmpty()

    }

    internal fun sjekkAvslått(uttaksperiode: Uttaksperiode, forventetPeriode: LukketPeriode, årsaker:Set<AvslåttPeriodeÅrsak>) {
        sjekkPeriode(uttaksperiode, forventetPeriode)
        assertThat(uttaksperiode.uttaksperiodeResultat.grad).isEqualByComparingTo(Prosent.ZERO.setScale(2, RoundingMode.HALF_EVEN))
        assertThat(uttaksperiode.uttaksperiodeResultat.avslåttPeriodeÅrsaker).isEqualTo(årsaker)
    }

    private fun sjekkPeriode(uttaksperiode: Uttaksperiode, forventetPeriode: LukketPeriode) {
        assertThat(uttaksperiode.periode.fom).isEqualTo(forventetPeriode.fom)
        assertThat(uttaksperiode.periode.tom).isEqualTo(forventetPeriode.tom)
        assertThat(uttaksperiode.uttaksperiodeResultat).isNotNull

    }
}