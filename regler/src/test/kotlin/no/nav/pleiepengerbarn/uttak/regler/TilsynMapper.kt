package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.regler.domene.Desimaltall
import no.nav.pleiepengerbarn.uttak.regler.domene.somDesimaltall
import java.time.Duration

internal fun Map<LukketPeriode, Prosent>.somTilsynperioder() = mapValues { (_, prosent) ->
    val lengdePåTilsynsPeriode = Desimaltall
            .fraDuration(Duration.ofHours(7).plusMinutes(30))
            .times(prosent.somDesimaltall().fraProsentTilFaktor().normaliserFaktor())
            .tilDuration()

    lengdePåTilsynsPeriode
}

