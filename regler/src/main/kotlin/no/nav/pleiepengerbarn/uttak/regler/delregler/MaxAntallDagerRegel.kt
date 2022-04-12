package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.EndringsstatusOppdaterer
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.UttaksplanMerger
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperDelvis
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

        val fullstendigUttaksplan = utledFullstendigUttaksplan(uttaksplan, grunnlag)

        val (forBrukteDagerHittil, maxDatoHittil) = grunnlag.finnForbrukteDagerHittil(fullstendigUttaksplan)

        var rest = BigDecimal(maxDager) - forBrukteDagerHittil

        val nyePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()

        fullstendigUttaksplan.perioder.forEach { (periode, info) ->
            if (info.utfall == Utfall.OPPFYLT && (info.endringsstatus == null || info.endringsstatus == Endringsstatus.NY)) {
                val forbrukteDagerDennePerioen = BigDecimal(periode.virkedager()) * (info.uttaksgrad / HUNDRE_PROSENT.setScale(2, RoundingMode.HALF_UP))

                if (rest <= BigDecimal.ZERO) {
                    // Hvis ingenting igjen på kvoten så må undersøke om det fremdeles kan innvilges
                    kanPeriodenInnvilgesFordiDenOverlapperMedTidligereInnvilgetPeriode(nyePerioder, periode, info, maxDatoHittil)
                } else if (forbrukteDagerDennePerioen <= rest) {
                    // Hvis det er nok dager igjen, så settes hele periode til oppfylt
                    nyePerioder[periode] = info
                    rest -= forbrukteDagerDennePerioen
                } else {
                    // Bare delvis nok dager igjen, så deler derfor opp perioden i en oppfylt og en ikke oppfylt periode
                    val restHeleDager = rest.setScale(0, RoundingMode.UP).toLong()
                    val restHeleDagerMedEventuellHelg = if (restHeleDager>5) ((restHeleDager/5L)*2L)+restHeleDager-2L else restHeleDager
                    nyePerioder[LukketPeriode(periode.fom, periode.fom.plusDays(restHeleDagerMedEventuellHelg - 1L))] = info
                    kanPeriodenInnvilgesFordiDenOverlapperMedTidligereInnvilgetPeriode(nyePerioder, LukketPeriode(periode.fom.plusDays(restHeleDagerMedEventuellHelg), periode.tom), info, maxDatoHittil)
                    rest = BigDecimal.ZERO
                }
            } else {
                // Gjør ingenting med perioder som ikke er oppfylt
                nyePerioder[periode] = info
            }
        }
        val kvoteInfo = KvoteInfo(
                maxDato = skalKunSetteMaxDatoHvisKvotenErbruktOpp(forBrukteDagerHittil, maxDatoHittil, BigDecimal(maxDager)),
                forbruktKvoteHittil = forBrukteDagerHittil,
                forbruktKvoteDenneBehandlingen = nyePerioder.finnForbrukteDager(brukKunPerioderFraForrigeUttaksplan = false).first)
        return uttaksplan.copy(perioder = nyePerioder, kvoteInfo = kvoteInfo)
    }

    private fun skalKunSetteMaxDatoHvisKvotenErbruktOpp(forBrukteDagerHittil: BigDecimal, maxDatoHittil: LocalDate?, maxDager: BigDecimal): LocalDate? {
        if (maxDatoHittil == null) {
            return null
        } else if (forBrukteDagerHittil == maxDager || forBrukteDagerHittil > maxDager ) {
            return maxDatoHittil
        }
        return null
    }

    private fun utledFullstendigUttaksplan(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag): Uttaksplan {
        return if (grunnlag.forrigeUttaksplan != null) {
            val fullstendigUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(grunnlag.forrigeUttaksplan, uttaksplan, grunnlag.trukketUttak)
            EndringsstatusOppdaterer.oppdater(grunnlag.forrigeUttaksplan, fullstendigUttaksplan)
        } else {
            uttaksplan
        }
    }

    private fun kanPeriodenInnvilgesFordiDenOverlapperMedTidligereInnvilgetPeriode(nyePerioder: MutableMap<LukketPeriode, UttaksperiodeInfo>, periode: LukketPeriode, info: UttaksperiodeInfo, maxDatoHittil: LocalDate?): Map<LukketPeriode, UttaksperiodeInfo> {
        if (maxDatoHittil!=null) {
            if (sjekkOmAltKanInnvilgesFordiDetErFørTidligereInnvilgetPeriode(periode, maxDatoHittil)) {
                nyePerioder[periode] = info
            } else if (sjekkOmNoeKanInnvilgesFordiDetOverlapperMedTidligereInnvilgetPeriode(periode, maxDatoHittil)) {
                nyePerioder[LukketPeriode(periode.fom, maxDatoHittil)] = info
                nyePerioder[LukketPeriode(maxDatoHittil.plusDays(1), periode.tom)] = info.settIkkeoppfylt()
            } else {
                nyePerioder[periode] = info.settIkkeoppfylt()
            }
        } else {
            nyePerioder[periode] = info.settIkkeoppfylt()
        }
        return nyePerioder
    }

    private fun sjekkOmNoeKanInnvilgesFordiDetOverlapperMedTidligereInnvilgetPeriode(periode: LukketPeriode, maxDatoHittil: LocalDate): Boolean {
        return (periode.fom == maxDatoHittil || periode.fom.isBefore(maxDatoHittil))
    }

    private fun sjekkOmAltKanInnvilgesFordiDetErFørTidligereInnvilgetPeriode(periode: LukketPeriode, maxDatoHittil: LocalDate): Boolean {
        return (periode.tom == maxDatoHittil || periode.tom.isBefore(maxDatoHittil))
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

private fun RegelGrunnlag.finnForbrukteDagerHittil(uttaksplan: Uttaksplan): Pair<BigDecimal, LocalDate?> {
    var antallDager = BigDecimal.ZERO
    val relevantePerioder = mutableListOf<LukketPeriode>()

    this.kravprioritetForBehandlinger.forEach { (kravprioritetsperiode, behandlingsUUIDer) ->
        for (behandlingUUID in behandlingsUUIDer) {
            if (behandlingUUID != this.behandlingUUID) {
                //Skal ikke telle med nåværende behandling
                val annenPartsUttaksplan = this.andrePartersUttaksplanPerBehandling[behandlingUUID]
                        ?: throw IllegalStateException("Skal ikke kunne skje at behandling ikke finnes")
                annenPartsUttaksplan.perioder.forEach { (annenPartsPeriode, info) ->
                    if (annenPartsPeriode.overlapperDelvis(kravprioritetsperiode)) {
                        if (info.utfall == Utfall.OPPFYLT) {
                            antallDager += (info.uttaksgrad.divide(HUNDRE_PROSENT.setScale(2, RoundingMode.HALF_UP)) * BigDecimal(annenPartsPeriode.virkedager()))
                            relevantePerioder.add(annenPartsPeriode)
                        }
                    }
                }
            }
        }
    }

    if (this.forrigeUttaksplan != null) {
        val (forBrukteDagerForrigeBehandling, relevantePerioderForrigeBehandling) = uttaksplan.perioder.finnForbrukteDager(brukKunPerioderFraForrigeUttaksplan = true)
        relevantePerioder.addAll(relevantePerioderForrigeBehandling)
        antallDager += forBrukteDagerForrigeBehandling
    }
    val maxDatoHittil = relevantePerioder.maxOfOrNull { it.tom }

    return Pair(antallDager, maxDatoHittil)
}

private fun Map<LukketPeriode, UttaksperiodeInfo>.finnForbrukteDager(brukKunPerioderFraForrigeUttaksplan: Boolean): Pair<BigDecimal, List<LukketPeriode>> {
    var antallDager = BigDecimal.ZERO
    var relevantePerioder = mutableListOf<LukketPeriode>()

    this.forEach { (annenPartsPeriode, info) ->
        if (info.utfall == Utfall.OPPFYLT) {
            if (info.erPeriodenFraForrigeUttaksplan(brukKunPerioderFraForrigeUttaksplan) || info.erPeriodenNyEllerEndret(brukKunPerioderFraForrigeUttaksplan)) {
                antallDager += (info.uttaksgrad.divide(HUNDRE_PROSENT.setScale(2, RoundingMode.HALF_UP)) * BigDecimal(annenPartsPeriode.virkedager()))
                relevantePerioder.add(annenPartsPeriode)
            }

        }
    }
    return Pair(antallDager, relevantePerioder)
}

private fun UttaksperiodeInfo.erPeriodenFraForrigeUttaksplan(brukKunPerioderFraForrigeUttaksplan: Boolean): Boolean {
    if (brukKunPerioderFraForrigeUttaksplan) {
        return (this.endringsstatus != null && this.endringsstatus == Endringsstatus.UENDRET)
    }
    return false
}

private fun UttaksperiodeInfo.erPeriodenNyEllerEndret(brukKunPerioderFraForrigeUttaksplan: Boolean): Boolean {
    if (!brukKunPerioderFraForrigeUttaksplan) {
        return ((this.endringsstatus == null) || (this.endringsstatus != null && (this.endringsstatus == Endringsstatus.NY || this.endringsstatus == Endringsstatus.ENDRET)))
    }
    return false
}