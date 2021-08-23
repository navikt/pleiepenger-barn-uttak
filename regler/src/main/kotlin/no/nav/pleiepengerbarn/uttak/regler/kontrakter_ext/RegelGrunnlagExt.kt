package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag


fun RegelGrunnlag.annenPart(periode: LukketPeriode): AnnenPart {
    if (this.andrePartersUttaksplan.overlapperHelt(periode)) {
        return AnnenPart.MED_ANDRE
    }
    return AnnenPart.ALENE
}

private fun Map<Saksnummer, Uttaksplan>.overlapperHelt(periode: LukketPeriode): Boolean {
    this.values.forEach { uttaksplan ->
        uttaksplan.perioder.forEach { uttaksperiode ->
            if (uttaksperiode.key.overlapperHelt(periode)) {
                if (uttaksperiode.value.utfall == Utfall.OPPFYLT) {
                    return true
                }
            }
        }
    }
    return false
}