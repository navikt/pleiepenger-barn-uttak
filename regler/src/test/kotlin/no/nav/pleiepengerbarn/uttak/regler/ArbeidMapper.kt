package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.time.Duration

internal fun Map<String, Map<LukketPeriode, ArbeidsforholdPeriodeInfo>>.somArbeid()
        = map { (ref, perioder ) ->
    Arbeid(
            arbeidsforhold = Arbeidsforhold(
                    type = "arbeidsforhold",
                    arbeidsforholdId = ref
            ),
            perioder = perioder
    )
}

internal fun Map<String, Prosent>.somUtbetalingsgrader() = map { (ref,utbetalingsgrad) ->
    Utbetalingsgrader(
            arbeidsforhold = Arbeidsforhold(
                    type = "arbeidsforhold",
                    arbeidsforholdId = ref
            ),
            utbetalingsgrad = utbetalingsgrad,
            normalArbeidstid = Duration.ZERO, //NB: Brukes ikke i sammenligning i test
            faktiskArbeidstid = Duration.ZERO //NB: Brukes ikke i sammenligning i test
    )
}