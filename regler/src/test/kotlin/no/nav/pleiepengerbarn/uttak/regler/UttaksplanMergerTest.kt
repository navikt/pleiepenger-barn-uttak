package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class UttaksplanMergerTest {

    private val arbeidsforhold1:ArbeidsforholdRef = UUID.randomUUID().toString()

    private val hundreProsent = Prosent(100)
    private val innvilget = InnvilgetPeriode(
            grad = hundreProsent,
            årsak = InnvilgetÅrsak(InnvilgetÅrsaker.AvkortetMotInntekt, setOf()),
            utbetalingsgrader = mapOf(arbeidsforhold1 to hundreProsent))

    @Test
    fun `En uttaksplan med en uttaksperiode skal bli til samme uttaksplan`() {

        val enkelUttakplan =  Uttaksplan(
                mapOf(LukketPeriode("2020-01-01/2020-01-31") to innvilget)
        )

        val sammenslåttUttaksplan = UttaksplanMerger.slåSammenUttaksplaner(listOf(enkelUttakplan))

        assertThat(sammenslåttUttaksplan).isEqualTo(enkelUttakplan)
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

        assertThat(sammenslåttUttaksplan).isEqualTo(Uttaksplan(mapOf(
                LukketPeriode("2020-01-01/2020-01-14") to innvilget,
                LukketPeriode("2020-01-15/2020-02-10") to innvilget
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

        assertThat(sammenslåttUttaksplan).isEqualTo(Uttaksplan(mapOf(
                LukketPeriode("2020-01-01/2020-01-15") to innvilget,
                LukketPeriode("2020-01-16/2020-01-24") to innvilget,
                LukketPeriode("2020-01-25/2020-02-15") to innvilget
        )))
    }



}