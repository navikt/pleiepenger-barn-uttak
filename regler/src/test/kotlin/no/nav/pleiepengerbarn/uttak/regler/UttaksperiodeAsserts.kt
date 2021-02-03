package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*

internal object UttaksperiodeAsserts {

    internal fun sjekkOppfylt(
        uttaksplan: Uttaksplan,
        forventetPeriode: LukketPeriode,
        forventetGrad:Prosent,
        forventedeUtbetalingsgrader: Map<String,Prosent> = mapOf(),
        forventedeOppfyltÅrsak: Årsak) {
        val uttaksperiodeInfo = uttaksplan.perioder.forsikreAtDetIkkeErSortedMap()[forventetPeriode]
        assertThat(uttaksperiodeInfo).isNotNull()
        assertThat(uttaksperiodeInfo!!.utfall).isEqualTo(Utfall.OPPFYLT)
        assertThat(uttaksperiodeInfo.uttaksgrad).isEqualByComparingTo(forventetGrad)

        forventedeUtbetalingsgrader.somUtbetalingsgrader().forEach { forventet ->
            val utbetalingsgrader = uttaksperiodeInfo.utbetalingsgrader.hentForArbeidsforhold(forventet.arbeidsforhold)
            assertThat(forventet.utbetalingsgrad).isEqualByComparingTo(utbetalingsgrader.utbetalingsgrad)
        }

//TODO: legg inn igjen oppfylt årsak igjen når den settes riktig
//        assertThat(oppfyltPeriode.årsak).isEqualTo(forventedeOppfyltÅrsak)

    }

    internal fun sjekkIkkeOppfylt(
        uttaksplan: Uttaksplan,
        forventetPeriode: LukketPeriode,
        forventetIkkeOppfyltÅrsaker:Set<Årsak>) {
        val uttaksperiodeInfo = uttaksplan.perioder.forsikreAtDetIkkeErSortedMap()[forventetPeriode]
        assertNotNull(uttaksperiodeInfo)
        assertThat(uttaksperiodeInfo!!.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(uttaksperiodeInfo.årsaker).isEqualTo(forventetIkkeOppfyltÅrsaker)
    }

    internal fun sjekkIkkeOppfyltPeriodeInneholderIkkeOppfyltÅrsaker(
        uttaksplan: Uttaksplan,
        forventetPeriode: LukketPeriode,
        forventetIkkeOppfyltÅrsaker :Set<Årsak>) {
        val uttaksperiodeInfo = uttaksplan.perioder.forsikreAtDetIkkeErSortedMap()[forventetPeriode]
        assertTrue(uttaksperiodeInfo != null)
        assertThat(uttaksperiodeInfo!!.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertTrue(uttaksperiodeInfo.årsaker.containsAll(forventetIkkeOppfyltÅrsaker))
    }

}

// Om det er et sorted map på fom/tom får man treff på å hente ut fra mappet
// kun om TOM/FOM er lik, ikke hele perioden. Ved å alltid gjøre det om til et Map før
// Assertion får vi kun treff på faktiske matchene perioder
private fun Map<LukketPeriode, UttaksperiodeInfo>.forsikreAtDetIkkeErSortedMap() = toMap()

private fun List<Utbetalingsgrader>.hentForArbeidsforhold(arbeidsforhold: Arbeidsforhold) =
        firstOrNull { it.arbeidsforhold == arbeidsforhold} ?: throw IllegalStateException("Fant ikke utbetalingsgrad for $arbeidsforhold")
