package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.BehandlingId
import no.nav.pleiepengerbarn.uttak.kontrakter.Søknad
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan

object UttakTjeneste {

    fun uttaksplan(søknad: Søknad, andrePartersBehandlig:List<BehandlingId> = listOf()): Uttaksplan {
        val uttaksplan = UttaksplanOppretter.opprettUttaksperioder(søknad)
        val knekkpunkt = KnekkpunktOppretter.finnKnekkpunkter(søknad.oppgittePerioder)
        //TODO bruk knekkpunkt på uttaksplan
        val avklartUttaksplan = UttaksplanRegler.kjørRegler(uttaksplan)
        return avklartUttaksplan
    }

}