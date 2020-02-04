package no.nav.pleiepengerbarn.uttak.regler.tidslinje

import no.nav.pleiepengerbarn.uttak.kontrakter.Grad
import no.nav.pleiepengerbarn.uttak.kontrakter.Periode

internal data class Tidslinje (
        internal val beskrivelse: String,
        internal val perioder: Map<Periode, Grad?>
)
