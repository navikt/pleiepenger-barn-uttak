package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.util.*

class MaxAntallDagerRegelTest {
    private val regel: MaxAntallDagerRegel = MaxAntallDagerRegel()

    @BeforeEach
    internal fun setUp() {
        System.setProperty("GIR_ALDRI_MER_ENN_60_DAGER", "true")
    }

    @AfterEach
    internal fun tearDown() {
        System.clearProperty("GIR_ALDRI_MER_ENN_60_DAGER")
    }
    @Test
    internal fun `Pleiepenger sykt barn skal få innvilget uavhengig av kvote, og kvoteInfo skal derfor ikke være satt`() {
        val periode1 = LukketPeriode("2020-01-06/2020-04-03") // 65 dager
        val søkersUttaksplan = Uttaksplan(
            perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
            ), trukketUttak = listOf()
        )

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 20))
        val grunnlag = dummyRegelGrunnlag(helePerioden).copy(ytelseType = YtelseType.PSB)

        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        assertThat(resultat.perioder).hasSize(1)
        assertThat(resultat.kvoteInfo).isNull()

        val resultatPeriode = resultat.perioder.keys.first()
        val resultatInfo = resultat.perioder.values.first()
        assertThat(resultatPeriode).isEqualTo(periode1)
        assertThat(resultatInfo.utfall).isEqualTo(Utfall.OPPFYLT)
    }

    @Test
    internal fun `Søker får fraværet innvilget fordi det er mindre enn max antall dager`() {
        val periode1 = LukketPeriode("2020-01-06/2020-03-20")
        val søkersUttaksplan = Uttaksplan(
            perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
            ), trukketUttak = listOf()
        )

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 20))
        val grunnlag = dummyRegelGrunnlag(helePerioden)

        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        assertThat(resultat.perioder).hasSize(1)
        assertThat(resultat.kvoteInfo).isNotNull
        assertThat(resultat.kvoteInfo!!.maxDato).isNull()
        assertThat(resultat.kvoteInfo!!.totaltForbruktKvote).isEqualTo(BigDecimal.valueOf(55).setScale(2))

        val resultatPeriode = resultat.perioder.keys.first()
        val resultatInfo = resultat.perioder.values.first()
        assertThat(resultatPeriode).isEqualTo(periode1)
        assertThat(resultatInfo.utfall).isEqualTo(Utfall.OPPFYLT)
    }

    @Test
    internal fun `Søker får innvilget det fraværet som er mindre enn max antall dager, deretter avslag`() {
        val periode1 = LukketPeriode("2020-01-06/2020-04-03")
        val søkersUttaksplan = Uttaksplan(
            perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
            ), trukketUttak = listOf()
        )

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.APRIL, 3))
        val grunnlag = dummyRegelGrunnlag(helePerioden)

        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        assertThat(resultat.perioder).hasSize(2)
        assertThat(resultat.kvoteInfo).isNotNull
        assertThat(resultat.kvoteInfo!!.maxDato).isNull()
        assertThat(resultat.kvoteInfo!!.totaltForbruktKvote).isEqualTo(BigDecimal.valueOf(60).setScale(2))

        val resultatPeriode = resultat.perioder.keys.first()
        val resultatInfo = resultat.perioder.values.first()
        assertThat(resultatPeriode).isEqualTo(LukketPeriode("2020-01-06/2020-03-27"))
        assertThat(resultatInfo.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode2 = resultat.perioder.keys.last()
        val resultatInfo2 = resultat.perioder.values.last()
        assertThat(resultatPeriode2).isEqualTo(LukketPeriode("2020-03-28/2020-04-03"))
        assertThat(resultatInfo2.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    internal fun `Søker får avslått det fraværet som ikke er oppfylt av andre grunner, innvilget det som er innenfor kvoten, og avslått det som er over kvoten`() {
        val periode1 = LukketPeriode("2020-01-06/2020-01-31") // 20 dager
        val periode2 = LukketPeriode("2020-02-03/2020-02-07") // 5 dager ikke oppfylt
        val periode3 = LukketPeriode("2020-02-10/2020-04-10") // 40 dager
        val søkersUttaksplan = Uttaksplan(
            perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo(utfall = Utfall.OPPFYLT),
                periode2 to dummyUttaksperiodeInfo(utfall = Utfall.IKKE_OPPFYLT),
                periode3 to dummyUttaksperiodeInfo(utfall = Utfall.OPPFYLT)
            ), trukketUttak = listOf()
        )

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.APRIL, 10))
        val grunnlag = dummyRegelGrunnlag(helePerioden)

        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        assertThat(resultat.perioder).hasSize(4)
        assertThat(resultat.kvoteInfo).isNotNull
        assertThat(resultat.kvoteInfo!!.maxDato).isNull()
        assertThat(resultat.kvoteInfo!!.totaltForbruktKvote).isEqualTo(BigDecimal.valueOf(60).setScale(2))

        val resultatPeriode = resultat.perioder.keys.first()
        val resultatInfo = resultat.perioder.values.first()
        assertThat(resultatPeriode).isEqualTo(LukketPeriode("2020-01-06/2020-01-31"))
        assertThat(resultatInfo.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode2 = resultat.perioder.keys.toList()[1]
        val resultatInfo2 = resultat.perioder.values.toList()[1]
        assertThat(resultatPeriode2).isEqualTo(LukketPeriode("2020-02-03/2020-02-07"))
        assertThat(resultatInfo2.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)

        val resultatPeriode3 = resultat.perioder.keys.toList()[2]
        val resultatInfo3 = resultat.perioder.values.toList()[2]
        assertThat(resultatPeriode3).isEqualTo(LukketPeriode("2020-02-10/2020-04-03"))
        assertThat(resultatInfo3.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode4 = resultat.perioder.keys.last()
        val resultatInfo4 = resultat.perioder.values.last()
        assertThat(resultatPeriode4).isEqualTo(LukketPeriode("2020-04-04/2020-04-10"))
        assertThat(resultatInfo4.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    internal fun `Andre parter har brukt opp alle dagene, så søker får fraværet avslått fordi vi ikke lenger innvilger over 60 dager`() {
        val periode1 = LukketPeriode("2020-01-06/2020-01-10")
        val søkersUttaksplan = Uttaksplan(
            perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
            ), trukketUttak = listOf()
        )

        val annenPartsBehandlingUUID = nesteBehandlingUUID()

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 13), LocalDate.of(2020, Month.APRIL, 3))
        val grunnlag = dummyRegelGrunnlagMedAndreParter(helePerioden, annenPartsBehandlingUUID)

        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        assertThat(resultat.perioder).hasSize(1)
        assertThat(resultat.kvoteInfo).isNotNull
        assertThat(resultat.kvoteInfo!!.maxDato).isEqualTo(LocalDate.of(2020, 4, 3))
        assertThat(resultat.kvoteInfo!!.totaltForbruktKvote).isEqualTo(BigDecimal.valueOf(60).setScale(2))

        val resultatPeriode = resultat.perioder.keys.first()
        val resultatInfo = resultat.perioder.values.first()
        assertThat(resultatPeriode).isEqualTo(periode1)
        assertThat(resultatInfo.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    internal fun `Andre parter har fått avslått dager, derfor ikke brukt opp alle dagene, så søker får innvilget det som er igjen på kvote, deretter avslått fordi vi ikke lenger innvilger det som sammenfaller i tid`() {
        val periode1 = LukketPeriode("2020-02-17/2020-04-10")
        val søkersUttaksplan = Uttaksplan(
            perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
            ), trukketUttak = listOf()
        )

        val annenPartsBehandlingUUID = nesteBehandlingUUID()

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 27))
        val annenPeriode1 = LukketPeriode("2020-01-06/2020-02-07") // 25
        val annenPeriode2 = LukketPeriode("2020-02-10/2020-02-14") // 5 dager ikke oppfylt
        val annenPeriode3 = LukketPeriode("2020-02-17/2020-03-27") // 30 dager
        val grunnlag = RegelGrunnlag(
            ytelseType = YtelseType.PLS,
            saksnummer = nesteSaksnummer(),
            søker = Søker(
                aktørId = "123"
            ),
            barn = Barn(
                aktørId = "456"
            ),
            pleiebehov = mapOf(
                helePerioden to Pleiebehov.PROSENT_100
            ),
            søktUttak = listOf(
                SøktUttak(helePerioden)
            ),
            andrePartersUttaksplanPerBehandling = mapOf(
                annenPartsBehandlingUUID to Uttaksplan(
                    perioder = mapOf(
                        annenPeriode1 to UttaksperiodeInfo.oppfylt(
                            kildeBehandlingUUID = annenPartsBehandlingUUID.toString(),
                            uttaksgrad = Prosent(100),
                            utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(100)).somUtbetalingsgrader(),
                            søkersTapteArbeidstid = Prosent(100),
                            oppgittTilsyn = null,
                            årsak = Årsak.FULL_DEKNING,
                            pleiebehov = Pleiebehov.PROSENT_100.prosent,
                            knekkpunktTyper = setOf(),
                            annenPart = AnnenPart.ALENE,
                            nattevåk = null,
                            beredskap = null,
                            utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN)
                        ),
                        annenPeriode2 to UttaksperiodeInfo.ikkeOppfylt(
                            kildeBehandlingUUID = annenPartsBehandlingUUID.toString(),
                            utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(100)).somUtbetalingsgrader(),
                            søkersTapteArbeidstid = Prosent(100),
                            oppgittTilsyn = null,
                            pleiebehov = Pleiebehov.PROSENT_100.prosent,
                            knekkpunktTyper = setOf(),
                            annenPart = AnnenPart.ALENE,
                            nattevåk = null,
                            beredskap = null,
                            årsaker = setOf(Årsak.FOR_LAV_INNTEKT),
                            utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN)
                        ),
                        annenPeriode3 to UttaksperiodeInfo.oppfylt(
                            kildeBehandlingUUID = annenPartsBehandlingUUID.toString(),
                            uttaksgrad = Prosent(100),
                            utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(100)).somUtbetalingsgrader(),
                            søkersTapteArbeidstid = Prosent(100),
                            oppgittTilsyn = null,
                            årsak = Årsak.FULL_DEKNING,
                            pleiebehov = Pleiebehov.PROSENT_100.prosent,
                            knekkpunktTyper = setOf(),
                            annenPart = AnnenPart.ALENE,
                            nattevåk = null,
                            beredskap = null,
                            utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN)
                        )

                    ),
                    trukketUttak = listOf()
                )
            ),
            tilsynsperioder = emptyMap(),
            behandlingUUID = nesteBehandlingUUID(),
            arbeid = mapOf(
                arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, Duration.ZERO))
            ).somArbeid(),
            kravprioritetForBehandlinger = mapOf(helePerioden to listOf(annenPartsBehandlingUUID))
        )

        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        assertThat(resultat.perioder).hasSize(2)
        assertThat(resultat.kvoteInfo).isNotNull
        assertThat(resultat.kvoteInfo!!.maxDato).isNull()
        assertThat(resultat.kvoteInfo!!.totaltForbruktKvote).isEqualTo(BigDecimal.valueOf(60).setScale(2))

        val resultatPeriode1 = resultat.perioder.keys.first()
        val resultatInfo1 = resultat.perioder.values.first()
        assertThat(resultatPeriode1).isEqualTo(LukketPeriode("2020-02-17/2020-02-21")) // resterende 5 dagene på kvoten
        assertThat(resultatInfo1.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode2 = resultat.perioder.keys.last()
        val resultatInfo2 = resultat.perioder.values.last()
        assertThat(resultatPeriode2).isEqualTo(LukketPeriode("2020-02-22/2020-04-10")) // avslag fordi max kvote
        assertThat(resultatInfo2.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    internal fun `Andre parter har brukt opp alle dagene, så søker får avslag fordi vi ikke lenger innvilger det som sammenfaller i tid `() {
        val periode1 = LukketPeriode("2020-03-25/2020-03-30")
        val periode2 = LukketPeriode("2020-03-31/2020-04-03")
        val søkersUttaksplan = Uttaksplan(
            perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo(),
                periode2 to dummyUttaksperiodeInfo()
            ), trukketUttak = listOf()
        )

        val annenPartsBehandlingUUID = nesteBehandlingUUID()

        val helePerioden =
            LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 27)) // 60 dager
        val grunnlag = dummyRegelGrunnlagMedAndreParter(helePerioden, annenPartsBehandlingUUID)


        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        assertThat(resultat.perioder).hasSize(2)
        assertThat(resultat.kvoteInfo).isNotNull
        assertThat(resultat.kvoteInfo!!.maxDato).isEqualTo(LocalDate.of(2020, 3, 27))
        assertThat(resultat.kvoteInfo!!.totaltForbruktKvote).isEqualTo(BigDecimal.valueOf(60).setScale(2))

        val resultatPeriode1 = resultat.perioder.keys.first()
        val resultatInfo1 = resultat.perioder.values.first()
        assertThat(resultatPeriode1).isEqualTo(LukketPeriode("2020-03-25/2020-03-30"))
        assertThat(resultatInfo1.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)

        val resultatPeriode2 = resultat.perioder.keys.last()
        val resultatInfo2 = resultat.perioder.values.last()
        assertThat(resultatPeriode2).isEqualTo(LukketPeriode("2020-03-31/2020-04-03"))
        assertThat(resultatInfo2.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    internal fun `Andre parter har brukt opp alle dagene, så søker får avslått alt fordi vi ikke lenger innvilget det som sammenfaller i tid`() {
        val periode1 = LukketPeriode("2020-02-24/2020-04-24")
        val søkersUttaksplan = Uttaksplan(
            perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
            ), trukketUttak = listOf()
        )

        val annenPartsBehandlingUUID = nesteBehandlingUUID()

        val helePerioden =
            LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 27)) // 60 dager
        val grunnlag = dummyRegelGrunnlagMedAndreParter(helePerioden, annenPartsBehandlingUUID)


        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        assertThat(resultat.perioder).hasSize(1)
        assertThat(resultat.kvoteInfo).isNotNull
        assertThat(resultat.kvoteInfo!!.maxDato).isEqualTo(LocalDate.of(2020, 3, 27))
        assertThat(resultat.kvoteInfo!!.totaltForbruktKvote).isEqualTo(BigDecimal.valueOf(60).setScale(2))

        val resultatPeriode1 = resultat.perioder.keys.first()
        val resultatInfo1 = resultat.perioder.values.first()
        assertThat(resultatPeriode1).isEqualTo(LukketPeriode("2020-02-24/2020-04-24"))
        assertThat(resultatInfo1.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    internal fun `Andre parter har brukt opp en del dager, så søker får innvilget det som er igjen av kvoten, så avslag på det som ikke sammenfaller i tid`() {
        val periode1 = LukketPeriode("2020-03-16/2020-03-27") // 10
        val søkersUttaksplan = Uttaksplan(
            perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
            ), trukketUttak = listOf()
        )

        val annenPartsBehandlingUUID = nesteBehandlingUUID()

        val helePerioden =
            LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 20)) // 55 dager
        val grunnlag = dummyRegelGrunnlagMedAndreParter(helePerioden, annenPartsBehandlingUUID)

        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        assertThat(resultat.perioder).hasSize(2)
        assertThat(resultat.kvoteInfo).isNotNull
        assertThat(resultat.kvoteInfo!!.maxDato).isNull()
        assertThat(resultat.kvoteInfo!!.totaltForbruktKvote).isEqualTo(BigDecimal.valueOf(60).setScale(2))

        val resultatPeriode1 = resultat.perioder.keys.first()
        val resultatInfo1 = resultat.perioder.values.first()
        assertThat(resultatPeriode1).isEqualTo(LukketPeriode("2020-03-16/2020-03-20"))
        assertThat(resultatInfo1.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode2 = resultat.perioder.keys.last()
        val resultatInfo2 = resultat.perioder.values.last()
        assertThat(resultatPeriode2).isEqualTo(LukketPeriode("2020-03-21/2020-03-27"))
        assertThat(resultatInfo2.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    internal fun `Andre parter har brukt opp en del dager, så søker får innvilget det som er igjen av kvoten og avslått resten fordi vi ikke lenger innvilger det som sammenfaller i tid`() {
        val periode1 =
            LukketPeriode("2020-03-09/2020-03-27") // 15, 5 dager kvote, 5 dager sammenfaller i tid, 5 dager avslått
        val søkersUttaksplan = Uttaksplan(
            perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
            ), trukketUttak = listOf()
        )

        val annenPartsBehandlingUUID = nesteBehandlingUUID()

        val helePerioden =
            LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 20)) // 55 dager
        val grunnlag = dummyRegelGrunnlagMedAndreParter(helePerioden, annenPartsBehandlingUUID)


        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        assertThat(resultat.perioder).hasSize(2)
        assertThat(resultat.kvoteInfo).isNotNull
        assertThat(resultat.kvoteInfo!!.maxDato).isNull()
        assertThat(resultat.kvoteInfo!!.totaltForbruktKvote).isEqualTo(BigDecimal.valueOf(60).setScale(2))

        val resultatPeriode1 = resultat.perioder.keys.first()
        val resultatInfo1 = resultat.perioder.values.first()
        assertThat(resultatPeriode1).isEqualTo(LukketPeriode("2020-03-09/2020-03-13"))
        assertThat(resultatInfo1.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode2 = resultat.perioder.keys.last()
        val resultatInfo2 = resultat.perioder.values.last()
        assertThat(resultatPeriode2).isEqualTo(LukketPeriode("2020-03-14/2020-03-27"))
        assertThat(resultatInfo2.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

}

private fun nesteSaksnummer(): Saksnummer = UUID.randomUUID().toString().takeLast(19)
private fun nesteBehandlingUUID() = UUID.randomUUID()

private val arbeidsforhold1 = UUID.randomUUID().toString()

private fun dummyRegelGrunnlag(helePerioden: LukketPeriode) = RegelGrunnlag(
    ytelseType = YtelseType.PLS,
    saksnummer = nesteSaksnummer(),
    søker = Søker(
        aktørId = "123"
    ),
    barn = Barn(
        aktørId = "456"
    ),
    pleiebehov = mapOf(
        helePerioden to Pleiebehov.PROSENT_100
    ),
    søktUttak = listOf(
        SøktUttak(helePerioden)
    ),
    tilsynsperioder = emptyMap(),
    behandlingUUID = nesteBehandlingUUID(),
    arbeid = mapOf(
        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, Duration.ZERO))
    ).somArbeid()
)

private fun dummyRegelGrunnlagMedAndreParter(
    helePerioden: LukketPeriode,
    annenPartsBehandlingUUID: UUID = nesteBehandlingUUID()
) = RegelGrunnlag(
    ytelseType = YtelseType.PLS,
    saksnummer = nesteSaksnummer(),
    søker = Søker(
        aktørId = "123"
    ),
    barn = Barn(
        aktørId = "456"
    ),
    pleiebehov = mapOf(
        helePerioden to Pleiebehov.PROSENT_100
    ),
    søktUttak = listOf(
        SøktUttak(helePerioden)
    ),
    andrePartersUttaksplanPerBehandling = mapOf(
        annenPartsBehandlingUUID to Uttaksplan(
            perioder = mapOf(
                helePerioden to UttaksperiodeInfo.oppfylt(
                    kildeBehandlingUUID = annenPartsBehandlingUUID.toString(),
                    uttaksgrad = Prosent(100),
                    utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(100)).somUtbetalingsgrader(),
                    søkersTapteArbeidstid = Prosent(100),
                    oppgittTilsyn = null,
                    årsak = Årsak.FULL_DEKNING,
                    pleiebehov = Pleiebehov.PROSENT_100.prosent,
                    knekkpunktTyper = setOf(),
                    annenPart = AnnenPart.ALENE,
                    nattevåk = null,
                    beredskap = null,
                    utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN)
                )
            ),
            trukketUttak = listOf()
        )
    ),
    tilsynsperioder = emptyMap(),
    behandlingUUID = nesteBehandlingUUID(),
    arbeid = mapOf(
        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, Duration.ZERO))
    ).somArbeid(),
    kravprioritetForBehandlinger = mapOf(helePerioden to listOf(annenPartsBehandlingUUID))
)


private fun dummyUttaksperiodeInfo(oppgittTilsyn: Duration? = null, utfall: Utfall = Utfall.OPPFYLT) =
    UttaksperiodeInfo(
        utfall = utfall,
        utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(100)).somUtbetalingsgrader(),
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
        årsaker = setOf()
    )
