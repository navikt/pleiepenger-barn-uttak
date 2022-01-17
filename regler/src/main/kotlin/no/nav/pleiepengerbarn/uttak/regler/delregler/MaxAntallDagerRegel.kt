package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperDelvis
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.tilVirkedager
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.virkedager
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

internal class MaxAntallDagerRegel : UttaksplanRegel {

    companion object {
        val KVOTER = mapOf(
            YtelseType.PLS to 60
        )
    }

    override fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag): Uttaksplan {

        if (grunnlag.ytelseType != YtelseType.PLS) {
            return uttaksplan
        }
        val maxDager = KVOTER[grunnlag.ytelseType] ?: throw IllegalArgumentException("Ulovlig ytelsestype ${grunnlag.ytelseType}")
        val forBrukteDagerAndreParter = grunnlag.finnForbrukteDagerAndreParter()
        var rest = BigDecimal(maxDager) - forBrukteDagerAndreParter

        val nyePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()

        uttaksplan.perioder.forEach { (periode, info) ->
            if (info.utfall == Utfall.OPPFYLT) {
                val forbrukteDagerDennePerioen = BigDecimal(periode.virkedager()) * (info.uttaksgrad / HUNDRE_PROSENT.setScale(2, RoundingMode.HALF_UP))

                if (rest <= BigDecimal.ZERO) {
                    // Hvis ingenting igjen på kvoten så settes hele perioder til ikke oppfylt
                    nyePerioder[periode] = info.settIkkeoppfylt()
                } else if (forbrukteDagerDennePerioen <= rest) {
                    // Hvis det er nok dager igjen, så settes hele periode til oppfylt
                    nyePerioder[periode] = info
                    rest -= forbrukteDagerDennePerioen
                } else {
                    // Bare delvis nok dager igjen, så deler derfor opp perioden i en oppfylt og en ikke oppfylt periode
                    val restHeleDager = rest.setScale(0, RoundingMode.UP).toLong()
                    nyePerioder[LukketPeriode(periode.fom, periode.fom.plusDays(restHeleDager - 1L))] = info
                    nyePerioder[LukketPeriode(periode.fom.plusDays(restHeleDager), periode.tom)] = info.settIkkeoppfylt()
                    rest = BigDecimal.ZERO
                }
            } else {
                // Gjør ingenting med perioder som ikke er oppfylt
                nyePerioder[periode] = info
            }
        }

        return uttaksplan.copy(perioder = nyePerioder)
    }

}


private fun UttaksperiodeInfo.settIkkeoppfylt(): UttaksperiodeInfo {
    return this.copy(
        årsaker = setOf(Årsak.MAKS_DAGER_OVERSTEGET),
        utfall = Utfall.IKKE_OPPFYLT,
        inngangsvilkår = inngangsvilkår,
        uttaksgrad = Prosent(0).setScale(2, RoundingMode.HALF_UP),
        utbetalingsgrader = this.utbetalingsgrader.map {it.copy(utbetalingsgrad = Prosent(0).setScale(2, RoundingMode.HALF_UP))}
    )
}

private fun RegelGrunnlag.finnForbrukteDagerAndreParter(): BigDecimal {
    var antallDager = BigDecimal.ZERO
    this.kravprioritetForBehandlinger.forEach { (kravprioritetsperiode, behandlingsUUIDer) ->
        for (behandlingUUID in behandlingsUUIDer) {
            if (behandlingUUID == this.behandlingUUID) {
                //Avslutt loop dersom nåværende behandling
                break
            }
            val annenPartsUttaksplan = this.andrePartersUttaksplanPerBehandling[behandlingUUID] ?: throw IllegalStateException("Skal ikke kunne skje at behandling ikke finnes")
            annenPartsUttaksplan.perioder.forEach { (annenPartsPeriode, info) ->
                if (annenPartsPeriode.overlapperDelvis(kravprioritetsperiode)) {
                    if (info.utfall == Utfall.OPPFYLT) {
                        antallDager += (info.uttaksgrad / HUNDRE_PROSENT.setScale(2, RoundingMode.HALF_UP))
                    }
                }
            }
        }
    }
    return antallDager
}


private fun RegelGrunnlag.finnForbrukteDagerFraForrigeUttaksplan(): Map<LocalDate, BigDecimal> {
    if (this.forrigeUttaksplan == null) {
        return mapOf()
    }
    val forbrukteDagerPerDato = mutableMapOf<LocalDate, BigDecimal>()
    this.forrigeUttaksplan.perioder
        .filter {it.value.utfall == Utfall.OPPFYLT}
        .forEach {
            it.key.tilVirkedager().forEach { virkedag ->
                forbrukteDagerPerDato[virkedag] = it.value.uttaksgrad.divide(HUNDRE_PROSENT).setScale(2, RoundingMode.HALF_UP)
            }
        }
    return forbrukteDagerPerDato
}