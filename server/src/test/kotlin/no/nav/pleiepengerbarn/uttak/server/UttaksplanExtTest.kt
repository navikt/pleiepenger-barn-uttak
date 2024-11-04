package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.NULL_PROSENT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UttaksplanExtTest {

    @Test
    internal fun `Bare helg eller ingen dager mellom to datoer`() {
        assertThat(bareHelgEllerIngenDagerMellom(LocalDate.parse("2021-08-06"), LocalDate.parse("2021-08-09"))).isTrue
        assertThat(bareHelgEllerIngenDagerMellom(LocalDate.parse("2021-08-05"), LocalDate.parse("2021-08-09"))).isFalse
        assertThat(bareHelgEllerIngenDagerMellom(LocalDate.parse("2021-08-06"), LocalDate.parse("2021-08-10"))).isFalse
        assertThat(bareHelgEllerIngenDagerMellom(LocalDate.parse("2021-08-05"), LocalDate.parse("2021-08-10"))).isFalse
        assertThat(bareHelgEllerIngenDagerMellom(LocalDate.parse("2021-08-07"), LocalDate.parse("2021-08-10"))).isFalse

        assertThat(bareHelgEllerIngenDagerMellom(LocalDate.parse("2021-11-15"), LocalDate.parse("2021-11-16"))).isTrue
        assertThat(bareHelgEllerIngenDagerMellom(LocalDate.parse("2021-11-15"), LocalDate.parse("2021-11-17"))).isFalse
    }

    @Test
    internal fun `Tre sammenhengende perioder skal slås sammen`() {
        val periode1 = LukketPeriode("2021-08-09/2021-08-13")
        val periode2 = LukketPeriode("2021-08-16/2021-08-20")
        val periode3 = LukketPeriode("2021-08-23/2021-08-27")
        val uttaksplan = Uttaksplan(perioder = mapOf(
            periode1 to dummyUttaksperiodeInfo(),
            periode2 to dummyUttaksperiodeInfo(),
            periode3 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val nyUttaksplan = uttaksplan.slåSammenLikePerioder()

        assertThat(nyUttaksplan.perioder.keys).hasSize(1)
        assertThat(nyUttaksplan.perioder[LukketPeriode("2021-08-09/2021-08-27")]).isEqualTo(dummyUttaksperiodeInfo())
    }

    @Test
    internal fun `Tre ikke sammenhengende perioder skal ikke slås sammen`() {
        val periode1 = LukketPeriode("2021-08-09/2021-08-12")
        val periode2 = LukketPeriode("2021-08-16/2021-08-19")
        val periode3 = LukketPeriode("2021-08-23/2021-08-26")
        val uttaksplan = Uttaksplan(perioder = mapOf(
            periode1 to dummyUttaksperiodeInfo(),
            periode2 to dummyUttaksperiodeInfo(),
            periode3 to dummyUttaksperiodeInfo()
        ), trukketUttak = listOf())

        val nyUttaksplan = uttaksplan.slåSammenLikePerioder()

        assertThat(nyUttaksplan).isEqualTo(uttaksplan)
    }

    private fun dummyUttaksperiodeInfo() = UttaksperiodeInfo(
        utfall = Utfall.OPPFYLT,
        utbetalingsgrader = listOf(),
        annenPart = AnnenPart.ALENE,
        beredskap = null,
        nattevåk = null,
        graderingMotTilsyn = GraderingMotTilsyn(
            etablertTilsyn = NULL_PROSENT,
            overseEtablertTilsynÅrsak = null,
            andreSøkeresTilsyn = NULL_PROSENT,
            tilgjengeligForSøker = NULL_PROSENT,
            andreSøkeresTilsynReberegnet = false
        ),
        kildeBehandlingUUID = "123",
        oppgittTilsyn = null,
        pleiebehov = Pleiebehov.PROSENT_100.prosent,
        søkersTapteArbeidstid = null,
        uttaksgrad = HUNDRE_PROSENT,
        årsaker = setOf(),
        uttaksgradUtenReduksjonGrunnetInntektsgradering = null,
        uttaksgradMedReduksjonGrunnetInntektsgradering = null
    )


}
