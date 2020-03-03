package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkAvslått
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overordnetPeriode
import no.nav.pleiepengerbarn.uttak.regler.print
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SøkersDødRegelTest {

    @Test
    internal fun `Søker dør i en innvilget perioder med påfølgende uttaksperioder`() {
        val søkersDødsdato = LocalDate.parse("2020-01-07")

        var uttaksplan = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                grad = Prosent(80),
                                utbetalingsgrader = listOf()
                        ),
                        LukketPeriode("2020-01-11/2020-01-30") to AvslåttPeriode(
                                knekkpunktTyper = setOf(),
                                avslagsÅrsaker = setOf(AvslåttPeriodeÅrsak.OVERLAPPER_MED_FERIE)
                        ),
                        LukketPeriode("2020-02-10/2020-02-25") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                grad = Prosent(20),
                                utbetalingsgrader = listOf()
                        )
                )
        )

        val grunnlag = uttaksplan.dummyGrunnlag(
                søkersDødsdato = søkersDødsdato
        )

        uttaksplan.print(grunnlag)

        uttaksplan = SøkersDødRegel().kjør(
                uttaksplan = uttaksplan,
                grunnlag = grunnlag
        )


        uttaksplan.print(grunnlag)

        assertEquals(4, uttaksplan.perioder.size)

        sjekkInnvilget(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-07"),
                forventetGrad = Prosent(80)
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-10"),
                forventedAvslagsÅrsaker = setOf(
                        AvslåttPeriodeÅrsak.SØKERS_DØDSFALL
                )
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-11/2020-01-30"),
                forventedAvslagsÅrsaker = setOf(
                        AvslåttPeriodeÅrsak.OVERLAPPER_MED_FERIE,
                        AvslåttPeriodeÅrsak.SØKERS_DØDSFALL
                )
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-02-10/2020-02-25"),
                forventedAvslagsÅrsaker = setOf(
                        AvslåttPeriodeÅrsak.SØKERS_DØDSFALL
                )
        )
    }

    @Test
    internal fun `Om søker fortsatt lever har ikke kjøring av regel noen effekt`() {
        val uttaksplanFørRegelkjøring = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                grad = Prosent(80),
                                utbetalingsgrader = listOf()
                        ),
                        LukketPeriode("2020-01-11/2020-01-30") to AvslåttPeriode(
                                knekkpunktTyper = setOf(),
                                avslagsÅrsaker = setOf(AvslåttPeriodeÅrsak.OVERLAPPER_MED_FERIE)
                        ),
                        LukketPeriode("2020-02-10/2020-02-25") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                grad = Prosent(20),
                                utbetalingsgrader = listOf()
                        )
                )
        )

        val grunnlag = uttaksplanFørRegelkjøring.dummyGrunnlag(
                søkersDødsdato = null
        )

        uttaksplanFørRegelkjøring.print(grunnlag)

        val uttaksplanEtterRegelkjøring = SøkersDødRegel().kjør(
                uttaksplan = uttaksplanFørRegelkjøring,
                grunnlag = grunnlag
        )

        assertEquals(uttaksplanFørRegelkjøring, uttaksplanEtterRegelkjøring)
    }

    @Test
    internal fun `Om søker dør siste dag av uttaksperiodene har ikke kjøring av regel noen effekt`() {
        val søkersDødsdato = LocalDate.parse("2020-01-10")

        val uttaksplanFørRegelkjøring = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                grad = Prosent(80),
                                utbetalingsgrader = listOf()
                        )
                )
        )

        val grunnlag = uttaksplanFørRegelkjøring.dummyGrunnlag(
                søkersDødsdato = søkersDødsdato
        )

        uttaksplanFørRegelkjøring.print(grunnlag)

        val uttaksplanEtterRegelkjøring = SøkersDødRegel().kjør(
                uttaksplan = uttaksplanFørRegelkjøring,
                grunnlag = grunnlag
        )

        assertEquals(uttaksplanFørRegelkjøring, uttaksplanEtterRegelkjøring)
    }

    @Test
    internal fun `Søker dør i en avslått periode med påfølgende innvilget periode`() {
        val søkersDødsdato = LocalDate.parse("2020-01-07")

        var uttaksplan = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to AvslåttPeriode(
                                knekkpunktTyper = setOf(),
                                avslagsÅrsaker = setOf(AvslåttPeriodeÅrsak.IKKE_MEDLEM)
                        ),
                        LukketPeriode("2020-02-11/2020-02-20") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                grad = Prosent(50),
                                utbetalingsgrader = listOf()
                        )
                )
        )

        val grunnlag = uttaksplan.dummyGrunnlag(
                søkersDødsdato = søkersDødsdato
        )

        uttaksplan.print(grunnlag)

        uttaksplan = SøkersDødRegel().kjør(
                uttaksplan = uttaksplan,
                grunnlag = grunnlag
        )

        uttaksplan.print(grunnlag)

        assertEquals(3, uttaksplan.perioder.size)

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-07"),
                forventedAvslagsÅrsaker = setOf(
                        AvslåttPeriodeÅrsak.IKKE_MEDLEM
                )
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-10"),
                forventedAvslagsÅrsaker = setOf(
                        AvslåttPeriodeÅrsak.IKKE_MEDLEM,
                        AvslåttPeriodeÅrsak.SØKERS_DØDSFALL
                )
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-02-11/2020-02-20"),
                forventedAvslagsÅrsaker = setOf(
                        AvslåttPeriodeÅrsak.SØKERS_DØDSFALL
                )
        )
    }

    @Test
    internal fun `Om søker dør etter uttaksplanens perioder har kjøring av regel ingen effekt`() {
        val søkersDødsdato = LocalDate.parse("2020-01-11")

        val uttaksplanFørRegelkjøring = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                grad = Prosent(80),
                                utbetalingsgrader = listOf()
                        )
                )
        )

        val grunnlag = uttaksplanFørRegelkjøring.dummyGrunnlag(
                søkersDødsdato = søkersDødsdato
        )

        uttaksplanFørRegelkjøring.print(grunnlag)

        val uttaksplanEtterRegelkjøring = SøkersDødRegel().kjør(
                uttaksplan = uttaksplanFørRegelkjøring,
                grunnlag = grunnlag
        )

        assertEquals(uttaksplanFørRegelkjøring, uttaksplanEtterRegelkjøring)
    }

    @Test
    internal fun `Om søker dør før uttaksplanens perioder skal alle periodene avslås`() {
        val søkersDødsdato = LocalDate.parse("2019-12-24")

        var uttaksplan = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                grad = Prosent(80),
                                utbetalingsgrader = listOf()
                        ),
                        LukketPeriode("2020-01-11/2020-01-15") to AvslåttPeriode(
                                knekkpunktTyper = setOf(),
                                avslagsÅrsaker = setOf(
                                        AvslåttPeriodeÅrsak.PERIODE_ETTER_TILSYNSBEHOV
                                )
                        )
                )
        )

        val grunnlag = uttaksplan.dummyGrunnlag(
                søkersDødsdato = søkersDødsdato
        )

        uttaksplan.print(grunnlag)

        uttaksplan = SøkersDødRegel().kjør(
                uttaksplan = uttaksplan,
                grunnlag = grunnlag
        )

        assertEquals(2, uttaksplan.perioder.size)

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-10"),
                forventedAvslagsÅrsaker = setOf(
                        AvslåttPeriodeÅrsak.SØKERS_DØDSFALL
                )
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-11/2020-01-15"),
                forventedAvslagsÅrsaker = setOf(
                        AvslåttPeriodeÅrsak.PERIODE_ETTER_TILSYNSBEHOV,
                        AvslåttPeriodeÅrsak.SØKERS_DØDSFALL
                )
        )
    }
    
    private fun Uttaksplan.dummyGrunnlag(søkersDødsdato: LocalDate?): RegelGrunnlag {
        val overordnetPeriode = perioder.keys.overordnetPeriode()
        return RegelGrunnlag(
                søker = Søker(
                        dødsdato = søkersDødsdato
                ),
                tilsynsbehov = mapOf(
                        overordnetPeriode to Tilsynsbehov(prosent = TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(overordnetPeriode)
        )
    }
}