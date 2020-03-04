package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

internal object UttaksperiodeAsserts {

    internal fun sjekkInnvilget(uttaksplan: Uttaksplan, forventetPeriode: LukketPeriode, forventetGrad:Prosent, forventedeUtbetalingsgrader:Map<Arbeidsforhold,Prosent> = mapOf()) {
        val uttaksperiodeInfo = uttaksplan.perioder[forventetPeriode]
        assertTrue(uttaksperiodeInfo != null)
        assertThat(uttaksperiodeInfo is InnvilgetPeriode).isEqualTo(true)
        val innvilgetPeriode = uttaksperiodeInfo as InnvilgetPeriode
        assertThat(innvilgetPeriode.grad).isEqualByComparingTo(forventetGrad)
        forventedeUtbetalingsgrader.forEach { (arbeidsforhold, forventetUtbetalingsgrad) ->
            val arbeidsforholdOgUtbetalingsgrad = innvilgetPeriode.utbetalingsgrader.find {it.arbeidsforhold == arbeidsforhold}
            assertNotNull(arbeidsforholdOgUtbetalingsgrad)
            assertThat(arbeidsforholdOgUtbetalingsgrad?.utbetalingsgrad).isEqualByComparingTo(forventetUtbetalingsgrad)
        }
    }

    internal fun sjekkAvslått(uttaksplan: Uttaksplan, forventetPeriode: LukketPeriode, forventedAvslagsÅrsaker:Set<AvslåttPeriodeÅrsak>) {
        val uttaksperiodeInfo = uttaksplan.perioder[forventetPeriode]
        assertTrue(uttaksperiodeInfo != null)
        assertThat(uttaksperiodeInfo is AvslåttPeriode).isEqualTo(true)
        val avslåttPeriode = uttaksperiodeInfo as AvslåttPeriode
        assertThat(avslåttPeriode.avslagsÅrsaker).isEqualTo(forventedAvslagsÅrsaker)
    }

}