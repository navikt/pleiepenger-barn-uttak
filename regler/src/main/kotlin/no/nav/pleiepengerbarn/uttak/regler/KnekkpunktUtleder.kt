package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import java.time.LocalDate
import java.util.*

internal typealias KnekkpunktMap = MutableMap<LocalDate, MutableSet<KnekkpunktType>>

internal object KnekkpunktUtleder {

    /**
     * Finner alle aktuelle knekkpunkter for uttak.
     *
     * @param regelGrunnlag alle grunnlagsdata som skal brukes for uttak
     * @return sortert set med knekkpunkter
     */
    internal fun finnKnekkpunkter(regelGrunnlag: RegelGrunnlag): SortedSet<Knekkpunkt> {

        val knekkpunkMap = mutableMapOf<LocalDate, MutableSet<KnekkpunktType>>()

        finnForIkkeMedlem(knekkpunkMap, regelGrunnlag.ikkeMedlem)
        finnForFerie(knekkpunkMap, regelGrunnlag.lovbestemtFerie)
        finnForTilsynsbehov(knekkpunkMap, regelGrunnlag.tilsynsbehov)
        finnForAnnenPartsUttaksplan(knekkpunkMap, regelGrunnlag.andrePartersUttaksplan)
        finnForTilsynsperiode(knekkpunkMap, regelGrunnlag.tilsynsperioder)
        finnForArbeid(knekkpunkMap, regelGrunnlag.arbeid)

        val knekkpunkter = mutableListOf<Knekkpunkt>()
        knekkpunkMap.forEach { (key, value) ->
            knekkpunkter.add(Knekkpunkt(key, value))
        }
        return knekkpunkter.toSortedSet(compareBy { it.knekk })
    }

    private fun finnForIkkeMedlem(knekkpunkMap: MutableMap<LocalDate, MutableSet<KnekkpunktType>>, ikkeMedlem: List<LukketPeriode>) {
        ikkeMedlem.forEach { finnForPeriode(knekkpunkMap, it, KnekkpunktType.IKKE_MEDLEM_I_FOLKETRYGDEN) }
    }

    private fun finnForTilsynsperiode(knekkpunkMap: MutableMap<LocalDate, MutableSet<KnekkpunktType>>, tilsyn: Map<LukketPeriode, TilsynPeriodeInfo>) {
        tilsyn.entries.forEach { finnForPeriode(knekkpunkMap, it.key, KnekkpunktType.TILSYNSPERIODE) }
    }

    private fun finnForFerie(knekkpunktMap:KnekkpunktMap, ferier:List<LukketPeriode>) {
        ferier.forEach {finnForPeriode(knekkpunktMap, it, KnekkpunktType.LOVBESTEMT_FERIE)}
    }

    private fun finnForTilsynsbehov(knekkpunktMap:KnekkpunktMap, tilsynsbehov: Map<LukketPeriode, Tilsynsbehov>) {
        tilsynsbehov.entries.forEach {finnForPeriode(knekkpunktMap, it.key, KnekkpunktType.TILSYNSBEHOV)}
    }

    private fun finnForAnnenPartsUttaksplan(knekkpunktMap:KnekkpunktMap, andrePartersUttaksplan:Map<Saksnummer, Uttaksplan>) {
        andrePartersUttaksplan.forEach { (_, uttaksplan) ->
            uttaksplan.perioder.forEach {finnForPeriode(knekkpunktMap, it.key, KnekkpunktType.ANNEN_PARTS_UTTAK)}
        }
    }

    private fun finnForArbeid(knekkpunktMap:KnekkpunktMap, arbeid: List<Arbeid>) {
        arbeid.forEach {
            it.perioder.forEach {(periode,_) -> finnForPeriode(knekkpunktMap, periode, KnekkpunktType.ARBEID)}
        }
    }

    private fun finnForPeriode(knekkpunktMap:KnekkpunktMap, periode:LukketPeriode, knekkpunktType: KnekkpunktType) {
        oppdaterKnekkpunktMap(knekkpunktMap, periode.fom, knekkpunktType)
        oppdaterKnekkpunktMap(knekkpunktMap, periode.tom.plusDays(1), knekkpunktType)
    }

    private fun oppdaterKnekkpunktMap(knekkpunktMap:KnekkpunktMap, knekkpunkt:LocalDate, knekkpunktType: KnekkpunktType) {
        if (knekkpunktMap.containsKey(knekkpunkt)) {
            knekkpunktMap[knekkpunkt]?.add(knekkpunktType)
        } else {
            knekkpunktMap[knekkpunkt]=mutableSetOf(knekkpunktType)
        }
    }

}