package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*


internal class SimuleringTjenesteTest {

    private companion object {
        private val INGENTING = Duration.ZERO
        private val ARBEIDSGIVER1 = Arbeidsforhold("AT", "123456789")
    }

    @Test
    fun `To tomme uttaksplaner skal gi ingen endring`() {
        val forrigeUttaksplan = Uttaksplan()
        val simulertUttaksplan = Uttaksplan()

        val resultatEndret = SimuleringTjeneste.erResultatEndret(forrigeUttaksplan, simulertUttaksplan)

        Assertions.assertThat(resultatEndret).isFalse
    }

    @Test
    fun `Forrige uttaksplan er null skal gi endring`() {
        val simulertUttaksplan = Uttaksplan()

        val resultatEndret = SimuleringTjeneste.erResultatEndret(null, simulertUttaksplan)

        Assertions.assertThat(resultatEndret).isTrue
    }

    @Test
    fun `Like perioder skal gi ingen endring`() {
        val forrigeUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt(),
                LukketPeriode("2020-10-12/2020-10-16") to oppfylt(),
                LukketPeriode("2020-10-19/2020-10-23") to oppfylt(),
                LukketPeriode("2020-10-26/2020-10-30") to oppfylt(),
            )
        )
        val simulertUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt(),
                LukketPeriode("2020-10-12/2020-10-16") to oppfylt(),
                LukketPeriode("2020-10-19/2020-10-23") to oppfylt(),
                LukketPeriode("2020-10-26/2020-10-30") to oppfylt(),
            )
        )

        val resultatEndret = SimuleringTjeneste.erResultatEndret(forrigeUttaksplan, simulertUttaksplan)

        Assertions.assertThat(resultatEndret).isFalse
    }

    @Test
    fun `Forskjellige perioder skal gi endring`() {
        val forrigeUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-01/2020-10-02") to oppfylt(),
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt(),
                LukketPeriode("2020-10-12/2020-10-16") to oppfylt(),
                LukketPeriode("2020-10-19/2020-10-23") to oppfylt(),
                LukketPeriode("2020-10-26/2020-10-30") to oppfylt(),
            )
        )
        val simulertUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt(),
                LukketPeriode("2020-10-12/2020-10-16") to oppfylt(),
                LukketPeriode("2020-10-19/2020-10-23") to oppfylt(),
                LukketPeriode("2020-10-26/2020-10-30") to oppfylt(),
            )
        )

        val resultatEndret = SimuleringTjeneste.erResultatEndret(forrigeUttaksplan, simulertUttaksplan)

        Assertions.assertThat(resultatEndret).isTrue
    }

    @Test
    fun `Forskjellige perioder som kan slås sammen skal gi ikke endring`() {
        val forrigeUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt(),
                LukketPeriode("2020-10-12/2020-10-16") to oppfylt(),
                LukketPeriode("2020-10-19/2020-10-23") to oppfylt(),
                LukketPeriode("2020-10-26/2020-10-30") to oppfylt(),
            )
        )
        val simulertUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt(),
                LukketPeriode("2020-10-12/2020-10-16") to oppfylt(),
                LukketPeriode("2020-10-19/2020-10-20") to oppfylt(),
                LukketPeriode("2020-10-21/2020-10-23") to oppfylt(),
                LukketPeriode("2020-10-26/2020-10-30") to oppfylt(),
            )
        )

        val resultatEndret = SimuleringTjeneste.erResultatEndret(forrigeUttaksplan, simulertUttaksplan)

        Assertions.assertThat(resultatEndret).isFalse
    }


    @Test
    fun `Forskjellige perioder som ikke kan slås sammen pga forskjellig info skal gi endring`() {
        val forrigeUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt(),
                LukketPeriode("2020-10-12/2020-10-16") to oppfylt(),
                LukketPeriode("2020-10-19/2020-10-23") to oppfylt(),
                LukketPeriode("2020-10-26/2020-10-30") to oppfylt(),
            )
        )
        val simulertUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt(),
                LukketPeriode("2020-10-12/2020-10-16") to oppfylt(),
                LukketPeriode("2020-10-19/2020-10-20") to ikkeOppfylt(Årsak.FOR_LAV_ØNSKET_UTTAKSGRAD),
                LukketPeriode("2020-10-21/2020-10-23") to oppfylt(),
                LukketPeriode("2020-10-26/2020-10-30") to oppfylt(),
            )
        )

        val resultatEndret = SimuleringTjeneste.erResultatEndret(forrigeUttaksplan, simulertUttaksplan)

        Assertions.assertThat(resultatEndret).isTrue
    }


    @Test
    fun `Samme periode med forskjellig uttaksgrad skal gi endring`() {
        val forrigeUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt()
            )
        )
        val simulertUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt().copy(uttaksgrad = Prosent(50))
            )
        )

        val resultatEndret = SimuleringTjeneste.erResultatEndret(forrigeUttaksplan, simulertUttaksplan)

        Assertions.assertThat(resultatEndret).isTrue
    }

    @Test
    fun `Samme periode med forskjellig utbetalingsgrad skal gi endring`() {
        val forrigeUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt()
            )
        )
        val simulertUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt().copy(utbetalingsgrader = listOf(Utbetalingsgrader(ARBEIDSGIVER1, FULL_DAG, FULL_DAG.prosent(50), Prosent(50))))
            )
        )

        val resultatEndret = SimuleringTjeneste.erResultatEndret(forrigeUttaksplan, simulertUttaksplan)

        Assertions.assertThat(resultatEndret).isTrue
    }

    @Test
    fun `Samme periode med forskjellig utfall skal gi endring`() {
        val forrigeUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to oppfylt()
            )
        )
        val simulertUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to ikkeOppfylt(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE)
            )
        )

        val resultatEndret = SimuleringTjeneste.erResultatEndret(forrigeUttaksplan, simulertUttaksplan)

        Assertions.assertThat(resultatEndret).isTrue
    }

    @Test
    fun `Samme perioder med forskjellige ikke oppfylt årsaker skal gi endring`() {
        val forrigeUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to ikkeOppfylt(Årsak.FOR_LAV_ØNSKET_UTTAKSGRAD)
            )
        )
        val simulertUttaksplan = Uttaksplan(
            perioder = mapOf(
                LukketPeriode("2020-10-05/2020-10-09") to ikkeOppfylt(Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE)
            )
        )

        val resultatEndret = SimuleringTjeneste.erResultatEndret(forrigeUttaksplan, simulertUttaksplan)

        Assertions.assertThat(resultatEndret).isTrue
    }

    private fun oppfylt(): UttaksperiodeInfo {
        return UttaksperiodeInfo.oppfylt(
            uttaksgrad = HUNDRE_PROSENT,
            uttaksgradUtenReduksjonGrunnetInntektsgradering = HUNDRE_PROSENT,
            uttaksgradMedReduksjonGrunnetInntektsgradering = null,
            utbetalingsgrader = listOf(Utbetalingsgrader(ARBEIDSGIVER1, FULL_DAG, INGENTING, HUNDRE_PROSENT)),
            søkersTapteArbeidstid = HUNDRE_PROSENT,
            oppgittTilsyn = null,
            årsak = Årsak.FULL_DEKNING,
            pleiebehov = Pleiebehov.PROSENT_100.prosent,
            graderingMotTilsyn = null,
            knekkpunktTyper = setOf(),
            kildeBehandlingUUID = UUID.randomUUID().toString(),
            annenPart = AnnenPart.ALENE,
            nattevåk = null,
            beredskap = null,
            utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN)
        )
    }

    private fun ikkeOppfylt(vararg årsaker: Årsak): UttaksperiodeInfo {
        return UttaksperiodeInfo.ikkeOppfylt(
            utbetalingsgrader = listOf(Utbetalingsgrader(ARBEIDSGIVER1, FULL_DAG, INGENTING, Prosent.ZERO)),
            søkersTapteArbeidstid = HUNDRE_PROSENT,
            oppgittTilsyn = null,
            årsaker = årsaker.toSet(),
            pleiebehov = Pleiebehov.PROSENT_100.prosent,
            graderingMotTilsyn = null,
            knekkpunktTyper = setOf(),
            kildeBehandlingUUID = UUID.randomUUID().toString(),
            annenPart = AnnenPart.ALENE,
            nattevåk = null,
            beredskap = null,
            utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN)
        )

    }

}
