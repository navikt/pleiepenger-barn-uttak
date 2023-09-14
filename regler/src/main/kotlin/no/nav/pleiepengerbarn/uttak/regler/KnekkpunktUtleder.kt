package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.Knekkpunkt
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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

        finnForFerie(knekkpunkMap, regelGrunnlag.lovbestemtFerie)
        finnForIkkeOppfyltInngangsvilkår(knekkpunkMap, regelGrunnlag.inngangsvilkår)
        finnForPleiebehov(knekkpunkMap, regelGrunnlag.pleiebehov)
        finnForAnnenPartsUttaksplan(knekkpunkMap, regelGrunnlag.andrePartersUttaksplanPerBehandling)
        finnForAnnenPartsUttaksplan(knekkpunkMap, regelGrunnlag.vedtatteUttaksplanPerBehandling)
        if (regelGrunnlag.forrigeUttaksplan != null) {
            finnForForrigeUttaksplan(knekkpunkMap, regelGrunnlag.forrigeUttaksplan)
        }
        finnForTilsynsperiode(knekkpunkMap, regelGrunnlag.tilsynsperioder)
        finnForArbeid(knekkpunkMap, regelGrunnlag.arbeid)
        finnForBeredskap(knekkpunkMap, regelGrunnlag.beredskapsperioder.keys)
        finnForNattevåk(knekkpunkMap, regelGrunnlag.nattevåksperioder.keys)
        finnForKravprioritet(knekkpunkMap, regelGrunnlag.kravprioritetForBehandlinger.keys)
        finnForBarnsDød(knekkpunkMap, regelGrunnlag.barn)
        finnForSøkersDød(knekkpunkMap, regelGrunnlag.søker)
        finnForTrukketUttak(knekkpunkMap, regelGrunnlag.trukketUttak)
        finnForUtenlandsopphold(knekkpunkMap, regelGrunnlag.utenlandsoppholdperioder.keys)
        finnForStartSpesialhåndteringTilkommenAktivitet(knekkpunkMap, regelGrunnlag.nyeReglerUtbetalingsgrad)

        val knekkpunkter = mutableListOf<Knekkpunkt>()
        knekkpunkMap.forEach { (key, value) ->
            knekkpunkter.add(Knekkpunkt(key, value))
        }
        return knekkpunkter.toSortedSet(compareBy { it.knekk })
    }

    private fun finnForStartSpesialhåndteringTilkommenAktivitet(knekkpunkMap: MutableMap<LocalDate, MutableSet<KnekkpunktType>>, nyeReglerUtbetalingsgrad: LocalDate?) {
        if (nyeReglerUtbetalingsgrad != null) {
            oppdaterKnekkpunktMap(knekkpunkMap, nyeReglerUtbetalingsgrad, KnekkpunktType.START_SPESIALHÅNDTERING_TILKOMMEN)
        }
    }

    private fun finnForUtenlandsopphold(knekkpunkMap: MutableMap<LocalDate, MutableSet<KnekkpunktType>>, utenlandsopphold: Set<LukketPeriode>) {
        utenlandsopphold.forEach {finnForPeriode(knekkpunkMap, it, KnekkpunktType.UTENLANDSOPPHOLD)}
    }

    private fun finnForTrukketUttak(knekkpunkMap: MutableMap<LocalDate, MutableSet<KnekkpunktType>>, trukketUttak: List<LukketPeriode>) {
        trukketUttak.forEach {periode -> finnForPeriode(knekkpunkMap, periode, KnekkpunktType.TRUKKET_UTTAK)}
    }

    private fun finnForForrigeUttaksplan(knekkpunkMap: MutableMap<LocalDate, MutableSet<KnekkpunktType>>, forrigeUttaksplan: Uttaksplan) {
        forrigeUttaksplan.perioder.forEach { (periode ,_) ->
            finnForPeriode(knekkpunkMap, periode, KnekkpunktType.FORRIGE_UTTAKPLAN)
        }
    }

    private fun finnForBarnsDød(knekkpunktMap: KnekkpunktMap, barn: Barn) {
        if (barn.dødsdato != null) {
            oppdaterKnekkpunktMap(knekkpunktMap, barn.dødsdato!!.plusDays(1), KnekkpunktType.BARNETS_DØDSFALL)
            val antall = barn.rettVedDød?.lengde ?: 0
            val enhet = barn.rettVedDød?.enhet ?: ChronoUnit.DAYS
            oppdaterKnekkpunktMap(knekkpunktMap, barn.dødsdato!!.plusDays(1).plus(antall, enhet), KnekkpunktType.BARNETS_DØDSFALL)
        }
    }

    private fun finnForSøkersDød(knekkpunktMap: KnekkpunktMap, søker: Søker) {
        if (søker.dødsdato != null) {
            oppdaterKnekkpunktMap(knekkpunktMap, søker.dødsdato!!.plusDays(1), KnekkpunktType.SØKERS_DØDSFALL)
        }
    }

    private fun finnForKravprioritet(knekkpunktMap: KnekkpunktMap, kravprioritetsperioder: Set<LukketPeriode>) {
        kravprioritetsperioder.forEach { finnForPeriode(knekkpunktMap, it, KnekkpunktType.KRAVPRIORITETSPERIODE)}
    }

    private fun finnForNattevåk(knekkpunkMap: KnekkpunktMap, nattevåksperioder: Set<LukketPeriode>) {
        nattevåksperioder.forEach { finnForPeriode(knekkpunkMap, it, KnekkpunktType.NATTEVÅKSPERIODE)}
    }

    private fun finnForBeredskap(knekkpunkMap: KnekkpunktMap, beredskapsperioder: Set<LukketPeriode>) {
        beredskapsperioder.forEach { finnForPeriode(knekkpunkMap, it, KnekkpunktType.BEREDSKAPSPERIODE)}
    }

    private fun finnForTilsynsperiode(knekkpunkMap: KnekkpunktMap, tilsyn: Map<LukketPeriode, Duration>) {
        tilsyn.entries.forEach { finnForPeriode(knekkpunkMap, it.key, KnekkpunktType.TILSYNSPERIODE) }
    }

    private fun finnForFerie(knekkpunktMap: KnekkpunktMap, ferier:List<LukketPeriode>) {
        ferier.forEach {finnForPeriode(knekkpunktMap, it, KnekkpunktType.LOVBESTEMT_FERIE)}
    }

    private fun finnForIkkeOppfyltInngangsvilkår(knekkpunktMap: KnekkpunktMap, inngangsvilkår: Map<String, List<Vilkårsperiode>>) {
        inngangsvilkår.values.forEach { perioder ->
            perioder.forEach {
                if (it.utfall == Utfall.IKKE_OPPFYLT) {
                    finnForPeriode(knekkpunktMap, it.periode, KnekkpunktType.INNGANGSVILKÅR_IKKE_OPPFYLT)
                }
            }
        }
    }

    private fun finnForPleiebehov(knekkpunktMap:KnekkpunktMap, pleiebehov: Map<LukketPeriode, Pleiebehov>) {
        pleiebehov.entries.forEach {finnForPeriode(knekkpunktMap, it.key, KnekkpunktType.PLEIEBEHOV)}
    }

    private fun finnForAnnenPartsUttaksplan(knekkpunktMap:KnekkpunktMap, andrePartersUttaksplan:Map<UUID, Uttaksplan>) {
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
