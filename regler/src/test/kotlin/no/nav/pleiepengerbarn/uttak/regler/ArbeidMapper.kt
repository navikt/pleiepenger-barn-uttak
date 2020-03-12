package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*

internal fun Map<String, Map<LukketPeriode, ArbeidsforholdPeriodeInfo>>.somArbeid()
        = map { (ref, perioder ) ->
    Arbeidsforhold(
            arbeidsforhold = ArbeidsforholdReferanse(
                    arbeidsforholdId = ref
            ),
            perioder = perioder
    )
}

internal fun Map<String, Prosent>.somUtbetalingsgrader() = map { (ref,utbetalingsgrad) ->
    Utbetalingsgrader(
            arbeidsforhold = ArbeidsforholdReferanse(
                    arbeidsforholdId = ref
            ),
            utbetalingsgrad = utbetalingsgrad
    )
}