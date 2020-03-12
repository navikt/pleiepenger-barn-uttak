package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.delregler.BarnsDødRegel.Companion.EtHundreProsent
import no.nav.pleiepengerbarn.uttak.regler.delregler.BarnsDødRegel.Companion.barnetsDødUtenforInnvilgetPeriodeAvslåttÅrsak
import no.nav.pleiepengerbarn.uttak.regler.delregler.BarnsDødRegel.Companion.periodeEtterSorgperiodenAvslåttÅrsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.*
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.inneholder
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.perioderSomIkkeInngårI
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåTom
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.BarnetsDødsfall
import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

internal class BarnsDødRegel : UttaksplanRegel {
    internal companion object {
        internal val EtHundreProsent = Prosent(100)

        internal fun barnetsDødUtenforInnvilgetPeriodeAvslåttÅrsak(dødsdato: LocalDate) = AvslåttÅrsak(
                årsak = AvslåttÅrsaker.BARNETS_DØDSFALL,
                hjemler = setOf(BarnetsDødsfall.anvend(
                        "Fastsatt at barnet døde utenfor en innvilget periode ($dødsdato). " +
                                "Perioden avslås derfor ettersom det ikke foreligger rett til pleiepenger."
                ))
        )

        internal fun periodeEtterSorgperiodenAvslåttÅrsak(dødsdato: LocalDate) = AvslåttÅrsak(
                årsak = AvslåttÅrsaker.BARNETS_DØDSFALL,
                hjemler = setOf(BarnetsDødsfall.anvend(
                        "Fastsatt at barnet døde i løpet av en innvilget periode ($dødsdato). " +
                                "Perioden avslås ettersom den er etter 6 uker etter at dødsfallet fant sted."
                ))
        )

        internal fun barnetsDødInnvilgetÅrsak(dødsdato: LocalDate) = InnvilgetÅrsak(
                årsak = InnvilgetÅrsaker.BARNETS_DØDSFALL,
                hjemler = setOf(BarnetsDødsfall.anvend(
                        "Fastsatt at barnet døde i løpet av en innvilget periode ($dødsdato). " +
                                "Perioden innvilges derfor ettersom den er innenfor 6 uker etter at dødsfallet fant sted. "
                ))
        )
    }

    override fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag): Uttaksplan {
        val dødsdato = grunnlag.barn.dødsdato ?: return uttaksplan

        val perioder = uttaksplan.perioder.sortertPåFom()

        val uttaksperiodeDaBarnetDøde = perioder.inneholder(dødsdato)

        perioder.knekkUttaksperiodenDaBarnetDøde(
                dødsdato = dødsdato,
                uttaksperiodeDaBarnetDøde = uttaksperiodeDaBarnetDøde
        )

        val dødeIEnInnvilgetPeriode = perioder
                .inneholder(dødsdato)
                ?.takeIf { it.value is InnvilgetPeriode } != null

        if (!dødeIEnInnvilgetPeriode) {
            perioder.avslåAllePerioderEtterDødsfallet(
                    dødsdato = dødsdato
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
             *          Setter derfor tilsynsbehovet alltid til 100% ettersom det kun
             *          er den aktuelle søkeren som er med i beregningen. (Ref. punktet over)
             *      -   Om søknadsperiodene går utover 'sorgperioden' vil `GradBeregner` avslå med årsak
             *          at det er 'UTENOM_TILSYNSBEHOV'.
             *          Det overstyres her til at årsaken blir 'BARNETS_DØDSFALL' istedenfor
             */
            val perioderEtterDødsfall = UttakTjeneste.uttaksplan(
                    grunnlag = grunnlag.copy(
                            barn = Barn(
                                    dødsdato = null
                            ),
                            andrePartersUttaksplan = listOf(),
                            søknadsperioder = grunnlag.søknadsperioder.søknadsperioderEtterDødsdato(
                                    dødsdato = dødsdato
                            ),
                            tilsynsperioder = emptyMap(),
                            tilsynsbehov = mapOf(sorgperiode to Tilsynsbehov(
                                    prosent = TilsynsbehovStørrelse.PROSENT_100
                            ))
                    )
            ).perioder.mapValues { (_,uttaksPeriodeInfo) ->
                uttaksPeriodeInfo.håndterPeriodeUtenomTilsynsbehov(dødsdato)
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
                    .medArbeidsforholdFraForrigeInnvilgedePeriode(perioder)
                    .forEach { (periode, arbeidsforholdMedUttbetalingsgrader) ->
                        val innvilgetÅrsak = barnetsDødInnvilgetÅrsak(dødsdato)
                        perioder[periode] = InnvilgetPeriode(
                                knekkpunktTyper = setOf(KnekkpunktType.BARNETS_DØDSFALL),
                                grad = EtHundreProsent,
                                utbetalingsgrader = arbeidsforholdMedUttbetalingsgrader,
                                årsak = innvilgetÅrsak
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
private fun SortedMap<LukketPeriode, UttaksPeriodeInfo>.knekkUttaksperiodenDaBarnetDøde(
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

        val periodeInfoMedKnekkpunkt = when (periodeInfo) {
            is InnvilgetPeriode -> {
                periodeInfo.copy(knekkpunktTyper = setOf(KnekkpunktType.BARNETS_DØDSFALL))
            }
            is AvslåttPeriode -> {
                periodeInfo.copy(knekkpunktTyper = setOf(KnekkpunktType.BARNETS_DØDSFALL))
            }
            else -> throw IllegalStateException("Må være en innvilget eller avslått periode.")
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
 *  - Bruker arbeidsforholdene fra forrige innvilgede periode også for denne perioden,
 *    men med ny utbetalingsgrad; 100%
 */
private fun List<LukketPeriode>.medArbeidsforholdFraForrigeInnvilgedePeriode(
        perioder: Map<LukketPeriode, UttaksPeriodeInfo>
) : Map<LukketPeriode, List<Utbetalingsgrader>> {
    val innvilgedePerioder = perioder
            .filterValues { it is InnvilgetPeriode }
            .mapValues { it.value as InnvilgetPeriode }

    val map = mutableMapOf<LukketPeriode, List<Utbetalingsgrader>>()
    forEach {
        map[it] = innvilgedePerioder.arbeidsforholdFraForrigeInnvilgedePeriode(it)
    }
    return map
}

/**
 *  - Finner utbetalingsgradene for perioden med TOM nærmeste den aktuelle perioden.
 *  - Bruker samme arbeidsforhold men overstyrer alle utbetalingsgradene til 100%
 */
private fun Map<LukketPeriode, InnvilgetPeriode>.arbeidsforholdFraForrigeInnvilgedePeriode(
        periode: LukketPeriode): List<Utbetalingsgrader> {
    return innvilgetPeriodeMedNærmesteTom(periode.fom)
            .utbetalingsgrader
            .map {
                it.copy(utbetalingsgrad = EtHundreProsent)
            }
}

/**
 *  - Finner innvilgede periode med TOM nærmest parameteret FOM
 *    som her er FOM i periden vi mangler informasjon om.
 */
private fun Map<LukketPeriode, InnvilgetPeriode>.innvilgetPeriodeMedNærmesteTom(fom: LocalDate) : InnvilgetPeriode {
    var nåværendePeriode = keys.first()
    var nåværendeInnvilgetPeriode = values.first()
    var nåværendeMellomrom = ChronoUnit.DAYS.between(nåværendePeriode.tom, fom)

    filterNot { it.key == nåværendePeriode }.forEach { (periode, innvilgetPeriode) ->
        val nyttMellomrom = ChronoUnit.DAYS.between(periode.tom, fom)
        if (nyttMellomrom < nåværendeMellomrom) {
            nåværendePeriode = periode
            nåværendeMellomrom = nyttMellomrom
            nåværendeInnvilgetPeriode = innvilgetPeriode
        }
    }
    return nåværendeInnvilgetPeriode
}

/**
 *  - Tar utgangspunkt i de opprinnelige søknadsperiodene og returerer perioder etter dødsfallet
 *  - Om dødsfallet fant sted siste dag i en periode forblir søknadsperiodene som de var
 */
private fun List<LukketPeriode>.søknadsperioderEtterDødsdato(dødsdato: LocalDate) : List<LukketPeriode> {
    val perioderEtterDødfallet =
            filter { it.fom.isAfter(dødsdato) }
            .toMutableList()

    val periodenDødsfalletFantSted =
            find { it.inneholder(dødsdato) }
            ?.takeUnless { dødsdato.isEqual(it.tom) }

    if (periodenDødsfalletFantSted != null) {
        perioderEtterDødfallet.add(LukketPeriode(
                fom = dødsdato.plusDays(1),
                tom = periodenDødsfalletFantSted.tom
        ))
    }
    return perioderEtterDødfallet.toList()
}

/**
 *  - Alle periodene med FOM etter dødsdato avslås
 *      - De som allerede var avslått får en ny AvslåttÅrsak 'BARNETS_DØDSFALL'
 *      - De som var innvilget blir avslått med AvslåttÅrsak 'BARNETS_DØDSFALL'
 */
private fun SortedMap<LukketPeriode, UttaksPeriodeInfo>.avslåAllePerioderEtterDødsfallet(
        dødsdato: LocalDate) {
    filterKeys { it.fom.isAfter(dødsdato) }.forEach {
        val periodeInfo = it.value
        if (periodeInfo is AvslåttPeriode) {
            val avslåttÅrsaker = periodeInfo
                    .årsaker
                    .toMutableSet()
                    .also { årsaker ->
                        årsaker.add(barnetsDødUtenforInnvilgetPeriodeAvslåttÅrsak(dødsdato))
                    }
            put(it.key, periodeInfo.copy(
                    årsaker = avslåttÅrsaker)
            )
        } else {
            put(it.key, AvslåttPeriode(
                    knekkpunktTyper = periodeInfo.knekkpunktTyper(),
                    årsaker = setOf(barnetsDødUtenforInnvilgetPeriodeAvslåttÅrsak(dødsdato))
            ))
        }
    }
}

/**
 *  - Alle perider med FOM etter dødsdato fjernes
 */
private fun SortedMap<LukketPeriode, UttaksPeriodeInfo>.fjernAllePerioderEtterDødsfallet(
        dødsdato: LocalDate) {
    filterKeys { it.fom.isAfter(dødsdato) }.forEach { (periode, _) ->
        remove(periode)
    }
}

private fun UttaksPeriodeInfo.håndterPeriodeUtenomTilsynsbehov(dødsdato: LocalDate) : UttaksPeriodeInfo {
    return if (this is AvslåttPeriode && årsaker.size == 1 && årsaker.first().årsak == AvslåttÅrsaker.UTENOM_TILSYNSBEHOV) {
        this.copy(
                årsaker = setOf(periodeEtterSorgperiodenAvslåttÅrsak(dødsdato))
        )
    } else this
}

private fun RegelGrunnlag.utledSorgperiode() = LukketPeriode(
    fom = barn.dagenEtterDødsfall(),
    tom = barn.dagenEtterDødsfall().plusWeeks(6)
)
private fun Barn.dagenEtterDødsfall() = dødsdato!!.plusDays(1)
