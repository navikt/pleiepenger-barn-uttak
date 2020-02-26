package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.MedlemskapPeriodeInfo
import org.junit.jupiter.api.Test
import java.time.LocalDate

import org.assertj.core.api.Assertions.assertThat

internal class MedlemskapTest {

    @Test
    internal fun `Utlede perioder man ikke er medlem`() {

        val søknadsperioder = listOf(
                LukketPeriode(fom = LocalDate.parse("2020-03-14"), tom = LocalDate.parse("2020-03-25")),
                LukketPeriode(fom = LocalDate.parse("2020-02-24"), tom = LocalDate.parse("2020-02-29")),
                LukketPeriode(fom = LocalDate.parse("2020-03-05"), tom = LocalDate.parse("2020-03-10"))
        )

        val medlem = mapOf(
                LukketPeriode(fom = LocalDate.parse("2020-02-25"), tom = LocalDate.parse("2020-02-27")) to MedlemskapPeriodeInfo(frivilligMedlem = true),
                LukketPeriode(fom = LocalDate.parse("2020-03-12"), tom = LocalDate.parse("2020-03-15")) to MedlemskapPeriodeInfo(frivilligMedlem = true),
                LukketPeriode(fom = LocalDate.parse("2020-03-08"), tom = LocalDate.parse("2020-03-10")) to MedlemskapPeriodeInfo(frivilligMedlem = true),
                LukketPeriode(fom = LocalDate.parse("2020-02-28"), tom = LocalDate.parse("2020-03-01")) to MedlemskapPeriodeInfo(frivilligMedlem = true),
                LukketPeriode(fom = LocalDate.parse("2020-03-17"), tom = LocalDate.parse("2020-03-18")) to MedlemskapPeriodeInfo(frivilligMedlem = true),
                LukketPeriode(fom = LocalDate.parse("2020-03-19"), tom = LocalDate.parse("2020-03-20")) to MedlemskapPeriodeInfo(frivilligMedlem = true)
        )

        val forventedePerioderIkkeMedlem = listOf(
                LukketPeriode(fom = LocalDate.parse("2020-02-24"), tom = LocalDate.parse("2020-02-24")),
                LukketPeriode(fom = LocalDate.parse("2020-03-02"), tom = LocalDate.parse("2020-03-07")),
                LukketPeriode(fom = LocalDate.parse("2020-03-11"), tom = LocalDate.parse("2020-03-11")),
                LukketPeriode(fom = LocalDate.parse("2020-03-16"), tom = LocalDate.parse("2020-03-16")),
                LukketPeriode(fom = LocalDate.parse("2020-03-21"), tom = LocalDate.parse("2020-03-25"))
        )

        val faktiskePerioderIkkeMedlem = medlem.ikkeMedlem(søknadsperioder)

        assertThat(forventedePerioderIkkeMedlem.size).isEqualTo(faktiskePerioderIkkeMedlem.size)
        assertThat(forventedePerioderIkkeMedlem).containsAll(faktiskePerioderIkkeMedlem)
    }

    @Test
    internal fun `Utlende perioder man ikke er medlem uten noen perioder som medlem`() {
        val søknadsperioder = listOf(
                LukketPeriode(fom = LocalDate.parse("2020-03-14"), tom = LocalDate.parse("2020-03-25")),
                LukketPeriode(fom = LocalDate.parse("2020-02-24"), tom = LocalDate.parse("2020-02-29")),
                LukketPeriode(fom = LocalDate.parse("2020-03-05"), tom = LocalDate.parse("2020-03-10"))
        )

        val medlem = emptyMap<LukketPeriode, MedlemskapPeriodeInfo>()

        val faktiskePerioderIkkeMedlem = medlem.ikkeMedlem(søknadsperioder)

        val forventedePerioderIkkeMedlem = listOf(
                LukketPeriode(fom = LocalDate.parse("2020-02-24"), tom = LocalDate.parse("2020-03-25"))
        )

        assertThat(forventedePerioderIkkeMedlem.size).isEqualTo(faktiskePerioderIkkeMedlem.size)
        assertThat(forventedePerioderIkkeMedlem).containsAll(faktiskePerioderIkkeMedlem)
    }
}