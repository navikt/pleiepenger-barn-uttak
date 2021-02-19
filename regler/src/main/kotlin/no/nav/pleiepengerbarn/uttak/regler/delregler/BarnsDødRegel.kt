package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.delregler.BarnsDødRegel.Companion.EtHundreProsent
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.*
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.inneholder
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.perioderSomIkkeInngårI
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåTom
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

internal class BarnsDødRegel : UttaksplanRegel {

    internal companion object {
        internal val EtHundreProsent = Prosent(100)
    }

    override fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag): Uttaksplan {
        val dødsdato = grunnlag.barn.dødsdato ?: return uttaksplan

        val perioder = uttaksplan.perioder.sortertPåFom()

        val uttaksperiodeDaBarnetDøde = perioder.inneholder(dødsdato)

        perioder.knekkUttaksperiodenDaBarnetDøde(
                dødsdato = dødsdato,
                uttaksperiodeDaBarnetDøde = uttaksperiodeDaBarnetDøde
        )

        val dødeIEnOppfyltPeriode = perioder
                .inneholder(dødsdato)
                ?.takeIf { it.value.utfall == Utfall.OPPFYLT } != null

        if (!dødeIEnOppfyltPeriode) {
            perioder.avslåAllePerioderEtterDødsfallet(
                    dødsdato = dødsdato,
                    grunnlag = grunnlag
            )
        } else {
            perioder.fjernAllePerioderEtterDødsfallet(
                    dødsdato = dødsdato
            )

            val sorgperiode = grunnlag.utledSorgperiode()

            /**
             *  Kjører nytt uttak for perioden etter barnets død.
             *      -   Kjører uttak som om barnet lever.
             *      -   Søknadsperiodene er kun etter barnets død.
             *      -   Aldri noe tilsynsperioder.
             *      -   Fjerner andre omsorgspersoner.
             *      -  "Taket" på ytelsen totalt sett er nå 1000% (10 personer med 100%)
             *          Setter derfor pleiebehovet alltid til 100% ettersom det kun
             *          er den aktuelle søkeren som er med i beregningen. (Ref. punktet over)
             *      -   Om søknadsperiodene går utover 'sorgperioden' vil `GradBeregner` avslå med årsak
             *          at det er 'UTENOM_PLEIEBEHOV'.
             *          Det overstyres her til at årsaken blir 'BARNETS_DØDSFALL' istedenfor
             */
            val perioderEtterDødsfall = UttakTjeneste.uttaksplan(
                    grunnlag = grunnlag.copy(
                            barn = Barn(
                                    aktørId = grunnlag.barn.aktørId,
                                    dødsdato = null
                            ),
                            andrePartersUttaksplan = mapOf(),
                            søktUttak = grunnlag.søktUttak.søknadsperioderEtterDødsdato(
                                    dødsdato = dødsdato
                            ),
                            tilsynsperioder = emptyMap(),
                            pleiebehov = mapOf(sorgperiode to Pleiebehov.PROSENT_100)
                    )
            ).perioder.mapValues { (_,uttaksPeriodeInfo) ->
                uttaksPeriodeInfo.håndterPeriodeUtenomPleiebehov()
            }

            // Legger til alle periodene etter dødsfallet
            perioder.putAll(perioderEtterDødsfall)

            // Alle perioder i sorgperioden som nå ikke har en uttaksperiode vil få perioder med 100%
            // Arbeidsforholdene hentes fra forrige innvilgede periode
            val sisteDagIUttaksplan = perioder.sortertPåTom().keys.last().tom
            val sisteDagISorgperioden = sorgperiode.tom
            sorgperiode
                    .perioderSomIkkeInngårI(perioder)
                    .filter { it.fom.isAfter(dødsdato) }
                    .plussDelenAvSorgperiodenSomIkkeInngårIUttakplanen(
                            sisteDagIUttaksplan = sisteDagIUttaksplan,
                            sisteDagISorgperioden = sisteDagISorgperioden
                    )
                    .medArbeidsforholdFraForrigeOppfyltePeriode(perioder)
                    .forEach { (periode, arbeidsforholdMedUttbetalingsgrader) ->
                        perioder[periode] = UttaksperiodeInfo.innvilgelse(
                            uttaksgrad = EtHundreProsent,
                            utbetalingsgrader = arbeidsforholdMedUttbetalingsgrader,
                            årsak= Årsak.OPPFYLT_PGA_BARNETS_DØDSFALL,
                            graderingMotTilsyn = null, //Skal ikke ta hensyn til gradering mot tilsyn i sorgperioden, så derfor ikke relevant
                            knekkpunktTyper = setOf(KnekkpunktType.BARNETS_DØDSFALL),
                            kildeBehandlingUUID = grunnlag.behandlingUUID,
                            annenPart = grunnlag.annenPart(periode)
                        )
                    }
        }

        return uttaksplan.copy(
                perioder = perioder
        )
    }
}

/**
 *  - Legger til den siste delen av sorgperioen om den ikke allerde er dekket
 *    som en del av den uttaksplanen.
 */
private fun List<LukketPeriode>.plussDelenAvSorgperiodenSomIkkeInngårIUttakplanen(
        sisteDagIUttaksplan: LocalDate,
        sisteDagISorgperioden: LocalDate) : List<LukketPeriode> {
    return if (sisteDagIUttaksplan.erLikEllerEtter(sisteDagISorgperioden)) {
        this
    } else {
        toMutableList().also {
            it.add(LukketPeriode(
                fom = sisteDagIUttaksplan.plusDays(1),
                tom = sisteDagISorgperioden
            ))
        }
    }
}

/**
 * - Om barnet døde utenom en uttaksperiode forblir periodene som de var
 * - Om barnet døde i en uttaksperiode, men det var siste dag i uttaksperioden forblir periodene som de var
 * - Om barnet døde i en uttaksperiode utenom sise dag i perioden knekkes den i to:
 *      1) FOM til dødsdato - UttaksPeriodeInfo forblir som det var
 *      2) (dødsdato + 1 dag) til TOM - UtttaksPeriodeInfo forblir som det var, med knekkpunkt 'BARNETS_DØDSFALL'
 */
private fun SortedMap<LukketPeriode, UttaksperiodeInfo>.knekkUttaksperiodenDaBarnetDøde(
        dødsdato: LocalDate,
        uttaksperiodeDaBarnetDøde: Uttaksperiode?) {
    uttaksperiodeDaBarnetDøde?.takeUnless { it.key.tom.isEqual(dødsdato) }?.apply {
        val periode = this.key
        val periodeInfo = this.value

        // Fjerner perioden dødsfallet fant sted
        remove(periode)

        // Legger til knekk FOM - dødsdato
        put(LukketPeriode(
                fom = uttaksperiodeDaBarnetDøde.key.fom,
                tom = dødsdato
        ), periodeInfo)

        val periodeInfoMedKnekkpunkt = when (periodeInfo.utfall) {
            Utfall.OPPFYLT -> {
                periodeInfo.copy(knekkpunktTyper = setOf(KnekkpunktType.BARNETS_DØDSFALL))
            }
            Utfall.IKKE_OPPFYLT -> {
                periodeInfo.copy(knekkpunktTyper = setOf(KnekkpunktType.BARNETS_DØDSFALL))
            }
        }
        // Legger til knekk (dødsdato+1) - TOM
        put(LukketPeriode(
                fom = dødsdato.plusDays(1),
                tom = uttaksperiodeDaBarnetDøde.key.tom
        ), periodeInfoMedKnekkpunkt)
    }
}


/**
 *  - En liste med perioder vi ikke har noe informasjon om fra grunnlaget
 *  - Bruker arbeidsforholdene fra forrige oppfylte periode også for denne perioden,
 *    men med ny utbetalingsgrad; 100%
 */
private fun List<LukketPeriode>.medArbeidsforholdFraForrigeOppfyltePeriode(
        perioder: Map<LukketPeriode, UttaksperiodeInfo>
) : Map<LukketPeriode, List<Utbetalingsgrader>> {
    val oppfyltePerioder = perioder
            .filterValues { it.utfall == Utfall.OPPFYLT }
            .mapValues { it.value }

    val map = mutableMapOf<LukketPeriode, List<Utbetalingsgrader>>()
    forEach {
        map[it] = oppfyltePerioder.arbeidsforholdFraForrigeOppfyltePeriode(it)
    }
    return map
}

/**
 *  - Finner utbetalingsgradene for perioden med TOM nærmeste den aktuelle perioden.
 *  - Bruker samme arbeidsforhold men overstyrer alle utbetalingsgradene til 100%
 */
private fun Map<LukketPeriode, UttaksperiodeInfo>.arbeidsforholdFraForrigeOppfyltePeriode(
        periode: LukketPeriode): List<Utbetalingsgrader> {
    return oppfyltPeriodeMedNærmesteTom(periode.fom)
            .utbetalingsgrader
            .map {
                it.copy(utbetalingsgrad = EtHundreProsent)
            }
}

/**
 *  - Finner innvilgede periode med TOM nærmest parameteret FOM
 *    som her er FOM i periden vi mangler informasjon om.
 */
private fun Map<LukketPeriode, UttaksperiodeInfo>.oppfyltPeriodeMedNærmesteTom(fom: LocalDate) : UttaksperiodeInfo {
    var nåværendePeriode = keys.first()
    var nåværendeOppfyltPeriode = values.first()
    var nåværendeMellomrom = ChronoUnit.DAYS.between(nåværendePeriode.tom, fom)

    filterNot { it.key == nåværendePeriode }.forEach { (periode, oppfyltPeriode) ->
        val nyttMellomrom = ChronoUnit.DAYS.between(periode.tom, fom)
        if (nyttMellomrom < nåværendeMellomrom) {
            nåværendePeriode = periode
            nåværendeMellomrom = nyttMellomrom
            nåværendeOppfyltPeriode = oppfyltPeriode
        }
    }
    return nåværendeOppfyltPeriode
}

/**
 *  - Tar utgangspunkt i de opprinnelige søknadsperiodene og returerer perioder etter dødsfallet
 *  - Om dødsfallet fant sted siste dag i en periode forblir søknadsperiodene som de var
 */
private fun List<SøktUttak>.søknadsperioderEtterDødsdato(dødsdato: LocalDate) : List<SøktUttak> {
    val perioderEtterDødfallet =
            filter { it.periode.fom.isAfter(dødsdato) }
            .toMutableList()

    val periodenDødsfalletFantSted =
            find { it.periode.inneholder(dødsdato) }
            ?.takeUnless { dødsdato.isEqual(it.periode.tom) }

    if (periodenDødsfalletFantSted != null) {
        val uttak = SøktUttak(
            LukketPeriode(
                fom = dødsdato.plusDays(1),
                tom = periodenDødsfalletFantSted.periode.tom
            ),
            periodenDødsfalletFantSted.oppgittTilsyn

        )
        perioderEtterDødfallet.add(uttak)
    }
    return perioderEtterDødfallet.toList()
}

/**
 *  - Alle periodene med FOM etter dødsdato avslås
 *      - De som allerede var ikke oppfylt får en ny Årsak 'BARNETS_DØDSFALL'
 *      - De som var oppfylt blir ikke oppfylt med IkkeOppfyltÅrsak 'BARNETS_DØDSFALL'
 */
private fun SortedMap<LukketPeriode, UttaksperiodeInfo>.avslåAllePerioderEtterDødsfallet(dødsdato: LocalDate, grunnlag: RegelGrunnlag) {
    filterKeys { it.fom.isAfter(dødsdato) }.forEach {
        val periodeInfo = it.value
        if (periodeInfo.utfall == Utfall.IKKE_OPPFYLT) {
            val ikkeOppfyltÅrsaker = periodeInfo
                    .årsaker
                    .toMutableSet()
                    .also { årsaker ->
                        årsaker.add(Årsak.BARNETS_DØDSFALL)
                    }
            put(it.key, periodeInfo.copy(
                    årsaker = ikkeOppfyltÅrsaker)
            )
        } else {
            put(it.key, UttaksperiodeInfo.avslag(
                årsaker = setOf(Årsak.BARNETS_DØDSFALL),
                knekkpunktTyper = periodeInfo.knekkpunktTyper,
                kildeBehandlingUUID = grunnlag.behandlingUUID,
                annenPart = grunnlag.annenPart(it.key)
            ))
        }
    }
}

/**
 *  - Alle perider med FOM etter dødsdato fjernes
 */
private fun SortedMap<LukketPeriode, UttaksperiodeInfo>.fjernAllePerioderEtterDødsfallet(
        dødsdato: LocalDate) {
    filterKeys { it.fom.isAfter(dødsdato) }.forEach { (periode, _) ->
        remove(periode)
    }
}

private fun UttaksperiodeInfo.håndterPeriodeUtenomPleiebehov() : UttaksperiodeInfo {
    return if (this.utfall == Utfall.IKKE_OPPFYLT && årsaker.size == 1 && årsaker.first() == Årsak.UTENOM_PLEIEBEHOV) {
        this.copy(
                årsaker = setOf(Årsak.BARNETS_DØDSFALL)
        )
    } else this
}

private fun RegelGrunnlag.utledSorgperiode() = LukketPeriode(
    fom = barn.dagenEtterDødsfall(),
    tom = barn.dagenEtterDødsfall().plusWeeks(6)
)
private fun Barn.dagenEtterDødsfall() = dødsdato!!.plusDays(1)
