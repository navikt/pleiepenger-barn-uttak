package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag

fun RegelGrunnlag.annenPart(periode: LukketPeriode): AnnenPart {
    if (this.andrePartersUttaksplan.overlapper(periode)) {
        return AnnenPart.MED_ANDRE
    }
    if (this.søktUttak.overlapperMedVenterAndreParter(periode)) {
        return AnnenPart.VENTER_ANDRE
    }
    return AnnenPart.ALENE
}

private fun List<SøktUttak>.overlapperMedVenterAndreParter(periode: LukketPeriode): Boolean {
    return this.any { it.periode.overlapper(periode) && it.venterAnnenPart }
}

private fun Map<Saksnummer, Uttaksplan>.overlapper(periode: LukketPeriode): Boolean {
    this.values.forEach { uttaksplan ->
        uttaksplan.perioder.forEach { uttaksperiode ->
            if (uttaksperiode.key.overlapper(periode)) {
                if (uttaksperiode.value.utfall == Utfall.OPPFYLT) {
                    return true
                }
            }
        }
    }
    return false
}