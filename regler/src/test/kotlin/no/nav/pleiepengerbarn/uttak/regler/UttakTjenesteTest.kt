package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class UttakTjenesteTest {

    @Test
    fun `enkel uttaksperiode uten annen informasjon`() {
        val helePerioden = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        val avklarteFakta = AvklarteFakta(
                tilsynsbehov = listOf(
                        Tilsynsbehov(helePerioden, TilsynsbehovStørrelse.PROSENT_200)
                ),
                søktePerioder = listOf(
                        helePerioden
                )
        )

        val uttaksplan = UttakTjeneste.uttaksplan(avklarteFakta)

        Assertions.assertTrue(uttaksplan.perioder.size == 1)
    }

}