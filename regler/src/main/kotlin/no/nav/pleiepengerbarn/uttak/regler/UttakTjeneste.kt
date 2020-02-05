package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvklarteFakta
import no.nav.pleiepengerbarn.uttak.kontrakter.BehandlingId
import no.nav.pleiepengerbarn.uttak.kontrakter.Søknad
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan

object UttakTjeneste {

    fun uttaksplan(avklarteFakta: AvklarteFakta): Uttaksplan {
        val uttaksplan = UttaksplanOppretter.opprettUttaksperioder(avklarteFakta)
        val knekkpunkter = KnekkpunktOppretter.finnKnekkpunkter(avklarteFakta)
        //TODO bruk knekkpunkt på uttaksplan
        val avklartUttaksplan = UttaksplanRegler.kjørRegler(uttaksplan)
        return avklartUttaksplan
    }

}