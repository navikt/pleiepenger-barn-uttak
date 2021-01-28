package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*

internal fun Uttaksplan.tilForenkletUttaksplan(): ForenkletUttaksplan {
    val arbeidsforholdSet = mutableSetOf<Arbeidsforhold>()
    perioder
        .filter { it.value.utfall == Utfall.INNVILGET }
        .map { it.value }
        .forEach { it.utbetalingsgrader.forEach { utbetalingsgrad -> arbeidsforholdSet.add(utbetalingsgrad.arbeidsforhold) } }

    val aktiviteter = arbeidsforholdSet.map { arbeidsforhold ->
        Aktivitet(
            arbeidsforhold = arbeidsforhold,
            uttaksperioder = perioder.map { it.value.tilForenkletUttaksperiode(it.key, arbeidsforhold) }
        )
    }
    return ForenkletUttaksplan(aktiviteter = aktiviteter)
}

private fun UttaksperiodeInfo.tilForenkletUttaksperiode(periode: LukketPeriode, arbeidsforhold: Arbeidsforhold): ForenkletUttaksperiode {
    val periodeInfo = this
    return when (periodeInfo.utfall) {
        Utfall.INNVILGET -> {
            ForenkletUttaksperiode(periode = periode, innvilget = true, utbetalingsgrad = periodeInfo.utbetalingsgrader.finnFor(arbeidsforhold))
        }
        Utfall.AVSLÅTT -> {
            ForenkletUttaksperiode(periode = periode, innvilget = false, utbetalingsgrad = Prosent.ZERO)
        }
        else -> {
            throw IllegalStateException("Skal ikke kunne være andre sub-klasser enn InnvilgetPeriode og AvslåttPeriode.")
        }
    }
}

private fun List<Utbetalingsgrader>.finnFor(arbeidsforhold: Arbeidsforhold): Prosent {
    return find { it.arbeidsforhold == arbeidsforhold } ?.utbetalingsgrad ?: Prosent.ZERO
}