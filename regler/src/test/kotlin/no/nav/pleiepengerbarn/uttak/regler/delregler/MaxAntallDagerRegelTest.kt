package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.somArbeid
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.util.*

class MaxAntallDagerRegelTest {
    private val regel: MaxAntallDagerRegel = MaxAntallDagerRegel()

    @Test
    internal fun `Søker får fraværet innvilget fordi det er mindre enn max antall dager`() {
        val periode1 = LukketPeriode("2020-01-06/2020-03-20")
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 20))
        val grunnlag = dummyRegelGrunnlag(helePerioden)

        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        Assertions.assertThat(resultat.perioder).hasSize(1)

        val resultatPeriode = resultat.perioder.keys.first()
        val resultatInfo = resultat.perioder.values.first()
        Assertions.assertThat(resultatPeriode).isEqualTo(periode1)
        Assertions.assertThat(resultatInfo.utfall).isEqualTo(Utfall.OPPFYLT)
    }

    @Test
    internal fun `Søker får innvilget det fraværet som er mindre enn max antall dager, deretter avslag`() {
        val periode1 = LukketPeriode("2020-01-06/2020-04-03")
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.APRIL, 3))
        val grunnlag = dummyRegelGrunnlag(helePerioden)

        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        Assertions.assertThat(resultat.perioder).hasSize(2)

        val resultatPeriode = resultat.perioder.keys.first()
        val resultatInfo = resultat.perioder.values.first()
        Assertions.assertThat(resultatPeriode).isEqualTo(LukketPeriode("2020-01-06/2020-03-27"))
        Assertions.assertThat(resultatInfo.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode2 = resultat.perioder.keys.last()
        val resultatInfo2 = resultat.perioder.values.last()
        Assertions.assertThat(resultatPeriode2).isEqualTo(LukketPeriode("2020-03-28/2020-04-03"))
        Assertions.assertThat(resultatInfo2.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    internal fun `Andre parter har brukt opp alle dagene, men søker får fraværet innvilget fordi det er før datoen dagene ble brukt opp`() {
        val periode1 = LukketPeriode("2020-01-06/2020-01-10")
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val annenPartsBehandlingUUID = nesteBehandlingUUID()

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 13), LocalDate.of(2020, Month.APRIL, 3))
        val grunnlag = dummyRegelGrunnlagMedAndreParter(helePerioden, annenPartsBehandlingUUID)

        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        Assertions.assertThat(resultat.perioder).hasSize(1)

        val resultatPeriode = resultat.perioder.keys.first()
        val resultatInfo = resultat.perioder.values.first()
        Assertions.assertThat(resultatPeriode).isEqualTo(periode1)
        Assertions.assertThat(resultatInfo.utfall).isEqualTo(Utfall.OPPFYLT)
    }

    @Test
    internal fun `Andre parter har brukt opp alle dagene, så søker får innvilget det fraværet som sammenfaller i tid`() {
        val periode1 = LukketPeriode("2020-03-25/2020-03-30")
        val periode2 = LukketPeriode("2020-03-31/2020-04-03")
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo(),
                periode2 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val annenPartsBehandlingUUID = nesteBehandlingUUID()

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 27)) // 60 dager
        val grunnlag = dummyRegelGrunnlagMedAndreParter(helePerioden, annenPartsBehandlingUUID)


        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        Assertions.assertThat(resultat.perioder).hasSize(3)

        val resultatPeriode1 = resultat.perioder.keys.first()
        val resultatInfo1 = resultat.perioder.values.first()
        Assertions.assertThat(resultatPeriode1).isEqualTo(LukketPeriode("2020-03-25/2020-03-27"))
        Assertions.assertThat(resultatInfo1.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode2 = resultat.perioder.keys.toList()[1]
        val resultatInfo2 = resultat.perioder.values.toList()[1]
        Assertions.assertThat(resultatPeriode2).isEqualTo(LukketPeriode("2020-03-28/2020-03-30"))
        Assertions.assertThat(resultatInfo2.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)

        val resultatPeriode3 = resultat.perioder.keys.last()
        val resultatInfo3 = resultat.perioder.values.last()
        Assertions.assertThat(resultatPeriode3).isEqualTo(LukketPeriode("2020-03-31/2020-04-03"))
        Assertions.assertThat(resultatInfo3.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    internal fun `Andre parter har brukt opp alle dagene, så søker får innvilget det fraværet som sammenfaller i tid 2`() {
        val periode1 = LukketPeriode("2020-02-24/2020-04-24")
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val annenPartsBehandlingUUID = nesteBehandlingUUID()

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 27)) // 60 dager
        val grunnlag = dummyRegelGrunnlagMedAndreParter(helePerioden, annenPartsBehandlingUUID)


        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        Assertions.assertThat(resultat.perioder).hasSize(2)

        val resultatPeriode1 = resultat.perioder.keys.first()
        val resultatInfo1 = resultat.perioder.values.first()
        Assertions.assertThat(resultatPeriode1).isEqualTo(LukketPeriode("2020-02-24/2020-03-27"))
        Assertions.assertThat(resultatInfo1.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode2 = resultat.perioder.keys.last()
        val resultatInfo2 = resultat.perioder.values.last()
        Assertions.assertThat(resultatPeriode2).isEqualTo(LukketPeriode("2020-03-28/2020-04-24"))
        Assertions.assertThat(resultatInfo2.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    internal fun `Andre parter har brukt opp en del dager, så søker får innvilget det som er igjen av kvoten, så avslag på det som ikke sammenfaller i tid`() {
        val periode1 = LukketPeriode("2020-03-16/2020-03-27") // 10
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val annenPartsBehandlingUUID = nesteBehandlingUUID()

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 20)) // 55 dager
        val grunnlag = dummyRegelGrunnlagMedAndreParter(helePerioden, annenPartsBehandlingUUID)

        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        Assertions.assertThat(resultat.perioder).hasSize(2)

        val resultatPeriode1 = resultat.perioder.keys.first()
        val resultatInfo1 = resultat.perioder.values.first()
        Assertions.assertThat(resultatPeriode1).isEqualTo(LukketPeriode("2020-03-16/2020-03-20"))
        Assertions.assertThat(resultatInfo1.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode2 = resultat.perioder.keys.last()
        val resultatInfo2 = resultat.perioder.values.last()
        Assertions.assertThat(resultatPeriode2).isEqualTo(LukketPeriode("2020-03-21/2020-03-27"))
        Assertions.assertThat(resultatInfo2.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    internal fun `Andre parter har brukt opp en del dager, så søker får innvilget det som er igjen av kvoten og det som sammenfaller i tid`() {
        val periode1 = LukketPeriode("2020-03-09/2020-03-27") // 15, 5 dager kvote, 5 dager sammenfaller i tid, 5 dager avslått
        val søkersUttaksplan = Uttaksplan(perioder = mapOf(
                periode1 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val annenPartsBehandlingUUID = nesteBehandlingUUID()

        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 6), LocalDate.of(2020, Month.MARCH, 20)) // 55 dager
        val grunnlag = dummyRegelGrunnlagMedAndreParter(helePerioden, annenPartsBehandlingUUID)


        val resultat = regel.kjør(søkersUttaksplan, grunnlag)
        Assertions.assertThat(resultat.perioder).hasSize(3)

        val resultatPeriode1 = resultat.perioder.keys.first()
        val resultatInfo1 = resultat.perioder.values.first()
        Assertions.assertThat(resultatPeriode1).isEqualTo(LukketPeriode("2020-03-09/2020-03-13"))
        Assertions.assertThat(resultatInfo1.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode2 = resultat.perioder.keys.toList()[1]
        val resultatInfo2 = resultat.perioder.values.toList()[1]
        Assertions.assertThat(resultatPeriode2).isEqualTo(LukketPeriode("2020-03-14/2020-03-20"))
        Assertions.assertThat(resultatInfo2.utfall).isEqualTo(Utfall.OPPFYLT)

        val resultatPeriode3 = resultat.perioder.keys.last()
        val resultatInfo3 = resultat.perioder.values.last()
        Assertions.assertThat(resultatPeriode3).isEqualTo(LukketPeriode("2020-03-21/2020-03-27"))
        Assertions.assertThat(resultatInfo3.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
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
                helePerioden to Pleiebehov.PROSENT_6000
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

private fun dummyRegelGrunnlagMedAndreParter(helePerioden: LukketPeriode, annenPartsBehandlingUUID: UUID = nesteBehandlingUUID()) = RegelGrunnlag(
        ytelseType = YtelseType.PLS,
        saksnummer = nesteSaksnummer(),
        søker = Søker(
                aktørId = "123"
        ),
        barn = Barn(
                aktørId = "456"
        ),
        pleiebehov = mapOf(
                helePerioden to Pleiebehov.PROSENT_6000
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
                                        pleiebehov = Pleiebehov.PROSENT_6000.prosent,
                                        knekkpunktTyper = setOf(),
                                        annenPart = AnnenPart.ALENE,
                                        nattevåk = null,
                                        beredskap = null
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


private fun dummyUttaksperiodeInfo(oppgittTilsyn: Duration? = null) = UttaksperiodeInfo(
        utfall = Utfall.OPPFYLT,
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