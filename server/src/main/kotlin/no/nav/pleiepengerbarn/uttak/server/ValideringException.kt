package no.nav.pleiepengerbarn.uttak.server

import java.lang.RuntimeException

class ValideringException(feilmelding: String) : RuntimeException(feilmelding)