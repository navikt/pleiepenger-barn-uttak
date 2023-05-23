package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Pleiebehov
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperHelt
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.util.*

internal fun RegelGrunnlag.finnAndreSøkeresTilsyn(periode: LukketPeriode): Pair<Boolean, Prosent> {
    val søkersEtablertTilsyn = finnEtablertTilsyn(periode)
    val søkersNattevåk = finnNattevåk(periode)
    val søkersBeredskap = finnBeredskap(periode)
    val søkersPleiebehov = finnPleiebehov(periode)


    val etablertTilsynEndret = andrePartersUttaksplanPerBehandling.endret(periode) { uttaksperiodeInfo ->
        søkersEtablertTilsyn.prosentAvFullDag()
            .compareTo(uttaksperiodeInfo.graderingMotTilsyn?.etablertTilsyn ?: Prosent.ZERO) != 0
    }
    val nattevåkEndret =
        andrePartersUttaksplanPerBehandling.endret(periode) { uttaksperiodeInfo -> uttaksperiodeInfo.nattevåk != søkersNattevåk }
    val beredskapEndret =
        andrePartersUttaksplanPerBehandling.endret(periode) { uttaksperiodeInfo -> uttaksperiodeInfo.beredskap != søkersBeredskap }
    val pleiebehovEndret = andrePartersUttaksplanPerBehandling.endret(periode) { uttaksperiodeInfo ->
        uttaksperiodeInfo.pleiebehov.compareTo(søkersPleiebehov.prosent) != 0
    }

    val måReberegneAndrePartersTilsyn =
        true in listOf(etablertTilsynEndret, nattevåkEndret, beredskapEndret, pleiebehovEndret)

    val andrePartersTilsyn = if (måReberegneAndrePartersTilsyn) {
        reberegnAndreSøkeresTilsynKravprioritetBehandling(
            periode,
            søkersPleiebehov,
            søkersEtablertTilsyn,
            søkersNattevåk,
            søkersBeredskap
        )
    } else {
        finnAndreSøkeresTilsynFraUttaksperioder(periode)
    }
    return Pair(måReberegneAndrePartersTilsyn, andrePartersTilsyn)
}

private fun Map<UUID, Uttaksplan>.endret(
    periode: LukketPeriode,
    sjekkSøkersVerdi: (UttaksperiodeInfo) -> Boolean
): Boolean {
    return this.any { (_, uttaksplan) ->
        val periodeInfo = uttaksplan.finnOverlappendeUttaksperiode(periode)
        periodeInfo != null && sjekkSøkersVerdi(periodeInfo)
    }
}

private fun Uttaksplan.finnOverlappendeUttaksperiode(periode: LukketPeriode): UttaksperiodeInfo? {
    val uttaksperiode = perioder.keys.firstOrNull { it.overlapperHelt(periode) } ?: return null
    return perioder[uttaksperiode]
}

private fun RegelGrunnlag.reberegnAndreSøkeresTilsynKravprioritetBehandling(
    periode: LukketPeriode,
    pleiebehov: Pleiebehov,
    etablertTilsyn: Duration,
    nattevåkUtfall: Utfall?,
    beredskapUtfall: Utfall?
): Prosent {
    if (this.barn.dødsdato != null && this.barn.dødsdato!! <= periode.fom) {
        return Prosent.ZERO
    }

    val uttaksplanerMedKrav = if (this.sisteVedtatteUttaksplanForBehandling.isNotEmpty()) {
        this.alleSøkeresVedtatteUttaksplaner(periode)
    } else {
        this.andreSøkeresUttaksplaner(periode)
    }

    var sumAndreSøkeresTilsyn = Prosent.ZERO

    var oppdatertGrad = Prosent.ZERO
    for (uttaksplanMedUUIDMedKrav in uttaksplanerMedKrav) {
        val uttaksplanMedKrav = uttaksplanMedUUIDMedKrav.uttaksplan
        val annenPartsOverlappendePeriodeInfo = uttaksplanMedKrav.finnOverlappendeUttaksperiode(periode)
        if (annenPartsOverlappendePeriodeInfo != null) {
            if (annenPartsOverlappendePeriodeInfo.harÅrsakSomIkkeTriggerReberegning()) {
                sumAndreSøkeresTilsyn += annenPartsOverlappendePeriodeInfo.uttaksgrad
            } else {
                val forrigeUttaksgrad = if (this.sisteVedtatteUttaksplanForBehandling.isNotEmpty()) {
                    uttaksplanMedKrav.finnOverlappendeUttaksperiode(periode)?.uttaksgrad ?: Prosent.valueOf(100)
                } else {
                    Prosent.valueOf(100)
                }
                val beregnGraderGrunnlag = BeregnGraderGrunnlag(
                    pleiebehov = pleiebehov,
                    etablertTilsyn = etablertTilsyn,
                    oppgittTilsyn = annenPartsOverlappendePeriodeInfo.oppgittTilsyn,
                    andreSøkeresTilsyn = sumAndreSøkeresTilsyn,
                    andreSøkeresTilsynReberegnet = true, //NB: Alltid true her siden dette er en del av reberegning, men verdien brukes her ikke til noe.
                    overseEtablertTilsynÅrsak = finnOverseEtablertTilsynÅrsak(nattevåkUtfall, beredskapUtfall),
                    arbeid = annenPartsOverlappendePeriodeInfo.utbetalingsgrader.tilArbeid(),
                    ytelseType = ytelseType
                )
                val graderBeregnet = BeregnGrader.beregnMedMaksGrad(
                    beregnGraderGrunnlag,
                    forrigeUttaksgrad
                )
                if (gjelderDenneBehandlingen(uttaksplanMedUUIDMedKrav.behandlingUUID)) {
                    oppdatertGrad = graderBeregnet.uttaksgrad
                }
                sumAndreSøkeresTilsyn += graderBeregnet.uttaksgrad
            }
        }
    }

    val andreSøkersTilsyn = sumAndreSøkeresTilsyn - oppdatertGrad

    if (andreSøkersTilsyn < BigDecimal.ZERO) {
        return Prosent.ZERO
    }
    return andreSøkersTilsyn
}

private fun RegelGrunnlag.gjelderDenneBehandlingen(kildeBehandling: UUID): Boolean {
    return this.behandlingUUID == kildeBehandling
}

private fun UttaksperiodeInfo.harÅrsakSomIkkeTriggerReberegning(): Boolean {
    for (årsak in listOf(
        Årsak.BARNETS_DØDSFALL,
        Årsak.LOVBESTEMT_FERIE,
        Årsak.UTENOM_PLEIEBEHOV,
        Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT
    )) {
        if (this.årsaker.contains(årsak)) {
            return true
        }
    }
    return false
}

private fun List<Utbetalingsgrader>.tilArbeid(): Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo> {
    val arbeid = mutableMapOf<Arbeidsforhold, ArbeidsforholdPeriodeInfo>()
    forEach { utbetalingsgrader ->
        arbeid[utbetalingsgrader.arbeidsforhold] = ArbeidsforholdPeriodeInfo(
            utbetalingsgrader.normalArbeidstid,
            utbetalingsgrader.faktiskArbeidstid ?: Duration.ZERO
        )
    }
    return arbeid
}

private fun Duration.prosentAvFullDag(): Prosent {
    return BigDecimal(this.toMillis()).setScale(2, RoundingMode.HALF_UP)
        .divide(BigDecimal(FULL_DAG.toMillis()), 2, RoundingMode.HALF_UP) * HUNDRE_PROSENT
}

private fun RegelGrunnlag.finnAndreSøkeresTilsynFraUttaksperioder(periode: LukketPeriode): BigDecimal {
    if (this.barn.dødsdato != null && this.barn.dødsdato!! <= periode.fom) {
        return Prosent.ZERO
    }

    val uttaksplanerMedKrav = this.andreSøkeresUttaksplaner(periode)
    val andreSøkeresUttaksplanerMedTidligereVedtak = this.andreSøkeresUttaksplanerMedTidligereVedtak(periode)
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
        uttaksplaner = andreVedtak
    )

    return finnTilsynForUttaksPeriodeFraUttaksplaner(
        periode = periode,
        uttaksplaner = uttaksplanerMedKrav,
        sumAndreSøkeresTilsyn
    )
}

private fun finnTilsynForUttaksPeriodeFraUttaksplaner(
    periode: LukketPeriode, uttaksplaner: List<UttaksplanMedBehandlingUuid>, alleredeForbrukt: BigDecimal = Prosent.ZERO
): BigDecimal {
    var andreSøkeresTilsynsgrad = alleredeForbrukt
    uttaksplaner.forEach { uttaksplan ->
        val overlappendePeriode = uttaksplan.uttaksplan.perioder.keys.firstOrNull { it.overlapperHelt(periode) }
        if (overlappendePeriode != null) {
            val uttaksperiode = uttaksplan.uttaksplan.perioder[overlappendePeriode]
            if (uttaksperiode != null && uttaksperiode.utfall == Utfall.OPPFYLT) {
                andreSøkeresTilsynsgrad += uttaksperiode.uttaksgrad
            }
        }
    }
    return andreSøkeresTilsynsgrad
}

private fun RegelGrunnlag.andreSøkeresUttaksplaner(periode: LukketPeriode): List<UttaksplanMedBehandlingUuid> {
    val kravprioritetPeriode = kravprioritetForBehandlinger.keys.firstOrNull { it.overlapperHelt(periode) }
        ?: return listOf()

    val kravprioritetListe = kravprioritetForBehandlinger[kravprioritetPeriode] ?: return listOf()

    val uttaksplanerMedKrav = mutableListOf<UttaksplanMedBehandlingUuid>()
    for (behandlingMedKrav in kravprioritetListe) {
        if (behandlingMedKrav == this.behandlingUUID) {
            break
        }
        val uttaksplanMedKrav = andrePartersUttaksplanPerBehandling[behandlingMedKrav]
        if (uttaksplanMedKrav != null) {
            uttaksplanerMedKrav.add(UttaksplanMedBehandlingUuid(uttaksplanMedKrav, behandlingMedKrav))
        }
    }
    return uttaksplanerMedKrav
}

private fun RegelGrunnlag.alleSøkeresVedtatteUttaksplaner(periode: LukketPeriode): List<UttaksplanMedBehandlingUuid> {
    val kravprioritetPeriode = kravprioritetForBehandlinger.keys.firstOrNull { it.overlapperHelt(periode) }
        ?: return listOf()

    val kravprioritetListe = kravprioritetForBehandlinger[kravprioritetPeriode] ?: return listOf()

    val uttaksplanerMedKrav = mutableListOf<UttaksplanMedBehandlingUuid>()
    for (behandlingMedKrav in kravprioritetListe) {
        val forrigeUttaksplan = sisteVedtatteUttaksplanForBehandling[behandlingMedKrav] ?: continue
        val uttaksplan = vedtatteUttaksplanPerBehandling[forrigeUttaksplan]
        if (uttaksplan != null) {
            uttaksplanerMedKrav.add(UttaksplanMedBehandlingUuid(uttaksplan, behandlingMedKrav))
        }
    }
    return uttaksplanerMedKrav
}

private fun RegelGrunnlag.andreSøkeresUttaksplanerMedTidligereVedtak(periode: LukketPeriode): List<UttaksplanMedBehandlingUuid> {
    val kravprioritetPeriode = kravprioritetForBehandlinger.keys.firstOrNull { it.overlapperHelt(periode) }
        ?: return listOf()

    val kravprioritetListe = kravprioritetForBehandlinger[kravprioritetPeriode] ?: return listOf()

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
