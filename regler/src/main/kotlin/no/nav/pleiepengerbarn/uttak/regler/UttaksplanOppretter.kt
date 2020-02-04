package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*


object UttaksplanOppretter {

    fun opprettUttaksperioder(søknad: Søknad): Uttaksplan {
        val perioder = mutableListOf<Uttaksperiode>()
        for (p in søknad.oppgittePerioder.søktePerioder) {
            perioder.add(Uttaksperiode(fom = p.fom, tom =  p.tom))
        }
        return Uttaksplan(søknad.saksnummer, søknad.behandlingId, perioder)
    }


}
