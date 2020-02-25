package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat

import java.math.RoundingMode

internal object UttaksperiodeAsserts {

    internal fun sjekkInnvilget(uttaksperiode: UttaksperiodeV2, forventetPeriode: LukketPeriode, forventetGrad:Prosent) {
        sjekkPeriode(uttaksperiode, forventetPeriode)
        assertThat(uttaksperiode.value is InnvilgetPeriode).isEqualTo(true)
        val innvilgetPeriode = uttaksperiode.value as InnvilgetPeriode
        assertThat(innvilgetPeriode.grad).isEqualByComparingTo(forventetGrad.setScale(2, RoundingMode.HALF_EVEN))
    }

    internal fun sjekkAvslått(uttaksperiode: UttaksperiodeV2, forventetPeriode: LukketPeriode, forventedAvslagsÅrsaker:Set<AvslåttPeriodeÅrsak>) {
        sjekkPeriode(uttaksperiode, forventetPeriode)
        assertThat(uttaksperiode.value is AvslåttPeriode).isEqualTo(true)
        val avslåttPeriode = uttaksperiode.value as AvslåttPeriode
        assertThat(avslåttPeriode.avslagsÅrsaker).isEqualTo(forventedAvslagsÅrsaker)
    }

    private fun sjekkPeriode(uttaksperiode: UttaksperiodeV2, forventetPeriode: LukketPeriode) {
        assertThat(uttaksperiode.key.fom).isEqualTo(forventetPeriode.fom)
        assertThat(uttaksperiode.key.tom).isEqualTo(forventetPeriode.tom)
        assertThat(uttaksperiode.value).isNotNull
    }
}