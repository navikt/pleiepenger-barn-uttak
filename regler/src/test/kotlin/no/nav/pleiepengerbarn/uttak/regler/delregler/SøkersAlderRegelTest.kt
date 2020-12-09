package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkAvslått
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.print
import no.nav.pleiepengerbarn.uttak.regler.prosent
import no.nav.pleiepengerbarn.uttak.regler.somArbeid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

internal class SøkersAlderRegelTest {

    private companion object {
        private val helePerioden = LukketPeriode("2020-01-06/2020-01-26")
        private val periode1 = LukketPeriode("2020-01-06/2020-01-12")
        private val periode2 = LukketPeriode("2020-01-21/2020-01-26")
    }

    @Test
    internal fun `Søker fyller 70 siste dag i uttaksplanen`() {
        val grunnlag = lagRegelGrunnlag(
                fyllerÅrSisteDagISisteUttaksperiode = true
        )

        val grunnlagUtenSøttiårsdag = lagRegelGrunnlag()

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        val uttaksplanUtenSøttiårsdag = UttakTjeneste.uttaksplan(grunnlagUtenSøttiårsdag)

        uttaksplan.print(grunnlag)

        assertEquals(uttaksplanUtenSøttiårsdag, uttaksplan)
    }

    @Test
    internal fun `Søker fyller 70 etter uttaksplanen`() {
        val grunnlag = lagRegelGrunnlag(
                fyllerÅrEtterUttaksplanen = true
        )

        val grunnlagUtenSøttiårsdag = lagRegelGrunnlag()

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        val uttaksplanUtenSøttiårsdag = UttakTjeneste.uttaksplan(grunnlagUtenSøttiårsdag)

        uttaksplan.print(grunnlag)

        assertEquals(uttaksplanUtenSøttiårsdag, uttaksplan)
    }

    @Test
    internal fun `Søker fyller 70 før uttaksplanen`() {
        val grunnlag = lagRegelGrunnlag(
                fyllerÅrFørUttaksplanen = true
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        uttaksplan.print(grunnlag)

        assertEquals(2, uttaksplan.perioder.size)

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = periode1,
                forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.SØKERS_ALDER)
        )
        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = periode2,
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.LOVBESTEMT_FERIE,
                        AvslåttÅrsaker.SØKERS_ALDER
                )
        )
    }

    @Test
    internal fun `Søker fyller ikke 70 i løpet av en innvilget periode`() {
        val grunnlag = lagRegelGrunnlag(
                fyllerÅrIEnInnvilgetPeriode = true
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        uttaksplan.print(grunnlag)

        assertEquals(3, uttaksplan.perioder.size)

        sjekkInnvilget(
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-09"),
                uttaksplan = uttaksplan,
                forventetGrad = Prosent(25),
                forventedeUtbetalingsgrader = mapOf(),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-10/2020-01-12"),
                forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.SØKERS_ALDER)
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = periode2,
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.LOVBESTEMT_FERIE,
                        AvslåttÅrsaker.SØKERS_ALDER
                )
        )
    }

    @Test
    internal fun `Søker fyller 70 mellom to uttaksperioder`() {
        val grunnlag = lagRegelGrunnlag(
                fyllerÅrIEtHullIUttaksplanen = true
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        uttaksplan.print(grunnlag)

        assertEquals(2, uttaksplan.perioder.size)

        sjekkInnvilget(
                forventetPeriode = periode1,
                uttaksplan = uttaksplan,
                forventetGrad = Prosent(25),
                forventedeUtbetalingsgrader = mapOf(),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = periode2,
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.LOVBESTEMT_FERIE,
                        AvslåttÅrsaker.SØKERS_ALDER
                )
        )
    }

    @Test
    internal fun `Søker fyller 70 mellom i løpet av en avslått periode`() {
        val grunnlag = lagRegelGrunnlag(
                fyllerÅrIEnAvslåttperiode = true
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        uttaksplan.print(grunnlag)

        assertEquals(3, uttaksplan.perioder.size)

        sjekkInnvilget(
                forventetPeriode = periode1,
                uttaksplan = uttaksplan,
                forventetGrad = Prosent(25),
                forventedeUtbetalingsgrader = mapOf(),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-21/2020-01-23"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.LOVBESTEMT_FERIE
                )
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-24/2020-01-26"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.LOVBESTEMT_FERIE,
                        AvslåttÅrsaker.SØKERS_ALDER
                )
        )
    }

    private fun lagRegelGrunnlag(
            fyllerÅrFørUttaksplanen: Boolean = false,
            fyllerÅrEtterUttaksplanen: Boolean = false,
            fyllerÅrIEnInnvilgetPeriode: Boolean = false,
            fyllerÅrIEnAvslåttperiode: Boolean = false,
            fyllerÅrIEtHullIUttaksplanen: Boolean = false,
            fyllerÅrSisteDagISisteUttaksperiode: Boolean = false
    ) : RegelGrunnlag {
        val antallFlaggSatt = listOf(
                fyllerÅrFørUttaksplanen,
                fyllerÅrEtterUttaksplanen,
                fyllerÅrIEnInnvilgetPeriode,
                fyllerÅrIEnAvslåttperiode,
                fyllerÅrIEtHullIUttaksplanen)
                .filter { it }
                .size

        if (antallFlaggSatt > 1) {
            throw IllegalStateException("Kun et flagg kan settes")
        }

        val søttiårsdag =
                when {
                    fyllerÅrFørUttaksplanen -> LocalDate.parse("2019-12-31")
                    fyllerÅrEtterUttaksplanen -> LocalDate.parse("2020-02-01")
                    fyllerÅrIEnInnvilgetPeriode -> LocalDate.parse("2020-01-09")
                    fyllerÅrIEnAvslåttperiode -> LocalDate.parse("2020-01-23")
                    fyllerÅrIEtHullIUttaksplanen -> LocalDate.parse("2020-01-15")
                    fyllerÅrSisteDagISisteUttaksperiode -> LocalDate.parse("2020-01-26")
                    else -> null
                }

        val fødselsdato = søttiårsdag?.minusYears(70)?: LocalDate.parse("2020-01-19").minusYears(50)

        return RegelGrunnlag(
                søker = Søker(
                    fødselsdato = fødselsdato
                ),
                arbeid = mapOf(
                        "123" to mapOf(
                                periode1 to ArbeidsforholdPeriodeInfo(
                                        jobberNormalt = Duration.ofHours(3).plusMinutes(45),
                                        taptArbeidstid = Duration.ofHours(3).plusMinutes(45).prosent(50)
                                )
                        )
                ).somArbeid(),
                søknadsperioder = listOf(
                        periode1,
                        periode2
                ),
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(
                                prosent = TilsynsbehovStørrelse.PROSENT_100
                        )
                ),
                lovbestemtFerie = listOf(
                        periode2
                )
        )
    }
}