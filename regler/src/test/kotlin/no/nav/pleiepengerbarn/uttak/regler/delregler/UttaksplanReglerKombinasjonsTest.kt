package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkAvslått
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.prosent
import no.nav.pleiepengerbarn.uttak.regler.somArbeid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

internal class UttaksplanReglerKombinasjonsTest {

    private companion object {
        private val helePerioden = LukketPeriode("2020-01-06/2020-01-12")
        private val forventetGrad = Prosent(50)
        private val forventedeUtbetalingsgrader = mapOf("123" to Prosent(50))
    }

    @Test
    internal fun `Søker og barn dør samme dag`() {
        val søkersDødsdato = LocalDate.parse("2020-01-09")
        val søkersFødselsdato = søkersDødsdato.minusYears(50)
        val barnetsDødsdato = søkersDødsdato

        val grunnlag = lagRegelGrunnlag(
                søkersFødselsdato = søkersFødselsdato,
                søkersDødsdato = søkersDødsdato,
                barnetsDødsdato = barnetsDødsdato
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(3, uttaksplan.perioder.size)

        // Frem til dødsfallene uendret
        sjekkInnvilget(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-09"),
                forventetGrad = forventetGrad,
                forventedeUtbetalingsgrader = forventedeUtbetalingsgrader,
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.FULL_DEKNING
        )

        // Perioden som var innvilget er nå avslått
        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-10/2020-01-12"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.SØKERS_DØDSFALL
                )
        )

        // Blitt lagt til 6 ukers sorgperiode pga. barnets død som
        // I etterkant har blitt avslått grunnet søkers død
        val seksUkerEtterBarnetsDød =
                barnetsDødsdato.plusDays(1).plusWeeks(6)
        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-13/$seksUkerEtterBarnetsDød"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.SØKERS_DØDSFALL
                )
        )
    }

    @Test
    internal fun `Søker dør i sorgperioden etter barnets død`() {
        val barnetsDødsdato = LocalDate.parse("2020-01-09")
        val søkersDødsdato = barnetsDødsdato.plusWeeks(4)
        val søkersFødselsdato = søkersDødsdato.minusYears(30)

        val grunnlag = lagRegelGrunnlag(
                søkersFødselsdato = søkersFødselsdato,
                søkersDødsdato = søkersDødsdato,
                barnetsDødsdato = barnetsDødsdato
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(4, uttaksplan.perioder.size)

        // Frem til barnets dødsfall uendret
        sjekkInnvilget(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-09"),
                forventetGrad = forventetGrad,
                forventedeUtbetalingsgrader = forventedeUtbetalingsgrader,
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.FULL_DEKNING
        )

        // Etter dødsfall fortsatt avkortet mot inntekt
        sjekkInnvilget(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-10/2020-01-12"),
                forventetGrad = forventetGrad,
                forventedeUtbetalingsgrader = forventedeUtbetalingsgrader,
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.FULL_DEKNING
        )
        // Sorgperioden frem til søkers edød
        sjekkInnvilget(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-13/2020-02-06"),
                forventetGrad = Prosent(100),
                forventedeUtbetalingsgrader = mapOf("123" to Prosent(100)),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.BARNETS_DØDSFALL
        )
        // Sorgperiode etter søkers død
        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-02-07/2020-02-21"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.SØKERS_DØDSFALL
                )
        )
    }

    @Test
    internal fun `Søker dør rett etter fylte 70`() {
        val søkersDødsdato = LocalDate.parse("2020-01-09")
        val søkersFødselsdato = søkersDødsdato.minusYears(70).minusDays(2)

        val grunnlag = lagRegelGrunnlag(
                søkersFødselsdato = søkersFødselsdato,
                søkersDødsdato = søkersDødsdato,
                barnetsDødsdato = null
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(3, uttaksplan.perioder.size)

        // Frem til fylte 70
        sjekkInnvilget(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-07"),
                forventetGrad = forventetGrad,
                forventedeUtbetalingsgrader = forventedeUtbetalingsgrader,
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.FULL_DEKNING
        )

        // Frem til død
        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-09"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.SØKERS_ALDER
                )
        )
        // Etter død
        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-10/2020-01-12"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.SØKERS_ALDER,
                        AvslåttÅrsaker.SØKERS_DØDSFALL
                )
        )
    }

    private fun lagRegelGrunnlag(
            søkersFødselsdato: LocalDate,
            søkersDødsdato: LocalDate?,
            barnetsDødsdato: LocalDate?
    ) : RegelGrunnlag {
        return RegelGrunnlag(
                søker = Søker(
                        fødselsdato = søkersFødselsdato,
                        dødsdato = søkersDødsdato
                ),
                barn = Barn(
                        dødsdato = barnetsDødsdato
                ),
                arbeid = mapOf(
                        "123" to mapOf(
                                helePerioden to ArbeidsforholdPeriodeInfo(
                                        jobberNormalt = Duration.ofHours(7).plusMinutes(30),
                                        taptArbeidstid = Duration.ofHours(7).plusMinutes(30).prosent(50),
                                        søkersTilsyn = Duration.ofHours(7).plusMinutes(30).prosent(50)
                                )
                        )
                ).somArbeid(),
                søknadsperioder = listOf(
                        helePerioden
                ),
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(
                                prosent = TilsynsbehovStørrelse.PROSENT_100
                        )
                )
        )
    }
}