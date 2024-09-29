package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class UttaksplanMergerTest {

    private val arbeidsforhold1 = UUID.randomUUID().toString()
    private val behandling1 = UUID.randomUUID().toString()
    private val behandling2 = UUID.randomUUID().toString()

    private val hundreProsent = Prosent(100)
    private fun oppfylt(behandlingUUID: BehandlingUUID) = UttaksperiodeInfo.oppfylt(
        uttaksgrad = hundreProsent,
        uttaksgradUtenReduksjonGrunnetInntektsgradering = hundreProsent,
        uttaksgradMedReduksjonGrunnetInntektsgradering = null,
        utbetalingsgrader = mapOf(arbeidsforhold1 to hundreProsent).somUtbetalingsgrader(),
        søkersTapteArbeidstid = Prosent(100),
        oppgittTilsyn = null,
        årsak = Årsak.AVKORTET_MOT_INNTEKT,
        pleiebehov = Pleiebehov.PROSENT_100.prosent,
        knekkpunktTyper = setOf(),
        kildeBehandlingUUID = behandlingUUID,
        annenPart = AnnenPart.ALENE,
        nattevåk = null,
        beredskap = null,
        utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN),
    )

    private fun ikkeOppfylt(behandlingUUID: BehandlingUUID) = UttaksperiodeInfo.ikkeOppfylt(
        utbetalingsgrader = mapOf(arbeidsforhold1 to Prosent.ZERO).somUtbetalingsgrader(),
        søkersTapteArbeidstid = Prosent(100),
        oppgittTilsyn = null,
        årsaker = setOf(Årsak.FOR_LAV_REST_PGA_ETABLERT_TILSYN),
        pleiebehov = Pleiebehov.PROSENT_100.prosent,
        knekkpunktTyper = setOf(),
        kildeBehandlingUUID = behandlingUUID,
        annenPart = AnnenPart.ALENE,
        nattevåk = null,
        beredskap = null,
        utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN)
    )

    @Test
    fun `En uttaksplan med delvis overlapp en annen uttaksplan skal slås sammen`() {

        val uttaksplan1 = Uttaksplan(
            mapOf(LukketPeriode("2020-01-01/2020-01-31") to oppfylt(behandling1)),
            listOf()
        )
        val uttaksplan2 = Uttaksplan(
            mapOf(LukketPeriode("2020-01-15/2020-02-10") to oppfylt(behandling2)),
            listOf()
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(uttaksplan1, uttaksplan2, listOf())

        assertThat(sammenslåttUttaksplan).isEqualTo(
            Uttaksplan(
                mapOf(
                    LukketPeriode("2020-01-01/2020-01-14") to oppfylt(behandling1),
                    LukketPeriode("2020-01-15/2020-02-10") to oppfylt(behandling2)
                ), listOf()
            )
        )
    }

    @Test
    fun `En uttaksplan med delvis overlapp en annen uttaksplan(første uttaksperioder i nyeste uttaksplan) skal slås sammen`() {

        val uttaksplan1 = Uttaksplan(
            mapOf(LukketPeriode("2020-01-01/2020-02-10") to oppfylt(behandling1)),
            listOf()
        )
        val uttaksplan2 = Uttaksplan(
            mapOf(LukketPeriode("2020-01-17/2020-03-31") to oppfylt(behandling2)),
            listOf()
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(uttaksplan1, uttaksplan2, listOf())

        assertThat(sammenslåttUttaksplan).isEqualTo(
            Uttaksplan(
                mapOf(
                    LukketPeriode("2020-01-01/2020-01-16") to oppfylt(behandling1),
                    LukketPeriode("2020-01-17/2020-03-31") to oppfylt(behandling2)
                ), listOf()
            )
        )
    }

    @Test
    fun `En uttaksplan med hull, skal føre til at underliggende uttaksplan skal brukes i hullet`() {

        val uttaksplan1 = Uttaksplan(
            mapOf(LukketPeriode("2020-01-01/2020-01-31") to oppfylt(behandling1)),
            listOf()
        )
        val uttaksplan2 = Uttaksplan(
            mapOf(
                LukketPeriode("2020-01-01/2020-01-15") to oppfylt(behandling2),
                LukketPeriode("2020-01-25/2020-02-15") to oppfylt(behandling2)
            ),
            listOf()
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(uttaksplan1, uttaksplan2, listOf())

        assertThat(sammenslåttUttaksplan).isEqualTo(
            Uttaksplan(
                mapOf(
                    LukketPeriode("2020-01-01/2020-01-15") to oppfylt(behandling2),
                    LukketPeriode("2020-01-16/2020-01-24") to oppfylt(behandling1),
                    LukketPeriode("2020-01-25/2020-02-15") to oppfylt(behandling2)
                ), listOf()
            )
        )
    }

    @Test
    fun `En tidligere uttaksplan med oppfylt periode og en nyere uttaksplan med en ikke oppfylt periode, skal føre til at opprinnelige uttaksperioder blir splittet`() {

        val uttaksplan1 = Uttaksplan(
            mapOf(LukketPeriode("2020-01-01/2020-01-31") to oppfylt(behandling1)),
            listOf()
        )
        val uttaksplan2 = Uttaksplan(
            mapOf(LukketPeriode("2020-01-10/2020-01-20") to ikkeOppfylt(behandling2)),
            listOf()
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(uttaksplan1, uttaksplan2, listOf())

        assertThat(sammenslåttUttaksplan).isEqualTo(
            Uttaksplan(
                mapOf(
                    LukketPeriode("2020-01-01/2020-01-09") to oppfylt(behandling1),
                    LukketPeriode("2020-01-10/2020-01-20") to ikkeOppfylt(behandling2),
                    LukketPeriode("2020-01-21/2020-01-31") to oppfylt(behandling1)
                ), listOf()
            )
        )
    }

    @Test
    fun `Søker trekker uttak fra tidligere uttaksplan`() {
        val uttaksplan1 = Uttaksplan(
            mapOf(LukketPeriode("2020-01-01/2020-01-31") to oppfylt(behandling1)),
            listOf()
        )

        val uttaksplan2 = Uttaksplan(
            mapOf(),
            listOf()
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(
            uttaksplan1,
            uttaksplan2,
            listOf(LukketPeriode("2020-01-10/2020-01-20"))
        )
        assertThat(sammenslåttUttaksplan.perioder.keys.size).isEqualTo(2)
        assertThat(sammenslåttUttaksplan).isEqualTo(
            Uttaksplan(
                mapOf(
                    LukketPeriode("2020-01-01/2020-01-09") to oppfylt(behandling1),
                    LukketPeriode("2020-01-21/2020-01-31") to oppfylt(behandling1)
                ), listOf(LukketPeriode("2020-01-10/2020-01-20"))
            )
        )
    }

    @Test
    fun `Søker trekker uttak fra tidligere uttaksplan og legget til nytt uttak`() {
        val uttaksplan1 = Uttaksplan(
            mapOf(LukketPeriode("2020-01-01/2020-01-31") to oppfylt(behandling1)),
            listOf()
        )

        val uttaksplan2 = Uttaksplan(
            mapOf(LukketPeriode("2020-02-01/2020-02-20") to oppfylt(behandling2)),
            listOf()
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(
            uttaksplan1,
            uttaksplan2,
            listOf(LukketPeriode("2020-01-10/2020-01-20"))
        )
        assertThat(sammenslåttUttaksplan.perioder.keys.size).isEqualTo(3)
        assertThat(sammenslåttUttaksplan).isEqualTo(
            Uttaksplan(
                mapOf(
                    LukketPeriode("2020-01-01/2020-01-09") to oppfylt(behandling1),
                    LukketPeriode("2020-01-21/2020-01-31") to oppfylt(behandling1),
                    LukketPeriode("2020-02-01/2020-02-20") to oppfylt(behandling2)

                ), listOf(LukketPeriode("2020-01-10/2020-01-20"))
            )
        )
    }

}
