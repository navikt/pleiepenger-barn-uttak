package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.kontrakter.TilsynPeriodeInfo
import no.nav.pleiepengerbarn.uttak.regler.domene.Desimaltall
import no.nav.pleiepengerbarn.uttak.regler.domene.div
import no.nav.pleiepengerbarn.uttak.regler.domene.somDesimaltall
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.antallVirketimer

internal fun Map<LukketPeriode, Prosent>.somTilsynperioder() = mapValues { (periode,prosent) ->
        val virketimerIPerioden = periode.antallVirketimer()

        val lengdePåTilsynsPeriode = Desimaltall
                .fraDuration(virketimerIPerioden)
                .times(prosent.somDesimaltall().fraProsentTilFaktor().normaliserFaktor())
                .tilDuration()

        TilsynPeriodeInfo(
                lengde = lengdePåTilsynsPeriode
        )
    }

internal fun Map.Entry<LukketPeriode, TilsynPeriodeInfo>.somProsent() : Prosent {
    val virketimerIPerioden = key.antallVirketimer()
    return value.lengde.div(virketimerIPerioden).fraFaktorTilProsent().resultat

}