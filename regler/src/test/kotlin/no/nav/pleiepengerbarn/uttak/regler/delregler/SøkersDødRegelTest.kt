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
    internal fun `Håndtere når søker dør i en innvilget perioder med påfølgende uttaksperioder`() {
        val søkersDødsdato = LocalDate.parse("2020-01-07")

        var uttaksplan = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                grad = Prosent(80)
                        ),
                        LukketPeriode("2020-01-11/2020-01-30") to AvslåttPeriode(
                                knekkpunktTyper = setOf(),
                                avslagsÅrsaker = setOf(AvslåttPeriodeÅrsak.OVERLAPPER_MED_FERIE)
                        ),
                        LukketPeriode("2020-02-10/2020-02-25") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                grad = Prosent(20)
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

        sjekkInnvilget(
                uttaksperiode = uttaksplan.perioder.entries.first(),
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-07"),
                forventetGrad = Prosent(80)
        )

        sjekkAvslått(
                uttaksperiode = uttaksplan.perioder.entries.elementAt(1),
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-10"),
                forventedAvslagsÅrsaker = setOf(
                        AvslåttPeriodeÅrsak.SØKERS_DØDSFALL
                )
        )

        sjekkAvslått(
                uttaksperiode = uttaksplan.perioder.entries.elementAt(2),
                forventetPeriode = LukketPeriode("2020-01-11/2020-01-30"),
                forventedAvslagsÅrsaker = setOf(
                        AvslåttPeriodeÅrsak.OVERLAPPER_MED_FERIE,
                        AvslåttPeriodeÅrsak.SØKERS_DØDSFALL
                )
        )

        sjekkAvslått(
                uttaksperiode = uttaksplan.perioder.entries.elementAt(3),
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
                                grad = Prosent(80)
                        ),
                        LukketPeriode("2020-01-11/2020-01-30") to AvslåttPeriode(
                                knekkpunktTyper = setOf(),
                                avslagsÅrsaker = setOf(AvslåttPeriodeÅrsak.OVERLAPPER_MED_FERIE)
                        ),
                        LukketPeriode("2020-02-10/2020-02-25") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                grad = Prosent(20)
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