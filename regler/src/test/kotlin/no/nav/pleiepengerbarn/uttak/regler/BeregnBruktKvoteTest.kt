package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.util.*

internal class BeregnBruktKvoteTest {

    @Test
    internal fun `Kvoten er ikke oversteget og skal bli 46 når søker og annen part tar ut 23 dager hver`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31)) // 23 dager

        val annenPartsBehandlingUUID = nesteBehandlingUUID()
        val andrePartsUttak = mapOf(
                annenPartsBehandlingUUID to Uttaksplan(
                        perioder = mapOf(
                                helePerioden to dummyUttaksperiodeInfo()
                        ),
                        trukketUttak = listOf()
                )
        )

        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                helePerioden to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val erKvotenOversteget = BeregnBruktKvote.erKvotenOversteget(søkersUttaksplan, andrePartsUttak)

        Assertions.assertThat(erKvotenOversteget.first).isFalse()
        Assertions.assertThat(erKvotenOversteget.second).isEqualTo(BigDecimal.valueOf(46).setScale(2))
    }

    @Test
    internal fun `Brukt kvote skal bli 40 når søker og annen part har brukt opp 20 dager hver`() {
        val helePeriodenAnnenPart = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.JANUARY, 31))

        val annenPartsBehandlingUUID = nesteBehandlingUUID()
        val andrePartsUttak = mapOf(
                annenPartsBehandlingUUID to Uttaksplan(
                        perioder = mapOf(
                                helePeriodenAnnenPart to dummyUttaksperiodeInfo()
                        ),
                        trukketUttak = listOf()
                )
        )

        val periode1 = LukketPeriode("2020-01-06/2020-01-10")
        val periode2 = LukketPeriode("2020-01-13/2020-01-17")
        val periode3 = LukketPeriode("2020-01-20/2020-01-24")
        val periode4 = LukketPeriode("2020-01-27/2020-01-31")
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo(),
                periode2 to dummyUttaksperiodeInfo(),
                periode3 to dummyUttaksperiodeInfo(),
                periode4 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val erKvotenOversteget = BeregnBruktKvote.erKvotenOversteget(søkersUttaksplan, andrePartsUttak)

        Assertions.assertThat(erKvotenOversteget.first).isFalse()
        Assertions.assertThat(erKvotenOversteget.second).isEqualTo(BigDecimal.valueOf(40).setScale(2))
    }

    @Test
    internal fun `Brukt kvote skal bli 40 som testen over, fordi helg ikke teller med selvom uttaksplanen inneholder helg når kvoten blir regnet ut`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.JANUARY, 31))

        val annenPartsBehandlingUUID = nesteBehandlingUUID()
        val andrePartsUttak = mapOf(
                annenPartsBehandlingUUID to Uttaksplan(
                        perioder = mapOf(
                                helePerioden to dummyUttaksperiodeInfo()
                        ),
                        trukketUttak = listOf()
                )
        )

        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                helePerioden to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val erKvotenOversteget = BeregnBruktKvote.erKvotenOversteget(søkersUttaksplan, andrePartsUttak)

        Assertions.assertThat(erKvotenOversteget.first).isFalse()
        Assertions.assertThat(erKvotenOversteget.second).isEqualTo(BigDecimal.valueOf(40).setScale(2))
    }

    @Test
    internal fun `Kvoten er oversteget, og skal bli 80 når søker bruker 60 og annen part har brukt 20`() {
        val helePeriodenAnnenPart = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.JANUARY, 31)) //20 dager

        val annenPartsBehandlingUUID = nesteBehandlingUUID()
        val andrePartsUttak = mapOf(
                annenPartsBehandlingUUID to Uttaksplan(
                        perioder = mapOf(
                                helePeriodenAnnenPart to dummyUttaksperiodeInfo()
                        ),
                        trukketUttak = listOf()
                )
        )

        val periode1 = LukketPeriode("2020-01-06/2020-01-10") // 5
        val periode2 = LukketPeriode("2020-01-13/2020-01-17") // 10
        val periode3 = LukketPeriode("2020-01-20/2020-01-24") // 15
        val periode4 = LukketPeriode("2020-01-27/2020-01-31") // 20
        val periode5 = LukketPeriode("2020-02-03/2020-02-07") // 25
        val periode6 = LukketPeriode("2020-02-10/2020-02-14") // 30
        val periode7 = LukketPeriode("2020-02-17/2020-02-21") // 35
        val periode8 = LukketPeriode("2020-02-24/2020-02-28") // 40
        val periode9 = LukketPeriode("2020-03-02/2020-03-06") // 45
        val periode10 = LukketPeriode("2020-03-09/2020-03-13") // 50
        val periode11 = LukketPeriode("2020-03-16/2020-03-20") // 55
        val periode12 = LukketPeriode("2020-03-23/2020-03-27") // 60
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo(),
                periode2 to dummyUttaksperiodeInfo(),
                periode3 to dummyUttaksperiodeInfo(),
                periode4 to dummyUttaksperiodeInfo(),
                periode5 to dummyUttaksperiodeInfo(),
                periode6 to dummyUttaksperiodeInfo(),
                periode7 to dummyUttaksperiodeInfo(),
                periode8 to dummyUttaksperiodeInfo(),
                periode9 to dummyUttaksperiodeInfo(),
                periode10 to dummyUttaksperiodeInfo(),
                periode11 to dummyUttaksperiodeInfo(),
                periode12 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val erKvotenOversteget = BeregnBruktKvote.erKvotenOversteget(søkersUttaksplan, andrePartsUttak)

        Assertions.assertThat(erKvotenOversteget.first).isTrue()
        Assertions.assertThat(erKvotenOversteget.second).isEqualTo(BigDecimal.valueOf(80).setScale(2))
    }

    @Test
    internal fun `Kvoten er ikke oversteget når søker bruker 59 dager`() {

        val periode1 = LukketPeriode("2020-01-06/2020-01-10") // 5
        val periode2 = LukketPeriode("2020-01-13/2020-01-17") // 10
        val periode3 = LukketPeriode("2020-01-20/2020-01-24") // 15
        val periode4 = LukketPeriode("2020-01-27/2020-01-31") // 20
        val periode5 = LukketPeriode("2020-02-03/2020-02-07") // 25
        val periode6 = LukketPeriode("2020-02-10/2020-02-14") // 30
        val periode7 = LukketPeriode("2020-02-17/2020-02-21") // 35
        val periode8 = LukketPeriode("2020-02-24/2020-02-28") // 40
        val periode9 = LukketPeriode("2020-03-02/2020-03-06") // 45
        val periode10 = LukketPeriode("2020-03-09/2020-03-13") // 50
        val periode11 = LukketPeriode("2020-03-16/2020-03-20") // 55
        val periode12 = LukketPeriode("2020-03-23/2020-03-26") // 59
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo(),
                periode2 to dummyUttaksperiodeInfo(),
                periode3 to dummyUttaksperiodeInfo(),
                periode4 to dummyUttaksperiodeInfo(),
                periode5 to dummyUttaksperiodeInfo(),
                periode6 to dummyUttaksperiodeInfo(),
                periode7 to dummyUttaksperiodeInfo(),
                periode8 to dummyUttaksperiodeInfo(),
                periode9 to dummyUttaksperiodeInfo(),
                periode10 to dummyUttaksperiodeInfo(),
                periode11 to dummyUttaksperiodeInfo(),
                periode12 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val erKvotenOversteget = BeregnBruktKvote.erKvotenOversteget(søkersUttaksplan, emptyMap())

        Assertions.assertThat(erKvotenOversteget.first).isFalse()
        Assertions.assertThat(erKvotenOversteget.second).isEqualTo(BigDecimal.valueOf(59).setScale(2))
    }

    @Test
    internal fun `Kvoten er ikke oversteget når søker bruker akkuratt 60 dager`() {

        val periode1 = LukketPeriode("2020-01-06/2020-01-10") // 5
        val periode2 = LukketPeriode("2020-01-13/2020-01-17") // 10
        val periode3 = LukketPeriode("2020-01-20/2020-01-24") // 15
        val periode4 = LukketPeriode("2020-01-27/2020-01-31") // 20
        val periode5 = LukketPeriode("2020-02-03/2020-02-07") // 25
        val periode6 = LukketPeriode("2020-02-10/2020-02-14") // 30
        val periode7 = LukketPeriode("2020-02-17/2020-02-21") // 35
        val periode8 = LukketPeriode("2020-02-24/2020-02-28") // 40
        val periode9 = LukketPeriode("2020-03-02/2020-03-06") // 45
        val periode10 = LukketPeriode("2020-03-09/2020-03-13") // 50
        val periode11 = LukketPeriode("2020-03-16/2020-03-20") // 55
        val periode12 = LukketPeriode("2020-03-23/2020-03-27") // 60
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo(),
                periode2 to dummyUttaksperiodeInfo(),
                periode3 to dummyUttaksperiodeInfo(),
                periode4 to dummyUttaksperiodeInfo(),
                periode5 to dummyUttaksperiodeInfo(),
                periode6 to dummyUttaksperiodeInfo(),
                periode7 to dummyUttaksperiodeInfo(),
                periode8 to dummyUttaksperiodeInfo(),
                periode9 to dummyUttaksperiodeInfo(),
                periode10 to dummyUttaksperiodeInfo(),
                periode11 to dummyUttaksperiodeInfo(),
                periode12 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val erKvotenOversteget = BeregnBruktKvote.erKvotenOversteget(søkersUttaksplan, emptyMap())

        Assertions.assertThat(erKvotenOversteget.first).isFalse()
        Assertions.assertThat(erKvotenOversteget.second).isEqualTo(BigDecimal.valueOf(60).setScale(2))
    }

    @Test
    internal fun `Kvoten er oversteget når søker bruker kun 1 dag mer enn kvoten`() {

        val periode1 = LukketPeriode("2020-01-06/2020-01-10") // 5
        val periode2 = LukketPeriode("2020-01-13/2020-01-17") // 10
        val periode3 = LukketPeriode("2020-01-20/2020-01-24") // 15
        val periode4 = LukketPeriode("2020-01-27/2020-01-31") // 20
        val periode5 = LukketPeriode("2020-02-03/2020-02-07") // 25
        val periode6 = LukketPeriode("2020-02-10/2020-02-14") // 30
        val periode7 = LukketPeriode("2020-02-17/2020-02-21") // 35
        val periode8 = LukketPeriode("2020-02-24/2020-02-28") // 40
        val periode9 = LukketPeriode("2020-03-02/2020-03-06") // 45
        val periode10 = LukketPeriode("2020-03-09/2020-03-13") // 50
        val periode11 = LukketPeriode("2020-03-16/2020-03-20") // 55
        val periode12 = LukketPeriode("2020-03-23/2020-03-27") // 60
        val periode13 = LukketPeriode("2020-03-30/2020-03-30") // 61
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo(),
                periode2 to dummyUttaksperiodeInfo(),
                periode3 to dummyUttaksperiodeInfo(),
                periode4 to dummyUttaksperiodeInfo(),
                periode5 to dummyUttaksperiodeInfo(),
                periode6 to dummyUttaksperiodeInfo(),
                periode7 to dummyUttaksperiodeInfo(),
                periode8 to dummyUttaksperiodeInfo(),
                periode9 to dummyUttaksperiodeInfo(),
                periode10 to dummyUttaksperiodeInfo(),
                periode11 to dummyUttaksperiodeInfo(),
                periode12 to dummyUttaksperiodeInfo(),
                periode13 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val erKvotenOversteget = BeregnBruktKvote.erKvotenOversteget(søkersUttaksplan, emptyMap())

        Assertions.assertThat(erKvotenOversteget.first).isTrue()
        Assertions.assertThat(erKvotenOversteget.second).isEqualTo(BigDecimal.valueOf(61).setScale(2))
    }

    @Test
    internal fun `Kvoten er oversteget når søker bruker kun en halvtime mer enn kvoten`() {

        val periode1 = LukketPeriode("2020-01-06/2020-01-10") // 5
        val periode2 = LukketPeriode("2020-01-13/2020-01-17") // 10
        val periode3 = LukketPeriode("2020-01-20/2020-01-24") // 15
        val periode4 = LukketPeriode("2020-01-27/2020-01-31") // 20
        val periode5 = LukketPeriode("2020-02-03/2020-02-07") // 25
        val periode6 = LukketPeriode("2020-02-10/2020-02-14") // 30
        val periode7 = LukketPeriode("2020-02-17/2020-02-21") // 35
        val periode8 = LukketPeriode("2020-02-24/2020-02-28") // 40
        val periode9 = LukketPeriode("2020-03-02/2020-03-06") // 45
        val periode10 = LukketPeriode("2020-03-09/2020-03-13") // 50
        val periode11 = LukketPeriode("2020-03-16/2020-03-20") // 55
        val periode12 = LukketPeriode("2020-03-23/2020-03-27") // 60
        val periode13 = LukketPeriode("2020-03-30/2020-03-30") // 60 og en halvtime
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo(),
                periode2 to dummyUttaksperiodeInfo(),
                periode3 to dummyUttaksperiodeInfo(),
                periode4 to dummyUttaksperiodeInfo(),
                periode5 to dummyUttaksperiodeInfo(),
                periode6 to dummyUttaksperiodeInfo(),
                periode7 to dummyUttaksperiodeInfo(),
                periode8 to dummyUttaksperiodeInfo(),
                periode9 to dummyUttaksperiodeInfo(),
                periode10 to dummyUttaksperiodeInfo(),
                periode11 to dummyUttaksperiodeInfo(),
                periode12 to dummyUttaksperiodeInfo(),
                periode13 to dummyUttaksperiodeInfo(Duration.ofMinutes(30))
        ), trukketUttak = listOf())

        val erKvotenOversteget = BeregnBruktKvote.erKvotenOversteget(søkersUttaksplan, emptyMap())

        Assertions.assertThat(erKvotenOversteget.first).isTrue()
        Assertions.assertThat(erKvotenOversteget.second).isEqualTo(BigDecimal.valueOf(60.07))
    }

}

private fun nesteBehandlingUUID() = UUID.randomUUID()

private fun dummyUttaksperiodeInfo(oppgittTilsyn: Duration? = null) = UttaksperiodeInfo(
        utfall = Utfall.OPPFYLT,
        utbetalingsgrader = listOf(),
        annenPart = AnnenPart.ALENE,
        beredskap = null,
        nattevåk = null,
        graderingMotTilsyn = GraderingMotTilsyn(
                etablertTilsyn = NULL_PROSENT,
                overseEtablertTilsynÅrsak = null,
                andreSøkeresTilsyn = NULL_PROSENT,
                tilgjengeligForSøker = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false
        ),
        kildeBehandlingUUID = "123",
        oppgittTilsyn = oppgittTilsyn,
        pleiebehov = Pleiebehov.PROSENT_100.prosent,
        søkersTapteArbeidstid = null,
        uttaksgrad = HUNDRE_PROSENT,
        årsaker = setOf(),
    uttaksgradUtenReduksjonGrunnetInntektsgradering = null,
    uttaksgradMedReduksjonGrunnetInntektsgradering = null,
)
