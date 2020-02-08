package no.nav.pleiepengerbarn.uttak.kontrakter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class UttaksperiodeTest {

    val periode = Uttaksperiode(periode = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31)))


    @Test
    fun `Ingen knekkpunkter skal føre til periode uten knekk`() {
        val perioder = periode.knekk(
                listOf()
        )

        assertEquals(1, perioder.size)
        assertEquals(periode, perioder[0])
    }

    @Test
    fun `Et knekkpunkt innenfor en perioder skal føre til to peroider`() {
        val perioder = periode.knekk(listOf(
                Knekkpunkt(knekk = LocalDate.of(2020, Month.JANUARY, 13), typer = setOf(KnekkpunktType.ARBEID))
        ))

        assertEquals(2, perioder.size)
        sjekkPerioder(LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 12)), setOf(), perioder[0])
        sjekkPerioder(LukketPeriode(LocalDate.of(2020, Month.JANUARY, 13), LocalDate.of(2020, Month.JANUARY, 31)), setOf(KnekkpunktType.ARBEID), perioder[1])
    }

    @Test
    fun `Et knekkpunkt utenfor en perioder skal føre til uendret peroide`() {
        val perioder = periode.knekk(listOf(
                Knekkpunkt(knekk = LocalDate.of(2020, Month.FEBRUARY, 13), typer = setOf(KnekkpunktType.ARBEID))
        ))

        assertEquals(1, perioder.size)
        sjekkPerioder(LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31)), setOf(), perioder[0])
    }

    @Test
    fun `Et knekkpunkt på starten av en perioder skal føre til uendret peroide`() {
        val perioder = periode.knekk(listOf(
                Knekkpunkt(knekk = LocalDate.of(2020, Month.JANUARY, 1), typer = setOf(KnekkpunktType.ARBEID))
        ))

        assertEquals(1, perioder.size)
        sjekkPerioder(LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31)), setOf(), perioder[0])
    }

    @Test
    fun `Et knekkpunkt på slutten av en perioder skal føre til to perioder`() {
        val perioder = periode.knekk(listOf(
                Knekkpunkt(knekk = LocalDate.of(2020, Month.JANUARY, 31), typer = setOf(KnekkpunktType.ARBEID))
        ))

        assertEquals(2, perioder.size)
        sjekkPerioder(LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 30)), setOf(), perioder[0])
        sjekkPerioder(LukketPeriode(LocalDate.of(2020, Month.JANUARY, 31), LocalDate.of(2020, Month.JANUARY, 31)), setOf(KnekkpunktType.ARBEID), perioder[1])
    }

    @Test
    fun `To knekkpunkt innenfor en periode skal føre til tre perioder`() {
        val perioder = periode.knekk(listOf(
                Knekkpunkt(knekk = LocalDate.of(2020, Month.JANUARY, 13), typer = setOf(KnekkpunktType.ARBEID)),
                Knekkpunkt(knekk = LocalDate.of(2020, Month.JANUARY, 25), typer = setOf(KnekkpunktType.ANNEN_PARTS_UTTAK))
        ))

        assertEquals(3, perioder.size)
        sjekkPerioder(LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 12)), setOf(), perioder[0])
        sjekkPerioder(LukketPeriode(LocalDate.of(2020, Month.JANUARY, 13), LocalDate.of(2020, Month.JANUARY, 24)), setOf(KnekkpunktType.ARBEID), perioder[1])
        sjekkPerioder(LukketPeriode(LocalDate.of(2020, Month.JANUARY, 25), LocalDate.of(2020, Month.JANUARY, 31)), setOf(KnekkpunktType.ANNEN_PARTS_UTTAK), perioder[2])
    }

    private fun sjekkPerioder(periode: LukketPeriode, knekkpunktTyper: Set<KnekkpunktType>, uttaksperiode: Uttaksperiode) {
        assertEquals(periode.fom, uttaksperiode.periode.fom)
        assertEquals(periode.fom, uttaksperiode.periode.fom)
        assertEquals(knekkpunktTyper, uttaksperiode.knekkpunktTyper)
    }


}