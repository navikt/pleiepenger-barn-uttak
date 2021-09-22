package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import java.time.Duration

data class Utbetalingsgrad(
    val utbetalingsgrad: Prosent,
    val normalArbeidstid: Duration,
    val faktiskArbeidstid: Duration?
)