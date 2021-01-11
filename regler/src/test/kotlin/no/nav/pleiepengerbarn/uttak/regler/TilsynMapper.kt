package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.kontrakter.TilsynPeriodeInfo
import no.nav.pleiepengerbarn.uttak.regler.domene.Desimaltall
import no.nav.pleiepengerbarn.uttak.regler.domene.div
import no.nav.pleiepengerbarn.uttak.regler.domene.somDesimaltall
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.antallVirketimer
import java.time.Duration

internal fun Map<LukketPeriode, Prosent>.somTilsynperioder() = mapValues { (_, prosent) ->

        val lengdePåTilsynsPeriode = Desimaltall
                .fraDuration(Duration.ofHours(7).plusMinutes(30))
                .times(prosent.somDesimaltall().fraProsentTilFaktor().normaliserFaktor())
                .tilDuration()

        lengdePåTilsynsPeriode
    }

internal fun Map.Entry<LukketPeriode, TilsynPeriodeInfo>.somProsent() : Prosent {
    val virketimerIPerioden = key.antallVirketimer()
    return value.lengde.div(virketimerIPerioden).fraFaktorTilProsent().resultat

}