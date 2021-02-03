package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*

internal fun Uttaksplan.tilForenkletUttaksplan(): ForenkletUttaksplan {
    val arbeidsforholdSet = mutableSetOf<Arbeidsforhold>()
    perioder
        .filter { it.value.utfall == Utfall.OPPFYLT }
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
        Utfall.OPPFYLT -> {
            ForenkletUttaksperiode(periode = periode, oppfylt = true, utbetalingsgrad = periodeInfo.utbetalingsgrader.finnFor(arbeidsforhold))
        }
        Utfall.IKKE_OPPFYLT -> {
            ForenkletUttaksperiode(periode = periode, oppfylt = false, utbetalingsgrad = Prosent.ZERO)
        }
    }
}

private fun List<Utbetalingsgrader>.finnFor(arbeidsforhold: Arbeidsforhold): Prosent {
    return find { it.arbeidsforhold == arbeidsforhold } ?.utbetalingsgrad ?: Prosent.ZERO
}