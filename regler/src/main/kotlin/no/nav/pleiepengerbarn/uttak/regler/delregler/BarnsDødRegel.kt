package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.delregler.BarnsDødRegel.Companion.EtHundreProsent
import no.nav.pleiepengerbarn.uttak.regler.delregler.BarnsDødRegel.Companion.barnetsDødAvslåttÅrsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.inneholder
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.perioderSomIkkeInngårI
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.BarnetsDødsfall
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class BarnsDødRegel : UttaksplanRegel {
    internal companion object {
        internal val EtHundreProsent = Prosent(100)

        internal fun barnetsDødAvslåttÅrsak(dødsdato: LocalDate) = AvslåttÅrsak(
                årsak = AvslåttÅrsaker.BarnetsDødsfall, // TODO: Bedre beskrivelse på anvendelse..
                hjemler = setOf(BarnetsDødsfall.anvend("Fastsatt at barnet døde $dødsdato."))
        )
        internal fun barnetsDødInnvilgetÅrsak(dødsdato: LocalDate) = InnvilgetÅrsak(
                årsak = InnvilgetÅrsaker.BarnetsDødsfall,
                hjemler = setOf(BarnetsDødsfall.anvend("Fastsatt at barnet døde $dødsdato."))
        )
    }

    override fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag): Uttaksplan {
        val dødsdato = grunnlag.barn.dødsdato ?: return uttaksplan

        val perioder = uttaksplan.perioder.sortertPåFom()

        val innvilgetUttaksperiodeDaBarnetDøde =
                perioder.inneholder(dødsdato)?.takeIf { it.value is InnvilgetPeriode }

        if (innvilgetUttaksperiodeDaBarnetDøde == null) {
            perioder.avslåAllePerioderEtterDødsfallet(dødsdato)
        } else {
            perioder.fjernAllePerioderEtterDødsfallet(
                    dødsdato = dødsdato,
                    innvilgetUttaksperiodeDaBarnetDøde = innvilgetUttaksperiodeDaBarnetDøde
            )
            // Siste dag i siste periode vil nå være barnets dødsdato

            val sorgperiode = grunnlag.utledSorgperiode()

            /*
                Kjører nytt uttak for perioden etter barnets død.
                    -   søknadsperiodene er kun etter barnets død
                    -   aldri noe tilsynsperioder
                    -   taket for ytelsen er 1000 (10 personer med 100%)
             */
            val perioderEtterDødsfall = UttakTjeneste.uttaksplan(
                    grunnlag = grunnlag.copy(
                            søknadsperioder = grunnlag.søknadsperioder.søknadsperioderEtterDødsdato(
                                    dagenEtterDødsfall = grunnlag.barn.dagenEtterDødsfall()
                            ),
                            tilsynsperioder = emptyMap(),
                            tilsynsbehov = mapOf(sorgperiode to Tilsynsbehov(
                                    prosent = TilsynsbehovStørrelse.PROSENT_1000
                            ))
                    )
            ).perioder

            // Legger til alle periodene etter dødsfallet
            perioder.putAll(perioderEtterDødsfall)

            // Alle perioder i sorgperioden som nå ikke har en uttaksperiode vil få perioder med 100%
            // Arbeidsforholdene hentes fra forrige innvilgede periode
            sorgperiode
                    .perioderSomIkkeInngårI(perioder)
                    .medArbeidsforholdFraForrigeInnvilgedePeriode(perioder)
                    .forEach { (periode, arbeidsforholdMedUttbetalingsgrader) ->
                        perioder[periode] = InnvilgetPeriode(
                                knekkpunktTyper = setOf(KnekkpunktType.BarnetsDødsfall),
                                grad = EtHundreProsent,
                                utbetalingsgrader = arbeidsforholdMedUttbetalingsgrader,
                                årsak = barnetsDødInnvilgetÅrsak(dødsdato)
                        )
                    }

        }

        return uttaksplan.copy(
                perioder = perioder
        )
    }
}

private fun List<LukketPeriode>.medArbeidsforholdFraForrigeInnvilgedePeriode(
        perioder: Map<LukketPeriode, UttaksPeriodeInfo>
) : Map<LukketPeriode, Map<ArbeidsforholdRef, Prosent>> {
    val innvilgedePerioder = perioder
            .filterValues { it is InnvilgetPeriode }
            .mapValues { it.value as InnvilgetPeriode }

    val map = mutableMapOf<LukketPeriode, Map<ArbeidsforholdRef, Prosent>>()
    forEach {
        map[it] = innvilgedePerioder.arbeidsforholdFraForrigeInnvilgedePeriode(it)
    }
    return map
}

private fun Map<LukketPeriode, InnvilgetPeriode>.arbeidsforholdFraForrigeInnvilgedePeriode(
        periode: LukketPeriode): Map<ArbeidsforholdRef, Prosent> {
    return innvilgetPeriodeMedNærmesteTom(periode.fom)
            .utbetalingsgrader
            .mapValues { EtHundreProsent }
}

private fun Map<LukketPeriode, InnvilgetPeriode>.innvilgetPeriodeMedNærmesteTom(fom: LocalDate) : InnvilgetPeriode {
    var nåværendeInnvilgetPeriode = values.first()
    var nåværendeMellomrom = Duration.between(keys.first().tom, fom)

    forEach { periode, innvilgetPeriode ->
        val nyttMellomrom = Duration.between(periode.tom, fom)
        if (nyttMellomrom < nåværendeMellomrom) {
            nåværendeMellomrom = nyttMellomrom
            nåværendeInnvilgetPeriode = innvilgetPeriode
        }
    }
    return nåværendeInnvilgetPeriode
}


    private fun List<LukketPeriode>.søknadsperioderEtterDødsdato(dagenEtterDødsfall: LocalDate) : List<LukketPeriode> {
    val perioder = filter { dagenEtterDødsfall.isAfter(it.fom) }.toMutableList()
    val periodenDødsfalletFantSted = find { it.inneholder(dagenEtterDødsfall) }
    if (periodenDødsfalletFantSted != null) {
        perioder.add(LukketPeriode(
                fom = dagenEtterDødsfall,
                tom = periodenDødsfalletFantSted.tom
        ))
    }
    return perioder.toList()
}

/*
    1. Legger til ny avslagsårsak på alle avslåtte perioder etter dødsfallet fant sted
    2. Avslår alle innvilgede perioder etter dødsfallet fant sted med avslagsårsak
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
                        årsaker.add(barnetsDødAvslåttÅrsak(dødsdato))
                    }
            put(it.key, periodeInfo.copy(
                    årsaker = avslåttÅrsaker)
            )
        } else {
            put(it.key, AvslåttPeriode(
                    knekkpunktTyper = periodeInfo.knekkpunktTyper(),
                    årsaker = setOf(barnetsDødAvslåttÅrsak(dødsdato))
            ))
        }
    }
}

/*
    1. Om barnet dør siste dag av uttakperioden forblir den som den var
    2. Om barnet dør et annet tidspunkt i uttaksperioden tar vi kun med oss
        FOM - dødsdato
 */
private fun SortedMap<LukketPeriode, UttaksPeriodeInfo>.fjernAllePerioderEtterDødsfallet(
        dødsdato: LocalDate,
        innvilgetUttaksperiodeDaBarnetDøde: Uttaksperiode) {

    if (!innvilgetUttaksperiodeDaBarnetDøde.key.tom.isEqual(dødsdato)) {
        // Fjerner perioden dødsfallet fant sted
        remove(innvilgetUttaksperiodeDaBarnetDøde.key)

        // Legger til perioden frem til og med dødsfallet slik det var
        val periodeFremTilDødsfallet = LukketPeriode(
                fom = innvilgetUttaksperiodeDaBarnetDøde.key.fom,
                tom = dødsdato
        )
        put(periodeFremTilDødsfallet, innvilgetUttaksperiodeDaBarnetDøde.value)
    }

    // Fjerner alle perioder etter dødsato
    keys.filter { it.fom.isAfter(dødsdato) }.forEach { periode ->
        remove(periode)
    }

}

private fun RegelGrunnlag.utledSorgperiode() = LukketPeriode(
    fom = barn.dagenEtterDødsfall(),
    tom = barn.dagenEtterDødsfall().plusWeeks(6)
)

private fun Barn.dagenEtterDødsfall() = dødsdato!!.plusDays(1)
