package no.nav.pleiepengerbarn.uttak.server.db

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.testklient.FULL_DAG
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.util.*
import java.util.concurrent.TimeUnit

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("postgres")
@Tag("integration")
@Transactional
internal class UttakRepositoryTest {

    private companion object {
        private val heleJanuar = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        private val heleFebruar = LukketPeriode(LocalDate.of(2020, Month.FEBRUARY, 1), LocalDate.of(2020, Month.FEBRUARY, 29))
        private val heleMars = LukketPeriode(LocalDate.of(2020, Month.MARCH, 1), LocalDate.of(2020, Month.MARCH, 31))

        private val arbeidsforhold1 = Arbeidsforhold(type = "arbeidsgiver", organisasjonsnummer = "123456789")

        private const val aktørIdSøker = "123"
        private const val aktørIdBarn = "456"
    }

    @Autowired
    private lateinit var uttakRepository: UttakRepository

    @Test
    internal fun `Søker etter ikke eksisterende behandling skal føre til exception`() {
        val uttaksplan = uttakRepository.hent(UUID.randomUUID())
        assertThat(uttaksplan).isNull()
    }

    @Test
    internal fun `Uttaksplan kan lagres og hentes opp igjen`() {
        val behandlingId = UUID.randomUUID()

        val uttakJanuar = dummyUttaksplan(heleJanuar)

        uttakRepository.lagre("123", behandlingId, uttaksplan = uttakJanuar, regelGrunnlag = dummyRegelGrunnlag(heleJanuar))

        val uttaksplan = uttakRepository.hent(behandlingId)
        assertThat(uttaksplan).isNotNull
        assertThat(uttaksplan).isEqualTo(uttakJanuar)
    }

    @Test
    internal fun `Ny uttaksplan på samme behanding, skal føre til at den opprinnelig uttaksplanen blir slettet`() {
        val behandlingId = UUID.randomUUID()

        val uttakJanuar = dummyUttaksplan(heleJanuar)
        val uttakFebruar = dummyUttaksplan(heleFebruar)

        uttakRepository.lagre("123", behandlingId, uttaksplan = uttakJanuar, regelGrunnlag = dummyRegelGrunnlag(heleJanuar))
        uttakRepository.lagre("123", behandlingId, uttaksplan = uttakFebruar, regelGrunnlag = dummyRegelGrunnlag(heleFebruar))


        val uttaksplan = uttakRepository.hent(behandlingId)
        assertThat(uttaksplan).isNotNull
        assertThat(uttaksplan).isEqualTo(uttakFebruar)
    }



    @Test
    internal fun `Flere behandlinger på samme saksnummer skal hente ut med nyeste uttaksplan`() {
        val saksnummer = "123456"

        val uttakJanuar = dummyUttaksplan(heleJanuar)
        val uttakFebruar = dummyUttaksplan(heleFebruar)

        uttakRepository.lagre(saksnummer, UUID.randomUUID(), uttaksplan = uttakJanuar, regelGrunnlag = dummyRegelGrunnlag(heleJanuar))
        TimeUnit.MILLISECONDS.sleep(25L) //vent 25 ms
        uttakRepository.lagre(saksnummer, UUID.randomUUID(), uttaksplan = uttakFebruar, regelGrunnlag = dummyRegelGrunnlag(heleFebruar))


        val uttaksplan = uttakRepository.hent(saksnummer)
        assertThat(uttaksplan!!.perioder).hasSize(1)
        assertThat(uttaksplan.perioder[heleJanuar]).isNull()
        assertThat(uttaksplan.perioder[heleFebruar]).isNotNull()
    }


    @Test
    internal fun `Skal ikke finne forrige behandling når det er en behandling`() {
        val saksnummer = "123456"
        val behandlingUUID1 = UUID.randomUUID()

        val uttakJanuar = dummyUttaksplan(heleJanuar)

        uttakRepository.lagre(saksnummer, behandlingUUID1, uttaksplan = uttakJanuar, regelGrunnlag = dummyRegelGrunnlag(heleJanuar))

        val forrigeUttaksplan = uttakRepository.hentForrige(saksnummer, behandlingUUID1)

        assertThat(forrigeUttaksplan).isNull()
    }

    @Test
    internal fun `Skal finne forrige behandling når det er to behandlinger`() {
        val saksnummer = "123456"
        val behandlingUUID1 = UUID.randomUUID()
        val behandlingUUID2 = UUID.randomUUID()

        val uttakJanuar = dummyUttaksplan(heleJanuar)
        val uttakFebruar = dummyUttaksplan(heleFebruar)

        uttakRepository.lagre(saksnummer, behandlingUUID1, uttaksplan = uttakJanuar, regelGrunnlag = dummyRegelGrunnlag(heleJanuar))
        TimeUnit.MILLISECONDS.sleep(25L) //vent 25 ms
        uttakRepository.lagre(saksnummer, behandlingUUID2, uttaksplan = uttakFebruar, regelGrunnlag = dummyRegelGrunnlag(heleFebruar))

        val forrigeUttaksplan = uttakRepository.hentForrige(saksnummer, behandlingUUID2)

        assertThat(forrigeUttaksplan).isNotNull()
        assertThat(forrigeUttaksplan).isEqualTo(uttakJanuar)
    }

    @Test
    internal fun `Skal finne forrige behandling når det er tre behandlinger`() {
        val saksnummer = "123456"
        val behandlingUUID1 = UUID.randomUUID()
        val behandlingUUID2 = UUID.randomUUID()
        val behandlingUUID3 = UUID.randomUUID()

        val uttakJanuar = dummyUttaksplan(heleJanuar)
        val uttakFebruar = dummyUttaksplan(heleFebruar)
        val uttakMars = dummyUttaksplan(heleMars)

        uttakRepository.lagre(saksnummer, behandlingUUID1, uttaksplan = uttakJanuar, regelGrunnlag = dummyRegelGrunnlag(heleJanuar))
        TimeUnit.MILLISECONDS.sleep(25L) //vent 25 ms
        uttakRepository.lagre(saksnummer, behandlingUUID2, uttaksplan = uttakFebruar, regelGrunnlag = dummyRegelGrunnlag(heleFebruar))
        TimeUnit.MILLISECONDS.sleep(25L) //vent 25 ms
        uttakRepository.lagre(saksnummer, behandlingUUID3, uttaksplan = uttakMars, regelGrunnlag = dummyRegelGrunnlag(heleMars))

        val forrigeUttaksplan = uttakRepository.hentForrige(saksnummer, behandlingUUID3)

        assertThat(forrigeUttaksplan).isNotNull()
        assertThat(forrigeUttaksplan).isEqualTo(uttakFebruar)
    }

    @Test
    internal fun `Skal finne forrige behandling når det ikke er registrert noen uttaksplan på nåværende behandling`() {
        val saksnummer = "123456"
        val behandlingUUID1 = UUID.randomUUID()

        val uttakJanuar = dummyUttaksplan(heleJanuar)

        uttakRepository.lagre(saksnummer, behandlingUUID1, uttaksplan = uttakJanuar, regelGrunnlag = dummyRegelGrunnlag(heleJanuar))

        val forrigeUttaksplan = uttakRepository.hentForrige(saksnummer, UUID.randomUUID())

        assertThat(forrigeUttaksplan).isNotNull()
        assertThat(forrigeUttaksplan).isEqualTo(uttakJanuar)
    }

    @Test
    internal fun `Skal ikke finne forrige behandling når det ikke er registrert noen uttaksplan på saken`() {
        val saksnummer = "123456"

        val forrigeUttaksplan = uttakRepository.hentForrige(saksnummer, UUID.randomUUID())

        assertThat(forrigeUttaksplan).isNull()
    }

    private fun dummyRegelGrunnlag(periode:LukketPeriode): RegelGrunnlag {
        return RegelGrunnlag(
                behandlingUUID = UUID.randomUUID().toString(),
                søker = Søker(
                    aktørId = aktørIdSøker
                ),
                barn = Barn(
                    aktørId = aktørIdBarn
                ),
                søktUttak = listOf(SøktUttak(periode)),
                pleiebehov = mapOf(periode to Pleiebehov.PROSENT_100),
                arbeid = listOf(
                        Arbeid(
                                arbeidsforhold = arbeidsforhold1,
                                perioder = mapOf(
                                        periode to ArbeidsforholdPeriodeInfo(
                                                jobberNormalt = Duration.ofHours(7).plusMinutes(30),
                                                jobberNå = Duration.ofHours(7).plusMinutes(30)
                                        )
                                )
                        )
                )
        )
    }

    private fun dummyUttaksplan(periode:LukketPeriode): Uttaksplan {
        return Uttaksplan(
                perioder = mapOf(
                        periode to UttaksperiodeInfo.oppfylt(
                                kildeBehandlingUUID = UUID.randomUUID().toString(),
                                uttaksgrad = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                                årsak = Årsak.FULL_DEKNING,
                                pleiebehov = Pleiebehov.PROSENT_200.prosent.setScale(2, RoundingMode.HALF_UP),
                                knekkpunktTyper = setOf(),
                                utbetalingsgrader = listOf(Utbetalingsgrader(
                                        arbeidsforhold = arbeidsforhold1,
                                        utbetalingsgrad = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                                        normalArbeidstid = FULL_DAG,
                                        faktiskArbeidstid = Duration.ZERO)),
                                søkersTapteArbeidstid = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                                oppgittTilsyn = null,
                                annenPart = AnnenPart.ALENE,
                                nattevåk = null,
                                beredskap = null)

                )
        )
    }

}