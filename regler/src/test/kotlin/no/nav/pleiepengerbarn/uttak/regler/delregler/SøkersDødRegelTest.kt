package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkIkkeOppfylt
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkOppfylt
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overordnetPeriode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SøkersDødRegelTest {

    private companion object {
        private val behandlingUUID = UUID.randomUUID().toString()

        private const val aktørIdSøker = "123"
        private const val aktørIdBarn = "456"
    }

    @Test
    internal fun `Søker dør i en oppfylt periode med påfølgende uttaksperioder`() {
        val søkersDødsdato = LocalDate.parse("2020-01-07")

        var uttaksplan = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to UttaksperiodeInfo.innvilgelse(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(80),
                                utbetalingsgrader = listOf(),
                                årsak = Årsak.AVKORTET_MOT_INNTEKT,
                                annenPart = AnnenPart.ALENE
                        ),
                        LukketPeriode("2020-01-11/2020-01-30") to UttaksperiodeInfo.avslag(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                årsaker = setOf(Årsak.LOVBESTEMT_FERIE),
                                annenPart = AnnenPart.ALENE
                        ),
                        LukketPeriode("2020-02-10/2020-02-25") to UttaksperiodeInfo.innvilgelse(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(20),
                                utbetalingsgrader = listOf(),
                                årsak = Årsak.AVKORTET_MOT_INNTEKT,
                                annenPart = AnnenPart.ALENE
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

        sjekkOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-07"),
                forventetGrad = Prosent(80),
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT
        )

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-10"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.SØKERS_DØDSFALL
                )
        )

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-11/2020-01-30"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.LOVBESTEMT_FERIE,
                        Årsak.SØKERS_DØDSFALL
                )
        )

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-02-10/2020-02-25"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.SØKERS_DØDSFALL
                )
        )
    }

    @Test
    internal fun `Om søker fortsatt lever har ikke kjøring av regel noen effekt`() {
        val uttaksplanFørRegelkjøring = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to UttaksperiodeInfo.innvilgelse(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(80),
                                utbetalingsgrader = listOf(),
                                årsak = Årsak.AVKORTET_MOT_INNTEKT,
                                annenPart = AnnenPart.ALENE
                        ),
                        LukketPeriode("2020-01-11/2020-01-30") to UttaksperiodeInfo.avslag(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                årsaker = setOf(Årsak.LOVBESTEMT_FERIE),
                                annenPart = AnnenPart.ALENE
                        ),
                        LukketPeriode("2020-02-10/2020-02-25") to UttaksperiodeInfo.innvilgelse(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(20),
                                utbetalingsgrader = listOf(),
                                årsak = Årsak.AVKORTET_MOT_INNTEKT,
                                annenPart = AnnenPart.ALENE
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
                        LukketPeriode("2020-01-01/2020-01-10") to UttaksperiodeInfo.innvilgelse(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(80),
                                utbetalingsgrader = listOf(),
                                årsak = Årsak.AVKORTET_MOT_INNTEKT,
                                annenPart = AnnenPart.ALENE
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
    internal fun `Søker dør i en ikke oppfylt periode med påfølgende oppfylt periode`() {
        val søkersDødsdato = LocalDate.parse("2020-01-07")

        var uttaksplan = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to UttaksperiodeInfo.avslag(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                årsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT),
                                annenPart = AnnenPart.ALENE
                        ),
                        LukketPeriode("2020-02-11/2020-02-20") to UttaksperiodeInfo.innvilgelse(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(50),
                                utbetalingsgrader = listOf(),
                                årsak = Årsak.AVKORTET_MOT_INNTEKT,
                                annenPart = AnnenPart.ALENE
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

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-07"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT
                )
        )

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-10"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT,
                        Årsak.SØKERS_DØDSFALL
                )
        )

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-02-11/2020-02-20"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.SØKERS_DØDSFALL
                )
        )
    }

    @Test
    internal fun `Om søker dør etter uttaksplanens perioder har kjøring av regel ingen effekt`() {
        val søkersDødsdato = LocalDate.parse("2020-01-11")

        val uttaksplanFørRegelkjøring = Uttaksplan(
                perioder = mapOf(
                        LukketPeriode("2020-01-01/2020-01-10") to UttaksperiodeInfo.innvilgelse(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(80),
                                utbetalingsgrader = listOf(),
                                årsak = Årsak.AVKORTET_MOT_INNTEKT,
                                annenPart = AnnenPart.ALENE
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
                        LukketPeriode("2020-01-01/2020-01-10") to UttaksperiodeInfo.innvilgelse(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                uttaksgrad = Prosent(80),
                                utbetalingsgrader = listOf(),
                                årsak = Årsak.AVKORTET_MOT_INNTEKT,
                                annenPart = AnnenPart.ALENE
                        ),
                        LukketPeriode("2020-01-11/2020-01-15") to UttaksperiodeInfo.avslag(
                                knekkpunktTyper = setOf(),
                                kildeBehandlingUUID = behandlingUUID,
                                årsaker = setOf(Årsak.UTENOM_TILSYNSBEHOV),
                                annenPart = AnnenPart.ALENE
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

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-10"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.SØKERS_DØDSFALL
                )
        )

        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-11/2020-01-15"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.UTENOM_TILSYNSBEHOV,
                        Årsak.SØKERS_DØDSFALL
                )
        )
    }
    
    private fun Uttaksplan.dummyGrunnlag(søkersDødsdato: LocalDate?): RegelGrunnlag {
        val overordnetPeriode = perioder.keys.overordnetPeriode()
        return RegelGrunnlag(
                behandlingUUID = behandlingUUID,
                søker = Søker(
                        aktørId = aktørIdSøker,
                        fødselsdato = søkersDødsdato?:LocalDate.now().minusYears(50),
                        dødsdato = søkersDødsdato
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                pleiebehov = mapOf(
                        overordnetPeriode to Pleiebehov.PROSENT_100
                ),
                søktUttak = listOf(SøktUttak(overordnetPeriode)),
                arbeid = listOf()
        )
    }
}