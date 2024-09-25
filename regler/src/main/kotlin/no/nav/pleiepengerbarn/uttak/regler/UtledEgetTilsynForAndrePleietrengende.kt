package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperHelt
import java.math.BigDecimal

internal fun RegelGrunnlag.finnEgetTilsynForAndrePleietrengende(
    periode: LukketPeriode
): Prosent {
    return finnEgetTilsynFraUttaksperioder(periode);
}


private fun RegelGrunnlag.finnEgetTilsynFraUttaksperioder(periode: LukketPeriode): BigDecimal {
    if (this.barn.dødsdato != null && this.barn.dødsdato!! <= periode.fom) {
        return Prosent.ZERO
    }

    val uttaksplanerMedKrav = this.egneUttaksplaner(periode)
    val andreSøkeresUttaksplanerMedTidligereVedtak = this.egneUttaksplanerAndrePleietrengendeMedTidligereVedtak(periode)
    val andreVedtak = andreSøkeresUttaksplanerMedTidligereVedtak
        .filter { uttaksplan ->
            uttaksplan.uttaksplan.perioder.filter { (pp, _) -> pp.overlapperHelt(periode) }.any { (_, periodeinfo) ->
                uttaksplanerMedKrav.none { plan ->
                    plan.uttaksplan.perioder.filter { (pp, _) -> pp.overlapperHelt(periode) }.any { (_, info) ->
                        info.kildeBehandlingUUID == periodeinfo.kildeBehandlingUUID
                    }
                }
            }
        }

    val sumAndreSøkeresTilsyn = finnTilsynForUttaksPeriodeFraUttaksplaner(
        periode = periode,
        uttaksplaner = andreVedtak,
    )

    return finnTilsynForUttaksPeriodeFraUttaksplaner(
        periode = periode,
        uttaksplaner = uttaksplanerMedKrav,
        sumAndreSøkeresTilsyn
    )
}

private fun finnTilsynForUttaksPeriodeFraUttaksplaner(
    periode: LukketPeriode,
    uttaksplaner: List<UttaksplanMedBehandlingUuid>,
    alleredeForbrukt: BigDecimal = Prosent.ZERO
): BigDecimal {
    var andreSøkeresTilsynsgrad = alleredeForbrukt
    uttaksplaner.forEach { uttaksplan ->
        val overlappendePeriode = uttaksplan.uttaksplan.perioder.keys.firstOrNull { it.overlapperHelt(periode) }
        if (overlappendePeriode != null) {
            val uttaksperiode = uttaksplan.uttaksplan.perioder[overlappendePeriode]
            if (uttaksperiode != null && uttaksperiode.utfall == Utfall.OPPFYLT) {
                andreSøkeresTilsynsgrad +=
                    uttaksperiode.uttaksgrad
            }
        }
    }
    return andreSøkeresTilsynsgrad
}

private fun RegelGrunnlag.egneUttaksplaner(periode: LukketPeriode): List<UttaksplanMedBehandlingUuid> {
    val kravprioritetPeriode = kravprioritetForEgneBehandlinger.keys.firstOrNull { it.overlapperHelt(periode) }
        ?: return listOf()

    val kravprioritetListe = kravprioritetForEgneBehandlinger[kravprioritetPeriode] ?: return listOf()

    val uttaksplanerMedKrav = mutableListOf<UttaksplanMedBehandlingUuid>()
    for (behandlingMedKrav in kravprioritetListe) {
        if (behandlingMedKrav == this.behandlingUUID) {
            break
        }
        val uttaksplanMedKrav = egneUttaksplanerAllePleietrengendePerBehandling[behandlingMedKrav]
        if (uttaksplanMedKrav != null) {
            uttaksplanerMedKrav.add(UttaksplanMedBehandlingUuid(uttaksplanMedKrav, behandlingMedKrav))
        }
    }
    return uttaksplanerMedKrav
}

private fun RegelGrunnlag.egneUttaksplanerAndrePleietrengendeMedTidligereVedtak(periode: LukketPeriode): List<UttaksplanMedBehandlingUuid> {
    val kravprioritetPeriode = kravprioritetForEgneBehandlinger.keys.firstOrNull { it.overlapperHelt(periode) }
        ?: return listOf()

    val kravprioritetListe = kravprioritetForEgneBehandlinger[kravprioritetPeriode] ?: return listOf()

    val uttaksplanerMedKrav = mutableListOf<UttaksplanMedBehandlingUuid>()
    var bakEgenBehandling = false;
    for (behandlingMedKrav in kravprioritetListe) {
        if (behandlingMedKrav == this.behandlingUUID) {
            bakEgenBehandling = true
        }
        if (bakEgenBehandling && behandlingMedKrav != this.behandlingUUID) {
            val forrigeUttaksplan = sisteVedtatteUttaksplanForBehandling[behandlingMedKrav] ?: continue
            val uttaksplan = vedtatteUttaksplanPerBehandling[forrigeUttaksplan]
            if (uttaksplan != null && uttaksplan.perioder.any { (periode, _) -> periode.overlapperHelt(periode) }) {
                uttaksplanerMedKrav.add(UttaksplanMedBehandlingUuid(uttaksplan, behandlingMedKrav))
            }
        }
    }
    return uttaksplanerMedKrav
}
