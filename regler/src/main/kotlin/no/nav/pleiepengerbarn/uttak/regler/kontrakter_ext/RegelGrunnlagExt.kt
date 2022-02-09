package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import java.util.*

fun RegelGrunnlag.annenPart(periode: LukketPeriode): AnnenPart {
    if (this.andrePartersUttaksplanPerBehandling.overlapper(periode)) {
        return AnnenPart.MED_ANDRE
    }
    return AnnenPart.ALENE
}

private fun Map<UUID, Uttaksplan>.overlapper(periode: LukketPeriode) =
    this.values.any { it.overlapperMedUttaksplan(periode) }

private fun Uttaksplan.overlapperMedUttaksplan(periode: LukketPeriode) =
    this.perioder.any { it.key.overlapperHelt(periode) && it.value.utfall == Utfall.OPPFYLT }
