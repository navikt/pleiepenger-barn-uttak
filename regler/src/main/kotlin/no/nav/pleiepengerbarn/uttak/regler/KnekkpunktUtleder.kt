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
        finnForFerie(knekkpunkMap, regelGrunnlag.ferier)
        finnForTilsynsbehov(knekkpunkMap, regelGrunnlag.tilsynsbehov)
        finnForAnnenPartsUttaksplan(knekkpunkMap, regelGrunnlag.andrePartersUttaksplan)
        finnForTilsynsperiode(knekkpunkMap, regelGrunnlag.tilsynsperioder)
        finnForArbeid(knekkpunkMap, regelGrunnlag.arbeidsforhold)

        val knekkpunkter = mutableListOf<Knekkpunkt>()
        knekkpunkMap.forEach { (key, value) ->
            knekkpunkter.add(Knekkpunkt(key, value))
        }
        return knekkpunkter.toSortedSet(compareBy { it.knekk })
    }

    private fun finnForIkkeMedlem(knekkpunkMap: MutableMap<LocalDate, MutableSet<KnekkpunktType>>, ikkeMedlem: List<LukketPeriode>) {
        ikkeMedlem.forEach { finnForPeriode(knekkpunkMap, it, KnekkpunktType.IKKE_MEDLEM) }
    }

    private fun finnForTilsynsperiode(knekkpunkMap: MutableMap<LocalDate, MutableSet<KnekkpunktType>>, tilsynPerioder: List<Tilsyn>) {
        tilsynPerioder.forEach { finnForPeriode(knekkpunkMap, it.periode, KnekkpunktType.TILSYNSPERIODE) }
    }

    private fun finnForFerie(knekkpunktMap:KnekkpunktMap, ferier:List<LukketPeriode>) {
        ferier.forEach {finnForPeriode(knekkpunktMap, it, KnekkpunktType.FERIE)}
    }

    private fun finnForTilsynsbehov(knekkpunktMap:KnekkpunktMap, tilsynsbehovListe: List<Tilsynsbehov>) {
        tilsynsbehovListe.forEach {finnForPeriode(knekkpunktMap, it.periode, KnekkpunktType.TILSYNSBEHOV)}
    }

    private fun finnForAnnenPartsUttaksplan(knekkpunktMap:KnekkpunktMap, andrePartersUttaksplan:List<Uttaksplan>) {
        andrePartersUttaksplan.forEach { uttaksplan ->
            uttaksplan.perioder.forEach {finnForPeriode(knekkpunktMap, it.key, KnekkpunktType.ANNEN_PARTS_UTTAK)}
        }
    }

    private fun finnForArbeid(knekkpunktMap:KnekkpunktMap, arbeidsforholdList: List<Arbeidsforhold>) {
        arbeidsforholdList.forEach { arbeidsforhold ->
            arbeidsforhold.perioder.keys.forEach { periode -> finnForPeriode(knekkpunktMap, periode, KnekkpunktType.ARBEID) }
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