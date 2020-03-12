package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkAvslått
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.Month

internal class UttakTjenesteTest {

    private companion object {val FULL_UKE = Duration.ofHours(37).plusMinutes(30)}

    private val arbeidsforhold1 = java.util.UUID.randomUUID().toString()

    @Test
    fun `Enkel uttaksperiode uten annen informasjon`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        fødselsdato = LocalDate.now().minusYears(20)
                ),
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_200)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent.ZERO))
                ).somArbeid()
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), InnvilgetÅrsaker.FULL_DEKNING)
    }


    @Test
    fun `En uttaksperiode som delvis overlapper med ferie`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        fødselsdato = LocalDate.now().minusYears(20)
                ),
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_200)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                lovbestemtFerie = listOf(
                        LukketPeriode(LocalDate.of(2020, Month.JANUARY, 15), LocalDate.of(2020, Month.FEBRUARY, 15))
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent.ZERO))
                ).somArbeid()
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkInnvilget(uttaksplan, helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 14)), Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), InnvilgetÅrsaker.FULL_DEKNING)
        sjekkAvslått(uttaksplan, helePerioden.copy(fom = LocalDate.of(2020, Month.JANUARY, 15)), setOf(AvslåttÅrsaker.LOVBESTEMT_FERIE))
    }

    @Test
    fun `En uttaksperiode som fortsetter etter slutt på tilsynsbehov perioden, skal avslås fra slutt på tilsynsbehov perioden`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        fødselsdato = LocalDate.now().minusYears(20)
                ),
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_200)
                ),
                søknadsperioder = listOf(
                        LukketPeriode(helePerioden.fom, helePerioden.tom.plusDays(7))
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent.ZERO))
                ).somArbeid()
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), InnvilgetÅrsaker.FULL_DEKNING)
        sjekkAvslått(uttaksplan, LukketPeriode(helePerioden.tom.plusDays(1), helePerioden.tom.plusDays(7)), setOf(AvslåttÅrsaker.UTENOM_TILSYNSBEHOV))
    }

    @Test
    fun `En uttaksperiode som overlapper med tilsyn slik at uttaksgraden blir under 20 prosent, skal avslås pga for høy tilsynsgrad`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        fødselsdato = LocalDate.now().minusYears(20)
                ),
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                tilsynsperioder = mapOf(
                        helePerioden.copy(fom = helePerioden.fom.plusDays(15)) to Tilsyn(Prosent(85))
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent.ZERO))
                ).somArbeid()
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkInnvilget(uttaksplan, helePerioden.copy(tom = helePerioden.fom.plusDays(15).minusDays(1)), Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), InnvilgetÅrsaker.FULL_DEKNING)
        sjekkAvslått(uttaksplan, helePerioden.copy(fom = helePerioden.fom.plusDays(15)), setOf(AvslåttÅrsaker.FOR_HØY_TILSYNSGRAD))
    }

    @Test
    fun `Kun medlem i slutten av søknadsperioden`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-25")
        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        fødselsdato = LocalDate.now().minusYears(20)
                ),
                tilsynsbehov = mapOf(
                        søknadsperiode to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(søknadsperiode),
                ikkeMedlem = listOf(LukketPeriode("2020-01-01/2020-01-15")),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent.ZERO))
                ).somArbeid()
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkAvslått(uttaksplan, LukketPeriode("2020-01-01/2020-01-15"), setOf(AvslåttÅrsaker.IKKE_MEDLEM_I_FOLKETRYGDEN))
        sjekkInnvilget(uttaksplan, LukketPeriode("2020-01-16/2020-01-25"), Prosent(100), mapOf(arbeidsforhold1 to Prosent(100)), InnvilgetÅrsaker.FULL_DEKNING)
    }

    @Test
    fun `Får jobbet siste halvdel av en perioder`() {
        val søknadsperiode = LukketPeriode("2020-03-09/2020-03-22")
        val periode1 = LukketPeriode("2020-03-09/2020-03-15")
        val periode2 = LukketPeriode("2020-03-16/2020-03-22")

        val grunnlag = RegelGrunnlag(
                søker = Søker(
                        fødselsdato = LocalDate.now().minusYears(20)
                ),
                tilsynsbehov = mapOf(
                        søknadsperiode to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(søknadsperiode),
                arbeid = mapOf(
                        arbeidsforhold1 to mapOf(
                                periode1 to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent.ZERO),
                                periode2 to ArbeidsforholdPeriodeInfo(FULL_UKE, Prosent(20))
                        )
                ).somArbeid()
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)
        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkInnvilget(
                uttaksplan = uttaksplan,
                forventetPeriode = periode1,
                forventetGrad = Prosent(100),
                forventedeUtbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(100)),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.FULL_DEKNING
        )
        sjekkInnvilget(
                uttaksplan = uttaksplan,
                forventetPeriode = periode2,
                forventetGrad = Prosent(80),
                forventedeUtbetalingsgrader = mapOf(arbeidsforhold1 to Prosent(80)),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT
        )
    }
}