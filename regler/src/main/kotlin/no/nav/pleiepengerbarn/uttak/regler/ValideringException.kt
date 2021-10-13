package no.nav.pleiepengerbarn.uttak.regler

import java.lang.RuntimeException

class ValideringException(feilmelding: String) : RuntimeException(feilmelding)