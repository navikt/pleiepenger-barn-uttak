package no.nav.pleiepengerbarn.uttak.server.db

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.EmptyResultDataAccessException
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.util.*

@SpringBootTest
internal class UttakRepositoryTest {

    private val heleJanuar = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
    private val heleFebruar = LukketPeriode(LocalDate.of(2020, Month.FEBRUARY, 1), LocalDate.of(2020, Month.FEBRUARY, 29))

    private val arbeidsforhold1 = ArbeidsforholdReferanse(organisasjonsnummer = "123456789")

    @Autowired
    private lateinit var uttakRepository: UttakRepository

    @Test
    internal fun `Søker etter ikke eksisterende behandling skal føre til exception`() {
        assertThrows<EmptyResultDataAccessException> {uttakRepository.hent(UUID.randomUUID())}
    }

    @Test
    internal fun `Uttaksplan kan lagres og hentes opp igjen`() {
        val behandlingId = UUID.randomUUID()

        uttakRepository.lagre("123", behandlingId, uttaksplan = dummyUttaksplan(heleJanuar), regelGrunnlag = dummyRegelGrunnlag(heleJanuar))


        val uttaksplan = uttakRepository.hent(behandlingId)
        assertThat(uttaksplan).isNotNull
        assertThat(uttaksplan).isEqualTo(dummyUttaksplan(heleJanuar))
    }

    @Test
    internal fun `Ny uttaksplan på samme behanding, skal føre til at den opprinnelig uttaksplanen blir slettet`() {
        val behandlingId = UUID.randomUUID()

        uttakRepository.lagre("123", behandlingId, uttaksplan = dummyUttaksplan(heleJanuar), regelGrunnlag = dummyRegelGrunnlag(heleJanuar))
        uttakRepository.lagre("123", behandlingId, uttaksplan = dummyUttaksplan(heleFebruar), regelGrunnlag = dummyRegelGrunnlag(heleFebruar))


        val uttaksplan = uttakRepository.hent(behandlingId)
        assertThat(uttaksplan).isNotNull
        assertThat(uttaksplan).isEqualTo(dummyUttaksplan(heleFebruar))
    }

    private fun dummyRegelGrunnlag(periode:LukketPeriode): RegelGrunnlag {
        return RegelGrunnlag(
                søker = Søker(fødselsdato = LocalDate.of(1970, Month.JANUARY, 1)),
                søknadsperioder = listOf(periode),
                tilsynsbehov = mapOf(periode to Tilsynsbehov(TilsynsbehovStørrelse.PROSENT_100)),
                arbeid = listOf(
                        Arbeidsforhold(
                                arbeidsforhold = arbeidsforhold1,
                                perioder = mapOf(
                                        periode to ArbeidsforholdPeriodeInfo(
                                                jobberNormaltPerUke = Duration.ofHours(37).plusMinutes(30),
                                                skalJobbeProsent = Prosent.ZERO)
                                )
                        )
                )
        )
    }

    private fun dummyUttaksplan(periode:LukketPeriode): Uttaksplan {
        return Uttaksplan(
                perioder = mapOf(
                        periode to InnvilgetPeriode(
                                grad = Prosent(100),
                                årsak = InnvilgetÅrsak(InnvilgetÅrsaker.FULL_DEKNING, setOf()),
                                utbetalingsgrader = listOf(Utbetalingsgrader(
                                        arbeidsforhold = arbeidsforhold1,
                                        utbetalingsgrad = Prosent(100))))
                )
        )
    }

}