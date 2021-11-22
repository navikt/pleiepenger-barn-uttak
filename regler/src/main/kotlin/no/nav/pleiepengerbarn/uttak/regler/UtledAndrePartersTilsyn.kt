package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
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
        søkersEtablertTilsyn.prosentAvFullDag().compareTo(uttaksperiodeInfo.graderingMotTilsyn?.etablertTilsyn ?: Prosent.ZERO) != 0
    }
    val nattevåkEndret = andrePartersUttaksplanPerBehandling.endret(periode) { uttaksperiodeInfo -> uttaksperiodeInfo.nattevåk != søkersNattevåk }
    val beredskapEndret = andrePartersUttaksplanPerBehandling.endret(periode) { uttaksperiodeInfo -> uttaksperiodeInfo.beredskap != søkersBeredskap }
    val pleiebehovEndret = andrePartersUttaksplanPerBehandling.endret(periode) { uttaksperiodeInfo -> uttaksperiodeInfo.pleiebehov.compareTo(søkersPleiebehov.prosent) != 0 }

    val måReberegneAndrePartersTilsyn = true in listOf(etablertTilsynEndret, nattevåkEndret, beredskapEndret, pleiebehovEndret)

    val andrePartersTilsyn = if (måReberegneAndrePartersTilsyn) {
        reberegnAndreSøkeresTilsynKravprioritetBehandling(periode, søkersPleiebehov, søkersEtablertTilsyn, søkersNattevåk, søkersBeredskap)
    } else {
        finnAndreSøkeresTilsynFraUttaksperioder(periode)
    }
    return Pair(måReberegneAndrePartersTilsyn, andrePartersTilsyn)
}

private fun Map<UUID, Uttaksplan>.endret(periode: LukketPeriode, sjekkSøkersVerdi: (UttaksperiodeInfo) -> Boolean): Boolean {
    return this.any { (_, uttaksplan) ->
        val periodeInfo = uttaksplan.finnOverlappendeUttaksperiode(periode)
        periodeInfo != null && sjekkSøkersVerdi(periodeInfo)
    }
}

private fun Uttaksplan.finnOverlappendeUttaksperiode(periode: LukketPeriode): UttaksperiodeInfo? {
    val uttaksperiode = perioder.keys.firstOrNull { it.overlapperHelt(periode) } ?: return null
    return perioder[uttaksperiode]
}

private fun RegelGrunnlag.reberegnAndreSøkeresTilsynKravprioritetBehandling(periode: LukketPeriode, pleiebehov: Pleiebehov, etablertTilsyn: Duration, nattevåkUtfall: Utfall?, beredskapUtfall: Utfall?): Prosent {
    val uttaksplanerMedKrav = this.andreSøkeresUttaksplaner(periode)

    var sumAndreSøkeresTilsyn = Prosent.ZERO

    for (uttaksplanMedKrav in uttaksplanerMedKrav) {
        val annenPartsOverlappendePeriodeInfo = uttaksplanMedKrav.finnOverlappendeUttaksperiode(periode)
        if (annenPartsOverlappendePeriodeInfo != null) {
            if (annenPartsOverlappendePeriodeInfo.harÅrsakSomIkkeTriggerReberegning()) {
                sumAndreSøkeresTilsyn += annenPartsOverlappendePeriodeInfo.uttaksgrad
            } else {
                val graderBeregnet = BeregnGrader.beregn(
                    pleiebehov,
                    etablertTilsyn,
                    annenPartsOverlappendePeriodeInfo.oppgittTilsyn,
                    sumAndreSøkeresTilsyn,
                    true, //NB: Alltid true her siden dette er en del av reberegning, men verdien brukes her ikke til noe.
                    finnOverseEtablertTilsynÅrsak(nattevåkUtfall, beredskapUtfall),
                    annenPartsOverlappendePeriodeInfo.utbetalingsgrader.tilArbeid()
                )
                sumAndreSøkeresTilsyn += graderBeregnet.uttaksgrad
            }
        }
    }

    return sumAndreSøkeresTilsyn
}

private fun UttaksperiodeInfo.harÅrsakSomIkkeTriggerReberegning(): Boolean {
    for (årsak in listOf(Årsak.BARNETS_DØDSFALL, Årsak.LOVBESTEMT_FERIE, Årsak.UTENOM_PLEIEBEHOV, Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT)) {
        if (this.årsaker.contains(årsak)) {
            return true
        }
    }
    return false
}

private fun List<Utbetalingsgrader>.tilArbeid(): Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo> {
    val arbeid = mutableMapOf<Arbeidsforhold, ArbeidsforholdPeriodeInfo>()
    forEach { utbetalingsgrader ->
        arbeid[utbetalingsgrader.arbeidsforhold] = ArbeidsforholdPeriodeInfo(utbetalingsgrader.normalArbeidstid, utbetalingsgrader.faktiskArbeidstid ?: Duration.ZERO)
    }
    return arbeid
}

private fun RegelGrunnlag.finnEtablertTilsyn(periode: LukketPeriode): Duration {
    val etablertTilsynPeriode = this.tilsynsperioder.keys.firstOrNull {it.overlapperHelt(periode)}
    return if (etablertTilsynPeriode != null) {
        this.tilsynsperioder[etablertTilsynPeriode] ?: Duration.ZERO
    } else {
        Duration.ZERO
    }
}

private fun Duration.prosentAvFullDag(): Prosent {
    return BigDecimal(this.toMillis()).setScale(2, RoundingMode.HALF_UP) / BigDecimal(FULL_DAG.toMillis()) * HUNDRE_PROSENT
}

private fun RegelGrunnlag.finnAndreSøkeresTilsynFraUttaksperioder(periode: LukketPeriode): BigDecimal {
    var andreSøkeresTilsynsgrad = BigDecimal.ZERO
    this.andreSøkeresUttaksplaner(periode).forEach { uttaksplan ->
        val overlappendePeriode = uttaksplan.perioder.keys.firstOrNull {it.overlapperHelt(periode)}
        if (overlappendePeriode != null) {
            val uttaksperiode = uttaksplan.perioder[overlappendePeriode]
            if (uttaksperiode != null && uttaksperiode.utfall  == Utfall.OPPFYLT) {
                andreSøkeresTilsynsgrad += uttaksperiode.uttaksgrad
            }
        }
    }
    return andreSøkeresTilsynsgrad
}

private fun RegelGrunnlag.andreSøkeresUttaksplaner(periode: LukketPeriode): List<Uttaksplan> {
    val kravprioritetPeriode = kravprioritetForBehandlinger.keys.firstOrNull { it.overlapperHelt(periode) }
        ?: return listOf()

    val kravprioritetListe = kravprioritetForBehandlinger[kravprioritetPeriode] ?: return listOf()

    val uttaksplanerMedKrav = mutableListOf<Uttaksplan>()
    for (behandlingMedKrav in kravprioritetListe) {
        if (behandlingMedKrav == this.behandlingUUID) {
            break
        }
        val uttaksplanMedKrav = andrePartersUttaksplanPerBehandling[behandlingMedKrav]
        if (uttaksplanMedKrav != null) {
            uttaksplanerMedKrav.add(uttaksplanMedKrav)
        }
    }
    return uttaksplanerMedKrav
}