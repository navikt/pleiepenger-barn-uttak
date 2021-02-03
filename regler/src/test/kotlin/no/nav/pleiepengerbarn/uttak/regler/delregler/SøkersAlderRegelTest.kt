package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkIkkeOppfylt
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkOppfylt
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.prosent
import no.nav.pleiepengerbarn.uttak.regler.somArbeid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class SøkersAlderRegelTest {

    private companion object {
        private val helePerioden = LukketPeriode("2020-01-06/2020-01-26")
        private val periode1 = LukketPeriode("2020-01-06/2020-01-12")
        private val periode2 = LukketPeriode("2020-01-21/2020-01-26")
        private val behandlingUUID = UUID.randomUUID().toString()
    }

    @Test
    internal fun `Søker fyller 70 siste dag i uttaksplanen`() {
        val grunnlag = lagRegelGrunnlag(
                fyllerÅrSisteDagISisteUttaksperiode = true
        )

        val grunnlagUtenSøttiårsdag = lagRegelGrunnlag()

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        val uttaksplanUtenSøttiårsdag = UttakTjeneste.uttaksplan(grunnlagUtenSøttiårsdag)


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


        assertEquals(uttaksplanUtenSøttiårsdag, uttaksplan)
    }

    @Test
    internal fun `Søker fyller 70 før uttaksplanen`() {
        val grunnlag = lagRegelGrunnlag(
                fyllerÅrFørUttaksplanen = true
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)


        assertEquals(2, uttaksplan.perioder.size)

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = periode1,
                forventetIkkeOppfyltÅrsaker = setOf(Årsak.SØKERS_ALDER)
        )
        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = periode2,
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.LOVBESTEMT_FERIE,
                        Årsak.SØKERS_ALDER
                )
        )
    }

    @Test
    internal fun `Søker fyller ikke 70 i løpet av en oppfylt periode`() {
        val grunnlag = lagRegelGrunnlag(
                fyllerÅrIEnOppfyltPeriode = true
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)


        assertEquals(3, uttaksplan.perioder.size)

        sjekkOppfylt(
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-09"),
                uttaksplan = uttaksplan,
                forventetGrad = Prosent(50),
                forventedeUtbetalingsgrader = mapOf(),
                forventedeOppfyltÅrsak = Årsak.FULL_DEKNING
        )

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-10/2020-01-12"),
                forventetIkkeOppfyltÅrsaker = setOf(Årsak.SØKERS_ALDER)
        )

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = periode2,
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.LOVBESTEMT_FERIE,
                        Årsak.SØKERS_ALDER
                )
        )
    }

    @Test
    internal fun `Søker fyller 70 mellom to uttaksperioder`() {
        val grunnlag = lagRegelGrunnlag(
                fyllerÅrIEtHullIUttaksplanen = true
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)


        assertEquals(2, uttaksplan.perioder.size)

        sjekkOppfylt(
                forventetPeriode = periode1,
                uttaksplan = uttaksplan,
                forventetGrad = Prosent(50),
                forventedeUtbetalingsgrader = mapOf(),
                forventedeOppfyltÅrsak = Årsak.FULL_DEKNING
        )

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = periode2,
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.LOVBESTEMT_FERIE,
                        Årsak.SØKERS_ALDER
                )
        )
    }

    @Test
    internal fun `Søker fyller 70 mellom i løpet av en ikke oppfylt periode`() {
        val grunnlag = lagRegelGrunnlag(
                fyllerÅrIEnIkkeOppfyltPeriode = true
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)


        assertEquals(3, uttaksplan.perioder.size)

        sjekkOppfylt(
                forventetPeriode = periode1,
                uttaksplan = uttaksplan,
                forventetGrad = Prosent(50),
                forventedeUtbetalingsgrader = mapOf(),
                forventedeOppfyltÅrsak = Årsak.FULL_DEKNING
        )

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-21/2020-01-23"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.LOVBESTEMT_FERIE
                )
        )

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-24/2020-01-26"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.LOVBESTEMT_FERIE,
                        Årsak.SØKERS_ALDER
                )
        )
    }

    private fun lagRegelGrunnlag(
        fyllerÅrFørUttaksplanen: Boolean = false,
        fyllerÅrEtterUttaksplanen: Boolean = false,
        fyllerÅrIEnOppfyltPeriode: Boolean = false,
        fyllerÅrIEnIkkeOppfyltPeriode: Boolean = false,
        fyllerÅrIEtHullIUttaksplanen: Boolean = false,
        fyllerÅrSisteDagISisteUttaksperiode: Boolean = false
    ) : RegelGrunnlag {
        val antallFlaggSatt = listOf(
                fyllerÅrFørUttaksplanen,
                fyllerÅrEtterUttaksplanen,
                fyllerÅrIEnOppfyltPeriode,
                fyllerÅrIEnIkkeOppfyltPeriode,
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
                    fyllerÅrIEnOppfyltPeriode -> LocalDate.parse("2020-01-09")
                    fyllerÅrIEnIkkeOppfyltPeriode -> LocalDate.parse("2020-01-23")
                    fyllerÅrIEtHullIUttaksplanen -> LocalDate.parse("2020-01-15")
                    fyllerÅrSisteDagISisteUttaksperiode -> LocalDate.parse("2020-01-26")
                    else -> null
                }

        val fødselsdato = søttiårsdag?.minusYears(70)?: LocalDate.parse("2020-01-19").minusYears(50)

        return RegelGrunnlag(
                behandlingUUID = behandlingUUID,
                søker = Søker(
                    fødselsdato = fødselsdato
                ),
                arbeid = mapOf(
                        "123" to mapOf(
                                periode1 to ArbeidsforholdPeriodeInfo(
                                        jobberNormalt = Duration.ofHours(3).plusMinutes(45),
                                        jobberNå = Duration.ofHours(3).plusMinutes(45).prosent(50)
                                )
                        )
                ).somArbeid(),
                søknadsperioder = listOf(
                        periode1,
                        periode2
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_100
                ),
                lovbestemtFerie = listOf(
                        periode2
                )
        )
    }
}