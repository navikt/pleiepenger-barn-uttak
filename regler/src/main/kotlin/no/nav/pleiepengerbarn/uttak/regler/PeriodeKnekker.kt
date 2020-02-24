package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Knekkpunkt
import no.nav.pleiepengerbarn.uttak.kontrakter.KnekkpunktType
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import java.util.*

internal object PeriodeKnekker {
    internal fun knekk(
            grunnlag: RegelGrunnlag,
            knekkpunkter: SortedSet<Knekkpunkt>) : Map<LukketPeriode, Set<KnekkpunktType>> {
        val resultat = mutableMapOf<LukketPeriode, MutableSet<KnekkpunktType>>()
        grunnlag.søknadsperioder.forEach { søknadsperiode ->
            var rest = PeriodeMedKnekkpunkttyper(
                    periode = søknadsperiode
            )
            knekkpunkter.forEach { knekkpunkt ->
                val knekkDato = knekkpunkt.knekk
                if (!knekkDato.isEqual(søknadsperiode.fom) && !knekkDato.isBefore(søknadsperiode.fom) && !knekkDato.isAfter(søknadsperiode.tom)) {
                    val periode = LukketPeriode(
                            fom = rest.periode.fom,
                            tom = knekkDato.minusDays(1)
                    )
                    resultat.putIfAbsent(periode, mutableSetOf())
                    resultat[periode]!!.addAll(rest.knekkpunkttyper)
                    rest = PeriodeMedKnekkpunkttyper(
                            periode = LukketPeriode(
                                    fom = knekkDato,
                                    tom = rest.periode.tom
                            ),
                            knekkpunkttyper = knekkpunkt.typer
                    )
                }
            }
            resultat.putIfAbsent(rest.periode, mutableSetOf())
            resultat[rest.periode]!!.addAll(rest.knekkpunkttyper)

        }
        return resultat.toMap()
    }

    private data class PeriodeMedKnekkpunkttyper(
            internal val periode: LukketPeriode,
            internal val knekkpunkttyper : Set<KnekkpunktType> = setOf()
    )
}