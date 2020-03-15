package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*

internal object UttaksperiodeAsserts {

    internal fun sjekkInnvilget(
            uttaksplan: Uttaksplan,
            forventetPeriode: LukketPeriode,
            forventetGrad:Prosent,
            forventedeUtbetalingsgrader: Map<String,Prosent> = mapOf(),
            forventedeInnvilgetÅrsak: InnvilgetÅrsaker) {
        val uttaksperiodeInfo = uttaksplan.perioder.forsikreAtDetIkkeErSortedMap()[forventetPeriode]
        assertTrue(uttaksperiodeInfo != null)
        assertThat(uttaksperiodeInfo is InnvilgetPeriode).isEqualTo(true)
        val innvilgetPeriode = uttaksperiodeInfo as InnvilgetPeriode
        assertThat(innvilgetPeriode.grad).isEqualByComparingTo(forventetGrad)

        forventedeUtbetalingsgrader.somUtbetalingsgrader().forEach { forventet ->
            val innvilgetUtbetalingsgrader = innvilgetPeriode.utbetalingsgrader.hentForArbeidsforhold(forventet.arbeidsforhold)
            assertThat(forventet.utbetalingsgrad).isEqualByComparingTo(innvilgetUtbetalingsgrader.utbetalingsgrad)
        }

        assertThat(innvilgetPeriode.årsak).isEqualTo(forventedeInnvilgetÅrsak)

    }

    internal fun sjekkAvslått(
            uttaksplan: Uttaksplan,
            forventetPeriode: LukketPeriode,
            forventetAvslåttÅrsaker:Set<AvslåttÅrsaker>) {
        val uttaksperiodeInfo = uttaksplan.perioder.forsikreAtDetIkkeErSortedMap()[forventetPeriode]
        assertNotNull(uttaksperiodeInfo)
        assertThat(uttaksperiodeInfo is AvslåttPeriode).isEqualTo(true)
        val avslåttPeriode = uttaksperiodeInfo as AvslåttPeriode
        assertThat(avslåttPeriode.årsaker.map { it.årsak.name }).isEqualTo(forventetAvslåttÅrsaker.map { it.name })
    }

    internal fun sjekkAvslåttInneholderAvslåttÅrsaker(
            uttaksplan: Uttaksplan,
            forventetPeriode: LukketPeriode,
            forventetAvslåttÅrsaker :Set<AvslåttÅrsaker>) {
        val uttaksperiodeInfo = uttaksplan.perioder.forsikreAtDetIkkeErSortedMap()[forventetPeriode]
        assertTrue(uttaksperiodeInfo != null)
        assertThat(uttaksperiodeInfo is AvslåttPeriode).isEqualTo(true)
        val avslåttPeriode = uttaksperiodeInfo as AvslåttPeriode
        assertTrue(avslåttPeriode.årsaker.map { it.årsak }.containsAll(forventetAvslåttÅrsaker))
    }

}

// Om det er et sorted map på fom/tom får man treff på å hente ut fra mappet
// kun om TOM/FOM er lik, ikke hele perioden. Ved å alltid gjøre det om til et Map før
// Assertion får vi kun treff på faktiske matchene perioder
private fun Map<LukketPeriode, UttaksPeriodeInfo>.forsikreAtDetIkkeErSortedMap() = toMap()

private fun List<Utbetalingsgrader>.hentForArbeidsforhold(arbeidsforhold: ArbeidsforholdReferanse) =
        firstOrNull { it.arbeidsforhold == arbeidsforhold} ?: throw IllegalStateException("Fant ikke utbetalingsgrad for $arbeidsforhold")
