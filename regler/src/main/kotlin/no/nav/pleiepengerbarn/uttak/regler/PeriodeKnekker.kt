package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.KnekkpunktType
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak
import no.nav.pleiepengerbarn.uttak.regler.domene.Knekkpunkt
import java.util.SortedSet

internal object PeriodeKnekker {
    internal fun knekk(
        søktUttak: List<SøktUttak>,
        knekkpunkter: SortedSet<Knekkpunkt>) : Map<SøktUttak, Set<KnekkpunktType>> {
        val resultat = mutableMapOf<SøktUttak, MutableSet<KnekkpunktType>>()

        søktUttak.forEach { søktUttaksperiode ->
            var rest = PeriodeMedKnekkpunkttyper(
                    periode = søktUttaksperiode.periode
            )
            knekkpunkter.forEach { knekkpunkt ->
                val knekkDato = knekkpunkt.knekk
                if (!knekkDato.isEqual(søktUttaksperiode.periode.fom) && !knekkDato.isBefore(søktUttaksperiode.periode.fom) && !knekkDato.isAfter(søktUttaksperiode.periode.tom)) {
                    val periode = LukketPeriode(
                            fom = rest.periode.fom,
                            tom = knekkDato.minusDays(1)
                    )
                    val uttak = SøktUttak(periode, søktUttaksperiode.oppgittTilsyn)
                    resultat.putIfAbsent(uttak, mutableSetOf())
                    resultat[uttak]!!.addAll(rest.knekkpunkttyper)
                    rest = PeriodeMedKnekkpunkttyper(
                            periode = LukketPeriode(
                                    fom = knekkDato,
                                    tom = rest.periode.tom
                            ),
                            knekkpunkttyper = knekkpunkt.typer
                    )
                }
            }
            val uttak = SøktUttak(rest.periode, søktUttaksperiode.oppgittTilsyn)
            resultat.putIfAbsent(uttak, mutableSetOf())
            resultat[uttak]!!.addAll(rest.knekkpunkttyper)

        }
        return resultat.toMap()
    }

    private data class PeriodeMedKnekkpunkttyper(
            val periode: LukketPeriode,
            val knekkpunkttyper : Set<KnekkpunktType> = setOf()
    )
}