package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.time.Duration

data class GraderBeregnet(
    val pleiebehov: Pleiebehov,
    val graderingMotTilsyn: GraderingMotTilsyn,
    val egetTilsynAndrePleietrengende: Prosent,
    val søkersTapteArbeidstid: Prosent,
    val oppgittTilsyn: Duration?,
    val uttaksgrad: Prosent,
    val uttaksgradUtenReduksjonGrunnetInntektsgradering: Prosent,
    val uttaksgradMedReduksjonGrunnetInntektsgradering: Prosent?,
    val utbetalingsgrader: Map<Arbeidsforhold, Utbetalingsgrad>,
    val årsak: Årsak,
    val manueltOverstyrt: Boolean = false
)
