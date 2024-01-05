package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.KnekkpunktType
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak
import no.nav.pleiepengerbarn.uttak.regler.domene.Knekkpunkt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.util.SortedSet
import java.util.TreeSet

class PeriodeKnekkerTest {

    @Test
    fun `skal legge til knekkpunkt om knekkpunkt lik tom`() {

        val fom = LocalDate.now()
        val tom = fom.plusDays(3)
        val søktUttak = SøktUttak(LukketPeriode(fom, tom), Duration.ofHours(2))
        val knekk = tom
        val resultat =
            PeriodeKnekker.knekk(listOf(søktUttak), setOf(Knekkpunkt(knekk, setOf(KnekkpunktType.ARBEID))).toSortedSet(compareBy { it.knekk }))

        assertThat(resultat.size).isEqualTo(2);
        val iterator = resultat.entries.iterator()
        val førsteDel = iterator.next()
        assertThat(førsteDel.key.periode.fom).isEqualTo(fom);
        assertThat(førsteDel.key.periode.tom).isEqualTo(knekk.minusDays(1));
        assertThat(førsteDel.value.size).isEqualTo(0);

        val andreDel = iterator.next()
        assertThat(andreDel.key.periode.fom).isEqualTo(knekk);
        assertThat(andreDel.key.periode.tom).isEqualTo(tom);
        assertThat(andreDel.value.size).isEqualTo(1);
        assertThat(andreDel.value.first()).isEqualTo(KnekkpunktType.ARBEID);

    }

    @Test
    fun `skal legge til knekkpunkt om knekkpunkt lik midt i periode`() {

        val fom = LocalDate.now()
        val tom = fom.plusDays(3)
        val søktUttak = SøktUttak(LukketPeriode(fom, tom), Duration.ofHours(2))
        val knekk = tom.minusDays(1)
        val resultat =
            PeriodeKnekker.knekk(listOf(søktUttak), setOf(Knekkpunkt(knekk, setOf(KnekkpunktType.ARBEID))).toSortedSet(compareBy { it.knekk }))

        assertThat(resultat.size).isEqualTo(2);
        val iterator = resultat.entries.iterator()
        val førsteDel = iterator.next()
        assertThat(førsteDel.key.periode.fom).isEqualTo(fom);
        assertThat(førsteDel.key.periode.tom).isEqualTo(knekk.minusDays(1));
        assertThat(førsteDel.value.size).isEqualTo(0);

        val andreDel = iterator.next()
        assertThat(andreDel.key.periode.fom).isEqualTo(knekk);
        assertThat(andreDel.key.periode.tom).isEqualTo(tom);
        assertThat(andreDel.value.size).isEqualTo(1);
        assertThat(andreDel.value.first()).isEqualTo(KnekkpunktType.ARBEID);

    }

    @Test
    fun `skal ikke legge til knekkpunkt om knekkpunkt lik fom`() {

        val fom = LocalDate.now()
        val tom = fom.plusDays(3)
        val søktUttak = SøktUttak(LukketPeriode(fom, tom), Duration.ofHours(2))
        val knekk = fom
        val resultat =
            PeriodeKnekker.knekk(listOf(søktUttak), setOf(Knekkpunkt(knekk, setOf(KnekkpunktType.ARBEID))).toSortedSet(compareBy { it.knekk }))

        assertThat(resultat.size).isEqualTo(1);
        val iterator = resultat.entries.iterator()
        val førsteDel = iterator.next()
        assertThat(førsteDel.key.periode.fom).isEqualTo(fom);
        assertThat(førsteDel.key.periode.tom).isEqualTo(tom);
        assertThat(førsteDel.value.size).isEqualTo(0);
    }

    @Test
    fun `skal legge til flere knekkpunkter om knekkpunkt lik midt i periode`() {

        val fom = LocalDate.now()
        val tom = fom.plusDays(5)
        val søktUttak = SøktUttak(LukketPeriode(fom, tom), Duration.ofHours(2))
        val knekk1 = fom.plusDays(1)
        val knekk2 = fom.plusDays(3)

        val resultat =
            PeriodeKnekker.knekk(listOf(søktUttak),
                setOf(
                    Knekkpunkt(knekk1, setOf(KnekkpunktType.ARBEID)),
                    Knekkpunkt(knekk2, setOf(KnekkpunktType.ARBEID))).toSortedSet(compareBy { it.knekk }))

        assertThat(resultat.size).isEqualTo(3);
        val iterator = resultat.entries.iterator()
        val førsteDel = iterator.next()
        assertThat(førsteDel.key.periode.fom).isEqualTo(fom);
        assertThat(førsteDel.key.periode.tom).isEqualTo(knekk1.minusDays(1));
        assertThat(førsteDel.value.size).isEqualTo(0);

        val andreDel = iterator.next()
        assertThat(andreDel.key.periode.fom).isEqualTo(knekk1);
        assertThat(andreDel.key.periode.tom).isEqualTo(knekk2.minusDays(1));
        assertThat(andreDel.value.size).isEqualTo(1);
        assertThat(andreDel.value.first()).isEqualTo(KnekkpunktType.ARBEID);

        val tredjeDel = iterator.next()
        assertThat(tredjeDel.key.periode.fom).isEqualTo(knekk2);
        assertThat(tredjeDel.key.periode.tom).isEqualTo(tom);
        assertThat(tredjeDel.value.size).isEqualTo(1);
        assertThat(tredjeDel.value.first()).isEqualTo(KnekkpunktType.ARBEID);

    }


    @Test
    fun `skal ikke legge til knekkpunkt utenfor søknadsperiode`() {

        val fom = LocalDate.now()
        val tom = fom.plusDays(5)
        val søktUttak = SøktUttak(LukketPeriode(fom, tom), Duration.ofHours(2))
        val knekk1 = fom.minusDays(1)
        val knekk2 = fom.plusDays(3)

        val resultat =
            PeriodeKnekker.knekk(listOf(søktUttak),
                setOf(
                    Knekkpunkt(knekk1, setOf(KnekkpunktType.ARBEID)),
                    Knekkpunkt(knekk2, setOf(KnekkpunktType.ARBEID))).toSortedSet(compareBy { it.knekk }))

        assertThat(resultat.size).isEqualTo(2);
        val iterator = resultat.entries.iterator()
        val førsteDel = iterator.next()
        assertThat(førsteDel.key.periode.fom).isEqualTo(fom);
        assertThat(førsteDel.key.periode.tom).isEqualTo(knekk2.minusDays(1));
        assertThat(førsteDel.value.size).isEqualTo(0);

        val andreDel = iterator.next()
        assertThat(andreDel.key.periode.fom).isEqualTo(knekk2);
        assertThat(andreDel.key.periode.tom).isEqualTo(tom);
        assertThat(andreDel.value.size).isEqualTo(1);
        assertThat(andreDel.value.first()).isEqualTo(KnekkpunktType.ARBEID);

    }

    @Test
    fun `skal legge til knekkpunkt for flere søknadsperioder`() {

        val fom1 = LocalDate.now()
        val fom2 = LocalDate.now().plusDays(3)
        val tom1 = fom2.minusDays(1)
        val tom2 = fom2.plusDays(5)

        val søktUttak1 = SøktUttak(LukketPeriode(fom1, tom1), Duration.ofHours(2))
        val knekk1 = fom1.plusDays(1)

        val søktUttak2 = SøktUttak(LukketPeriode(fom2, tom2), Duration.ofHours(2))

        val knekk2 = fom2.plusDays(3)

        val resultat =
            PeriodeKnekker.knekk(listOf(søktUttak1, søktUttak2),
                setOf(
                    Knekkpunkt(knekk1, setOf(KnekkpunktType.ARBEID)),
                    Knekkpunkt(knekk2, setOf(KnekkpunktType.ARBEID))).toSortedSet(compareBy { it.knekk }))

        assertThat(resultat.size).isEqualTo(4);
        val iterator = resultat.entries.iterator()
        val førsteDel = iterator.next()
        assertThat(førsteDel.key.periode.fom).isEqualTo(fom1);
        assertThat(førsteDel.key.periode.tom).isEqualTo(knekk1.minusDays(1));
        assertThat(førsteDel.value.size).isEqualTo(0);

        val andreDel = iterator.next()
        assertThat(andreDel.key.periode.fom).isEqualTo(knekk1);
        assertThat(andreDel.key.periode.tom).isEqualTo(tom1);
        assertThat(andreDel.value.size).isEqualTo(1);
        assertThat(andreDel.value.first()).isEqualTo(KnekkpunktType.ARBEID);

        val tredjeDel = iterator.next()
        assertThat(tredjeDel.key.periode.fom).isEqualTo(fom2);
        assertThat(tredjeDel.key.periode.tom).isEqualTo(knekk2.minusDays(1));
        assertThat(tredjeDel.value.size).isEqualTo(0);

        val fjerdeDel = iterator.next()
        assertThat(fjerdeDel.key.periode.fom).isEqualTo(knekk2);
        assertThat(fjerdeDel.key.periode.tom).isEqualTo(tom2);
        assertThat(fjerdeDel.value.size).isEqualTo(1);
        assertThat(fjerdeDel.value.first()).isEqualTo(KnekkpunktType.ARBEID);

    }

}
