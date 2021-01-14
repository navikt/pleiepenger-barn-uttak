package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkAvslått
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overordnetPeriode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SøkersDødRegelTest {

    private companion object {
        private val hjemler = setOf(Hjemmel(
                henvisning = "Henvsining til en lov",
                anvendelse = "Testformål"
        ))
        private val innvilgetÅrsak = InnvilgetÅrsak(
                årsak = InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT,
                hjemler = hjemler
        )
        private val behandlingUUID = UUID.randomUUID().toString()
    }

    @Test
    internal fun `Søker dør i en innvilget perioder med påfølgende uttaksperioder`() {
        val søkersDødsdato = LocalDate.parse("2020-01-07")

        var uttaksplan = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(80),
                                utbetalingsgrader = listOf(),
                                årsak = innvilgetÅrsak

                        ),
                        LukketPeriode("2020-01-11/2020-01-30") to AvslåttPeriode(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                årsaker = setOf(AvslåttÅrsak(
                                        årsak = AvslåttÅrsaker.LOVBESTEMT_FERIE,
                                        hjemler = hjemler
                                ))
                        ),
                        LukketPeriode("2020-02-10/2020-02-25") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(20),
                                utbetalingsgrader = listOf(),
                                årsak = innvilgetÅrsak
                        )
                )
        )

        val grunnlag = uttaksplan.dummyGrunnlag(
                søkersDødsdato = søkersDødsdato
        )


        uttaksplan = SøkersDødRegel().kjør(
                uttaksplan = uttaksplan,
                grunnlag = grunnlag
        )



        assertEquals(4, uttaksplan.perioder.size)

        sjekkInnvilget(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-07"),
                forventetGrad = Prosent(80),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-10"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.SØKERS_DØDSFALL
                )
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-11/2020-01-30"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.LOVBESTEMT_FERIE,
                        AvslåttÅrsaker.SØKERS_DØDSFALL
                )
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-02-10/2020-02-25"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.SØKERS_DØDSFALL
                )
        )
    }

    @Test
    internal fun `Om søker fortsatt lever har ikke kjøring av regel noen effekt`() {
        val uttaksplanFørRegelkjøring = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(80),
                                utbetalingsgrader = listOf(),
                                årsak = innvilgetÅrsak
                        ),
                        LukketPeriode("2020-01-11/2020-01-30") to AvslåttPeriode(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                årsaker = setOf(AvslåttÅrsak(
                                        årsak = AvslåttÅrsaker.LOVBESTEMT_FERIE,
                                        hjemler = hjemler
                                ))
                        ),
                        LukketPeriode("2020-02-10/2020-02-25") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(20),
                                utbetalingsgrader = listOf(),
                                årsak = innvilgetÅrsak
                        )
                )
        )

        val grunnlag = uttaksplanFørRegelkjøring.dummyGrunnlag(
                søkersDødsdato = null
        )


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
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(80),
                                utbetalingsgrader = listOf(),
                                årsak = innvilgetÅrsak
                        )
                )
        )

        val grunnlag = uttaksplanFørRegelkjøring.dummyGrunnlag(
                søkersDødsdato = søkersDødsdato
        )


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
                                kildeBehandlingUUID = behandlingUUID,
                                årsaker = setOf(AvslåttÅrsak(
                                        årsak = AvslåttÅrsaker.IKKE_MEDLEM_I_FOLKETRYGDEN,
                                        hjemler = hjemler
                                ))
                        ),
                        LukketPeriode("2020-02-11/2020-02-20") to InnvilgetPeriode(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(50),
                                utbetalingsgrader = listOf(),
                                årsak = innvilgetÅrsak
                        )
                )
        )

        val grunnlag = uttaksplan.dummyGrunnlag(
                søkersDødsdato = søkersDødsdato
        )


        uttaksplan = SøkersDødRegel().kjør(
                uttaksplan = uttaksplan,
                grunnlag = grunnlag
        )


        assertEquals(3, uttaksplan.perioder.size)

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-07"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.IKKE_MEDLEM_I_FOLKETRYGDEN
                )
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-10"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.IKKE_MEDLEM_I_FOLKETRYGDEN,
                        AvslåttÅrsaker.SØKERS_DØDSFALL
                )
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-02-11/2020-02-20"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.SØKERS_DØDSFALL
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
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(80),
                                utbetalingsgrader = listOf(),
                                årsak = innvilgetÅrsak
                        )
                )
        )

        val grunnlag = uttaksplanFørRegelkjøring.dummyGrunnlag(
                søkersDødsdato = søkersDødsdato
        )


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
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(80),
                                utbetalingsgrader = listOf(),
                                årsak = innvilgetÅrsak
                        ),
                        LukketPeriode("2020-01-11/2020-01-15") to AvslåttPeriode(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                årsaker = setOf(AvslåttÅrsak(
                                        årsak = AvslåttÅrsaker.UTENOM_TILSYNSBEHOV,
                                        hjemler = hjemler
                                ))
                        )
                )
        )

        val grunnlag = uttaksplan.dummyGrunnlag(
                søkersDødsdato = søkersDødsdato
        )


        uttaksplan = SøkersDødRegel().kjør(
                uttaksplan = uttaksplan,
                grunnlag = grunnlag
        )

        assertEquals(2, uttaksplan.perioder.size)

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-10"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.SØKERS_DØDSFALL
                )
        )

        sjekkAvslått(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-11/2020-01-15"),
                forventetAvslåttÅrsaker = setOf(
                        AvslåttÅrsaker.UTENOM_TILSYNSBEHOV,
                        AvslåttÅrsaker.SØKERS_DØDSFALL
                )
        )
    }
    
    private fun Uttaksplan.dummyGrunnlag(søkersDødsdato: LocalDate?): RegelGrunnlag {
        val overordnetPeriode = perioder.keys.overordnetPeriode()
        return RegelGrunnlag(
                behandlingUUID = behandlingUUID,
                søker = Søker(
                        fødselsdato = søkersDødsdato?:LocalDate.now().minusYears(50),
                        dødsdato = søkersDødsdato
                ),
                tilsynsbehov = mapOf(
                        overordnetPeriode to Tilsynsbehov(prosent = TilsynsbehovStørrelse.PROSENT_100)
                ),
                søknadsperioder = listOf(overordnetPeriode)
        )
    }
}