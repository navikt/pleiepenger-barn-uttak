package no.nav.pleiepengerbarn.uttak.regler.tidslinje

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent

internal data class Tidslinje (
        internal val beskrivelse: String,
        internal val perioder: Map<LukketPeriode, Prosent?>
)
