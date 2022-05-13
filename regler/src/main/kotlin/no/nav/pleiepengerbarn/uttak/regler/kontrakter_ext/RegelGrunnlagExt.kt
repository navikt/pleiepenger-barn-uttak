package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.AnnenPart
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import java.util.*

fun RegelGrunnlag.annenPart(periode: LukketPeriode): AnnenPart {
    if (this.andrePartersUttaksplanPerBehandling.filter { this.behandlingUUID != it.key }.overlapper(periode)) {
        return AnnenPart.MED_ANDRE
    }
    return AnnenPart.ALENE
}

private fun Map<UUID, Uttaksplan>.overlapper(periode: LukketPeriode) =
    this.values.any { it.overlapperMedUttaksplan(periode) }

private fun Uttaksplan.overlapperMedUttaksplan(periode: LukketPeriode) =
    this.perioder.any { it.key.overlapperHelt(periode) && it.value.utfall == Utfall.OPPFYLT }
