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

    internal fun sjekkAvslått(uttaksplan: Uttaksplan, forventetPeriode: LukketPeriode, forventedAvslagsÅrsaker:Set<AvslåttPeriodeÅrsak>) {
        val uttaksperiodeInfo = uttaksplan.perioder[forventetPeriode]
        assertTrue(uttaksperiodeInfo != null)
        assertThat(uttaksperiodeInfo is AvslåttPeriode).isEqualTo(true)
        val avslåttPeriode = uttaksperiodeInfo as AvslåttPeriode
        assertThat(avslåttPeriode.avslagsÅrsaker).isEqualTo(forventedAvslagsÅrsaker)
    }

}