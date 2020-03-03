package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue

import java.math.RoundingMode

internal object UttaksperiodeAsserts {

    internal fun sjekkInnvilget(uttaksplan: Uttaksplan, forventetPeriode: LukketPeriode, forventetGrad:Prosent) {
        val uttaksperiodeInfo = uttaksplan.perioder[forventetPeriode]
        assertTrue(uttaksperiodeInfo != null)
        assertThat(uttaksperiodeInfo is InnvilgetPeriode).isEqualTo(true)
        val innvilgetPeriode = uttaksperiodeInfo as InnvilgetPeriode
        assertThat(innvilgetPeriode.grad).isEqualByComparingTo(forventetGrad.setScale(2, RoundingMode.HALF_EVEN))
    }

    internal fun sjekkInnvilget(uttaksperiode: Uttaksperiode, forventetPeriode: LukketPeriode, forventetGrad:Prosent) {
        sjekkPeriode(uttaksperiode, forventetPeriode)
        assertThat(uttaksperiode.value is InnvilgetPeriode).isEqualTo(true)
        val innvilgetPeriode = uttaksperiode.value as InnvilgetPeriode
        assertThat(innvilgetPeriode.grad).isEqualByComparingTo(forventetGrad.setScale(2, RoundingMode.HALF_EVEN))
    }

    internal fun sjekkAvslått(uttaksperiode: Uttaksperiode, forventetPeriode: LukketPeriode, forventedAvslagsÅrsaker:Set<AvslåttPeriodeÅrsak>) {
        sjekkPeriode(uttaksperiode, forventetPeriode)
        assertThat(uttaksperiode.value is AvslåttPeriode).isEqualTo(true)
        val avslåttPeriode = uttaksperiode.value as AvslåttPeriode
        assertThat(avslåttPeriode.avslagsÅrsaker).isEqualTo(forventedAvslagsÅrsaker)
    }

    private fun sjekkPeriode(uttaksperiode: Uttaksperiode, forventetPeriode: LukketPeriode) {
        assertThat(uttaksperiode.key.fom).isEqualTo(forventetPeriode.fom)
        assertThat(uttaksperiode.key.tom).isEqualTo(forventetPeriode.tom)
        assertThat(uttaksperiode.value).isNotNull
    }
}