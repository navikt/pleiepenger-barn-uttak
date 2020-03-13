package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class UttaksplanMergerTest {

    private val arbeidsforhold1 = UUID.randomUUID().toString()

    private val hundreProsent = Prosent(100)
    private val innvilget = InnvilgetPeriode(
            grad = hundreProsent,
            årsak = InnvilgetÅrsak(InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT, setOf()),
            utbetalingsgrader = mapOf(arbeidsforhold1 to hundreProsent).somUtbetalingsgrader())

    private val avslått = AvslåttPeriode(
            årsaker = setOf(AvslåttÅrsak(AvslåttÅrsaker.FOR_LAV_GRAD, setOf()))
    )

    @Test
    fun `En uttaksplan med en uttaksperiode skal bli til samme uttaksplan`() {

        val enkelUttakplan =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-01/2020-01-31") to innvilget)
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(enkelUttakplan))

        assertThat(sammenslåttUttaksplan).isEqualTo(FullUttaksplan(enkelUttakplan.perioder))
    }

    @Test
    fun `En uttaksplan med delvis overlapp en annen uttaksplan skal slås sammen`() {

        val uttaksplan1 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-01/2020-01-31") to innvilget)
        )
        val uttaksplan2 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-15/2020-02-10") to innvilget)
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(uttaksplan2, uttaksplan1))

        assertThat(sammenslåttUttaksplan).isEqualTo(FullUttaksplan(mapOf(
                LukketPeriode("2020-01-01/2020-01-14") to innvilget,
                LukketPeriode("2020-01-15/2020-02-10") to innvilget
        )))
    }

    @Test
    fun `En uttaksplan med delvis overlapp en annen uttaksplan(første uttaksperioder i nyeste uttaksplan) skal slås sammen`() {

        val uttaksplan1 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-01/2020-02-10") to innvilget)
        )
        val uttaksplan2 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-17/2020-03-31") to innvilget)
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(uttaksplan2, uttaksplan1))

        assertThat(sammenslåttUttaksplan).isEqualTo(FullUttaksplan(mapOf(
                LukketPeriode("2020-01-01/2020-01-16") to innvilget,
                LukketPeriode("2020-01-17/2020-03-31") to innvilget
        )))
    }

    @Test
    fun `En uttaksplan med hull, skal føre til at underliggende uttaksplan skal brukes i hullet`() {

        val uttaksplan1 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-01/2020-01-31") to innvilget)
        )
        val uttaksplan2 =  Uttaksplan(
                mapOf(
                        LukketPeriode("2020-01-01/2020-01-15") to innvilget,
                        LukketPeriode("2020-01-25/2020-02-15") to innvilget
                )
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(uttaksplan2, uttaksplan1))

        assertThat(sammenslåttUttaksplan).isEqualTo(FullUttaksplan(mapOf(
                LukketPeriode("2020-01-01/2020-01-15") to innvilget,
                LukketPeriode("2020-01-16/2020-01-24") to innvilget,
                LukketPeriode("2020-01-25/2020-02-15") to innvilget
        )))
    }

    @Test
    fun `En tidligere uttaksplan med innvilget periode og en nyere uttaksplan med et avslag, skal føre til at opprinne uttaksperioder blir splittet`() {

        val uttaksplan1 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-10/2020-01-20") to avslått)
        )
        val uttaksplan2 =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-01/2020-01-31") to innvilget)
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(uttaksplan1, uttaksplan2))

        assertThat(sammenslåttUttaksplan).isEqualTo(FullUttaksplan(mapOf(
                LukketPeriode("2020-01-01/2020-01-09") to innvilget,
                LukketPeriode("2020-01-10/2020-01-20") to avslått,
                LukketPeriode("2020-01-21/2020-01-31") to innvilget
        )))
    }

}