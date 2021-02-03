package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class UttaksplanMergerTest {

    private val arbeidsforhold1 = UUID.randomUUID().toString()
    private val behandlingUUID = UUID.randomUUID().toString()

    private val hundreProsent = Prosent(100)
    private val oppfylt = UttaksperiodeInfo.innvilgelse(
            uttaksgrad = hundreProsent,
            utbetalingsgrader = mapOf(arbeidsforhold1 to hundreProsent).somUtbetalingsgrader(),
            årsak = Årsak.AVKORTET_MOT_INNTEKT,
            knekkpunktTyper = setOf(),
            kildeBehandlingUUID = behandlingUUID,
            annenPart = AnnenPart.ALENE
    )

    private val ikkeOppfylt = UttaksperiodeInfo.avslag(
            årsaker = setOf(Årsak.FOR_LAV_GRAD),
            knekkpunktTyper = setOf(),
            kildeBehandlingUUID = behandlingUUID,
            annenPart = AnnenPart.ALENE
    )

    @Test
    fun `En uttaksplan med en uttaksperiode skal bli til samme uttaksplan`() {

        val enkelUttakplan =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-01/2020-01-31") to oppfylt)
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(enkelUttakplan))

        assertThat(sammenslåttUttaksplan).isEqualTo(Uttaksplan(enkelUttakplan.perioder))
    }

    @Test
    fun `En uttaksplan med delvis overlapp en annen uttaksplan skal slås sammen`() {

        val uttaksplan1 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-01/2020-01-31") to oppfylt)
        )
        val uttaksplan2 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-15/2020-02-10") to oppfylt)
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(uttaksplan2, uttaksplan1))

        assertThat(sammenslåttUttaksplan).isEqualTo(Uttaksplan(mapOf(
                LukketPeriode("2020-01-01/2020-01-14") to oppfylt,
                LukketPeriode("2020-01-15/2020-02-10") to oppfylt
        )))
    }

    @Test
    fun `En uttaksplan med delvis overlapp en annen uttaksplan(første uttaksperioder i nyeste uttaksplan) skal slås sammen`() {

        val uttaksplan1 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-01/2020-02-10") to oppfylt)
        )
        val uttaksplan2 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-17/2020-03-31") to oppfylt)
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(uttaksplan2, uttaksplan1))

        assertThat(sammenslåttUttaksplan).isEqualTo(Uttaksplan(mapOf(
                LukketPeriode("2020-01-01/2020-01-16") to oppfylt,
                LukketPeriode("2020-01-17/2020-03-31") to oppfylt
        )))
    }

    @Test
    fun `En uttaksplan med hull, skal føre til at underliggende uttaksplan skal brukes i hullet`() {

        val uttaksplan1 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-01/2020-01-31") to oppfylt)
        )
        val uttaksplan2 =  Uttaksplan(
                mapOf(
                        LukketPeriode("2020-01-01/2020-01-15") to oppfylt,
                        LukketPeriode("2020-01-25/2020-02-15") to oppfylt
                )
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(uttaksplan2, uttaksplan1))

        assertThat(sammenslåttUttaksplan).isEqualTo(Uttaksplan(mapOf(
                LukketPeriode("2020-01-01/2020-01-15") to oppfylt,
                LukketPeriode("2020-01-16/2020-01-24") to oppfylt,
                LukketPeriode("2020-01-25/2020-02-15") to oppfylt
        )))
    }

    @Test
    fun `En tidligere uttaksplan med oppfylt periode og en nyere uttaksplan med en ikke oppfylt periode, skal føre til at opprinnelige uttaksperioder blir splittet`() {

        val uttaksplan1 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-10/2020-01-20") to ikkeOppfylt)
        )
        val uttaksplan2 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-01/2020-01-31") to oppfylt)
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(uttaksplan1, uttaksplan2))

        assertThat(sammenslåttUttaksplan).isEqualTo(Uttaksplan(mapOf(
                LukketPeriode("2020-01-01/2020-01-09") to oppfylt,
                LukketPeriode("2020-01-10/2020-01-20") to ikkeOppfylt,
                LukketPeriode("2020-01-21/2020-01-31") to oppfylt
        )))
    }

}