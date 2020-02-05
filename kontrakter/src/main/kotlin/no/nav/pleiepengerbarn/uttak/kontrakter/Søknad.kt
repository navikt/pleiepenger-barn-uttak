package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.LocalDate

data class Søknad(
        val saksnummer:SakId,
        val behandlingId: BehandlingId,
        val mottaksdato: LocalDate,
        val oppgittePerioder: OppgittePerioder
)