package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.util.*

@Disabled
internal class UttakTjenesteGraderingTest {

    private companion object {val FULL_UKE = Duration.ofHours(37).plusMinutes(30)}

    private val arbeidsforhold1 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID())
    private val arbeidsforhold2 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID())
    private val arbeidsforhold3 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "987654321", arbeidsforholdId = UUID.randomUUID())
    private val arbeidsforhold4 = Arbeidsforhold(arbeidstype = Arbeidstype.ARBEIDSGIVER, organisasjonsnummer = "987654321", arbeidsforholdId = UUID.randomUUID())

    private val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))

    @Test
    fun `En uttaksperiode med overlappende tilsynsperiode skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                tilsynsperioder = mapOf(
                        helePerioden to Tilsyn(Prosent(20))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder.entries.first(), helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(80))
    }

    @Test
    fun `En uttaksperiode med overlappende arbeidsperiode skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(25))))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder.entries.first(), helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(75))
    }

    @Test
    fun `En uttaksperiode med overlappende arbeidsperiode og uttak på annen part skal føre til redusert grad på uttaksperiode`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                andrePartersUttaksplan = listOf(
                        Uttaksplan(perioder = mapOf(helePerioden to InnvilgetPeriode(knekkpunktTyper = setOf(), grad = Prosent(40))
                ))),
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(25))))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder.entries.first(), helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(60))
    }


    @Test
    fun `En uttaksperiode med mer arbeid enn tilsyn, så skal perioden graderes mot arbeid`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(35))))
                ),
                tilsynsperioder = mapOf(
                        helePerioden to Tilsyn(Prosent(30))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder.entries.first(), helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(65))
    }


    @Test
    fun `En uttaksperiode med mer tilsyn enn arbeid, så skal perioden graderes mot arbeid`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE, skalJobbe = Prosent(25))))
                ),
                tilsynsperioder = mapOf(
                        helePerioden to Tilsyn(Prosent(30))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        assertTrue(uttaksplan.perioder.size == 1)
        sjekkInnvilget(uttaksplan.perioder.entries.first(), helePerioden.copy(tom = LocalDate.of(2020, Month.JANUARY, 31)), Prosent(70))
    }

    @Test
    fun `En uttaksperioder med fire arbeidsforhold som skal vurderes til gradering mot tilsyn`() {
        val grunnlag = RegelGrunnlag(
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(
                        helePerioden
                ),
                arbeid = listOf(
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold1, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE.minusHours(30), skalJobbe = Prosent(40)))),
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold2, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE.minusHours(30), skalJobbe = Prosent(20)))),
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold3, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE.minusHours(30), skalJobbe = Prosent(80)))),
                        ArbeidsforholdOgArbeidsperioder(arbeidsforhold4, mapOf(helePerioden to ArbeidInfo(jobberNormalt = FULL_UKE.minusHours(30), skalJobbe = Prosent(0))))

                ),
                tilsynsperioder = mapOf(
                        helePerioden to Tilsyn(Prosent(40))
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplanOgPrint(grunnlag)

        sjekkInnvilget(uttaksplan.perioder.entries.first(), helePerioden, Prosent(60))

    }
}