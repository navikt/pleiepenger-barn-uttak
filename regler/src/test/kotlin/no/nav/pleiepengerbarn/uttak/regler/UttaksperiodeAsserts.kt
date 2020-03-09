package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

internal object UttaksperiodeAsserts {

    internal fun sjekkInnvilget(
            uttaksplan: Uttaksplan,
            forventetPeriode: LukketPeriode,
            forventetGrad:Prosent,
            forventedeUtbetalingsgrader:Map<ArbeidsforholdRef,Prosent> = mapOf(),
            forventedeInnvilgetÅrsak: InnvilgetÅrsaker) {
        val uttaksperiodeInfo = uttaksplan.perioder[forventetPeriode]
        assertTrue(uttaksperiodeInfo != null)
        assertThat(uttaksperiodeInfo is InnvilgetPeriode).isEqualTo(true)
        val innvilgetPeriode = uttaksperiodeInfo as InnvilgetPeriode
        assertThat(innvilgetPeriode.grad).isEqualByComparingTo(forventetGrad)
        forventedeUtbetalingsgrader.forEach { (arbeidsforholdRef, forventetUtbetalingsgrad) ->
            val utbetalingsgrad =innvilgetPeriode.utbetalingsgrader[arbeidsforholdRef]
            assertNotNull(utbetalingsgrad)
            assertThat(utbetalingsgrad).isEqualByComparingTo(forventetUtbetalingsgrad)
        }
        assertThat(innvilgetPeriode.årsak.årsak()).isEqualTo(forventedeInnvilgetÅrsak.name)

    }

    internal fun sjekkAvslått(
            uttaksplan: Uttaksplan,
            forventetPeriode: LukketPeriode,
            forventetAvslåttÅrsaker:Set<AvslåttÅrsaker>) {
        val uttaksperiodeInfo = uttaksplan.perioder[forventetPeriode]
        assertTrue(uttaksperiodeInfo != null)
        assertThat(uttaksperiodeInfo is AvslåttPeriode).isEqualTo(true)
        val avslåttPeriode = uttaksperiodeInfo as AvslåttPeriode
        assertThat(avslåttPeriode.årsaker.map { it.årsak.name }).isEqualTo(forventetAvslåttÅrsaker.map { it.name })
    }

}