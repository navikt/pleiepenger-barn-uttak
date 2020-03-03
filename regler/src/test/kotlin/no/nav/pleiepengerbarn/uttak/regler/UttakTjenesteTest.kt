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

    private val arbeidsforhold1 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "123456789")

    @Test
    fun `Enkel uttaksperiode uten annen informasjon`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_200)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(FULL_UKE, Prosent.ZERO)))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(100))
    }


    @Test
    fun `En uttaksperiode som delvis overlapper med ferie`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_200)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                ferier = listOf(
                        LukketPeriode(LocalDate.of(2020, Month.JANUARY, 15), LocalDate.of(2020, Month.FEBRUARY, 15))
                ),
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(FULL_UKE, Prosent.ZERO)))
                )

        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkInnvilget(uttaksplan, helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 14)), Prosent(100))
        sjekkAvslått(uttaksplan, helePerioden.copy(fom = LocalDate.of(2020, Month.JANUARY, 15)), setOf(AvslåttPeriodeÅrsak.OVERLAPPER_MED_FERIE))
    }

    @Test
    fun `En uttaksperiode som fortsetter etter slutt på tilsynsbehov perioden, skal avslås fra slutt på tilsynsbehov perioden`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_200)
                ),
                søknadsperioder = listOf(
                        LukketPeriode(helePerioden.fom, helePerioden.tom.plusDays(7))
                ),
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(FULL_UKE, Prosent.ZERO)))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkInnvilget(uttaksplan, helePerioden, Prosent(100))
        sjekkAvslått(uttaksplan, LukketPeriode(helePerioden.tom.plusDays(1), helePerioden.tom.plusDays(7)), setOf(AvslåttPeriodeÅrsak.PERIODE_ETTER_TILSYNSBEHOV))
    }

    @Test
    fun `En uttaksperiode som overlapper med tilsyn slik at uttaksgraden blir under 20 prosent, skal avslås pga for lav prosent`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                tilsynsperioder = mapOf(
                        helePerioden.copy(fom = helePerioden.fom.plusDays(15)) to Tilsyn(Prosent(85))
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(FULL_UKE, Prosent.ZERO)))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkInnvilget(uttaksplan, helePerioden.copy(tom = helePerioden.fom.plusDays(15).minusDays(1)), Prosent(100))
        sjekkAvslått(uttaksplan, helePerioden.copy(fom = helePerioden.fom.plusDays(15)), setOf(AvslåttPeriodeÅrsak.FOR_LAV_UTTAKSGRAD))
    }

    @Test
    fun `Kun medlem i slutten av søknadsperioden`() {
        val søknadsperiode = LukketPeriode("2020-01-01/2020-01-25")
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        søknadsperiode to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(søknadsperiode),
                ikkeMedlem = listOf(LukketPeriode("2020-01-01/2020-01-15")),
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(søknadsperiode to ArbeidInfo(FULL_UKE, Prosent.ZERO)))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkAvslått(uttaksplan, LukketPeriode("2020-01-01/2020-01-15"), setOf(AvslåttPeriodeÅrsak.IKKE_MEDLEM))
        sjekkInnvilget(uttaksplan, LukketPeriode("2020-01-16/2020-01-25"), Prosent(100))
    }
}