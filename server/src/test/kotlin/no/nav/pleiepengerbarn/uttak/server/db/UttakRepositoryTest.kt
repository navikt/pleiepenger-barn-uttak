package no.nav.pleiepengerbarn.uttak.server.db

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.testklient.FULL_DAG
import no.nav.pleiepengerbarn.uttak.testklient.nesteSaksnummer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
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
        private val heleJanuar =
            LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31))
        private val heleFebruar =
            LukketPeriode(LocalDate.of(2020, Month.FEBRUARY, 1), LocalDate.of(2020, Month.FEBRUARY, 29))
        private val heleMars = LukketPeriode(LocalDate.of(2020, Month.MARCH, 1), LocalDate.of(2020, Month.MARCH, 31))

        private val arbeidsforhold1 = Arbeidsforhold(type = "AT", organisasjonsnummer = "123456789")

        private const val aktørIdSøker = "123"
        private const val aktørIdBarn = "456"
    }

    @Autowired
    private lateinit var uttakRepository: UttakRepository

    @Test
    internal fun `Ikke oppfylt uttaksplan skal lagre informasjon om utenlandsperioder`() {
        val behandlingUUID = UUID.randomUUID()

        val landkode = "CAN"
        val utenlandsoppholdÅrsak = UtenlandsoppholdÅrsak.INGEN
        val grunnlagJanuar = dummyRegelGrunnlag(heleJanuar, behandlingUUID)
        val uttakJanuar = dummyIkkeOppfyltUttaksplanMedUtenlandsopphold(heleJanuar, landkode, utenlandsoppholdÅrsak)
        uttakRepository.lagre(uttaksplan = uttakJanuar, regelGrunnlag = grunnlagJanuar)
        val uttaksplan = uttakRepository.hent(behandlingUUID)
        assertThat(uttaksplan).isNotNull
        // Krever at landkodene og utenlandsoppholdårsaken er som før.
        uttaksplan!!.perioder.values.forEach { it -> assertThat(it.utenlandsopphold?.landkode).isEqualTo(landkode) }
        uttaksplan.perioder.values.forEach { it -> assertThat(it.utenlandsopphold?.årsak).isEqualTo(utenlandsoppholdÅrsak) }
    }

    @Test
    internal fun `Ikke oppfylt uttaksplan skal lagre informasjon om utenlandsperioder med gyldig utenlandsoppholdÅrsak`() {
        val behandlingUUID = UUID.randomUUID()

        val landkode = "CAN"
        val utenlandsoppholdÅrsak =
            UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD
        val grunnlagJanuar = dummyRegelGrunnlag(heleJanuar, behandlingUUID)
        val uttakJanuar = dummyIkkeOppfyltUttaksplanMedUtenlandsopphold(heleJanuar, landkode, utenlandsoppholdÅrsak)
        uttakRepository.lagre(uttaksplan = uttakJanuar, regelGrunnlag = grunnlagJanuar)
        val uttaksplan = uttakRepository.hent(behandlingUUID)
        assertThat(uttaksplan).isNotNull
        // Krever at landkodene og utenlandsoppholdårsaken er som før.
        uttaksplan!!.perioder.values.forEach { it -> assertThat(it.utenlandsopphold?.landkode).isEqualTo(landkode) }
        uttaksplan.perioder.values.forEach { it ->
            assertThat(it.utenlandsopphold?.årsak).isEqualTo(
                utenlandsoppholdÅrsak
            )
        }
    }

    @Test
    internal fun `Oppfylt uttaksplan skal lagre informasjon om utenlandsperioder`() {
        val behandlingUUID = UUID.randomUUID()

        val landkode = "CAN"
        val utenlandsoppholdÅrsak =
            UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD
        val grunnlagJanuar = dummyRegelGrunnlag(heleJanuar, behandlingUUID)
        val uttakJanuar = dummyUttaksplanMedUtenlandsopphold(heleJanuar, landkode, utenlandsoppholdÅrsak)
        uttakRepository.lagre(uttaksplan = uttakJanuar, regelGrunnlag = grunnlagJanuar)
        val uttaksplan = uttakRepository.hent(behandlingUUID)
        assertThat(uttaksplan).isNotNull
        // Krever at landkodene og utenlandsoppholdårsaken er som før.
        uttaksplan!!.perioder.values.forEach { it -> assertThat(it.utenlandsopphold?.landkode).isEqualTo(landkode) }
        uttaksplan!!.perioder.values.forEach { it ->
            assertThat(it.utenlandsopphold?.årsak).isEqualTo(
                utenlandsoppholdÅrsak
            )
        }
    }

    @Test
    internal fun `Ikke oppfylt uttaksplan uten informasjon om utenlandsperioder skal lagres med standardverdier`() {
        val behandlingUUID = UUID.randomUUID()

        val grunnlagJanuar = dummyRegelGrunnlag(heleJanuar, behandlingUUID)
        val uttakJanuar = dummyIkkeOppfyltUttaksplan(heleJanuar)
        uttakRepository.lagre(uttaksplan = uttakJanuar, regelGrunnlag = grunnlagJanuar)
        val uttaksplan = uttakRepository.hent(behandlingUUID)
        assertThat(uttaksplan).isNotNull
        // Krever at landkoden er null og utenlandsoppholdårsaken er INGEN.
        uttaksplan!!.perioder.values.forEach { it -> assertThat(it.utenlandsopphold?.landkode).isNull() }
        uttaksplan.perioder.values.forEach { it ->
            assertThat(it.utenlandsopphold?.årsak)
                .isEqualTo(UtenlandsoppholdÅrsak.INGEN)
        }
    }

    @Test
    internal fun `Oppfylt uttaksplan uten informasjon om utenlandsperioder skal lagres med standardverdier`() {
        val behandlingUUID = UUID.randomUUID()

        val grunnlagJanuar = dummyRegelGrunnlag(heleJanuar, behandlingUUID)
        val uttakJanuar = dummyUttaksplan(heleJanuar)
        uttakRepository.lagre(uttaksplan = uttakJanuar, regelGrunnlag = grunnlagJanuar)
        val uttaksplan = uttakRepository.hent(behandlingUUID)
        assertThat(uttaksplan).isNotNull
        // Krever at landkoden er null og utenlandsoppholdårsaken er INGEN.
        uttaksplan!!.perioder.values.forEach { it -> assertThat(it.utenlandsopphold?.landkode).isNull() }
        uttaksplan.perioder.values.forEach { it ->
            assertThat(it.utenlandsopphold?.årsak)
                .isEqualTo(UtenlandsoppholdÅrsak.INGEN)
        }
    }

    @Test
    internal fun `Søker etter ikke eksisterende behandling skal føre til exception`() {
        val uttaksplan = uttakRepository.hent(UUID.randomUUID())
        assertThat(uttaksplan).isNull()
    }

    @Test
    internal fun `Uttaksplan kan lagres og hentes opp igjen`() {
        val uttakJanuar = dummyUttaksplan(heleJanuar).copy(commitId = "12345")
        val grunnlag = dummyRegelGrunnlag(heleJanuar).copy(commitId = "12345")

        uttakRepository.lagre(uttaksplan = uttakJanuar, regelGrunnlag = grunnlag)

        val uttaksplan = uttakRepository.hent(grunnlag.behandlingUUID)
        assertThat(uttaksplan).isNotNull
        assertThat(uttaksplan).isEqualTo(uttakJanuar)
    }

    @Test
    internal fun `Livets sluttfase - Uttaksplan kan lagres og hentes opp igjen, med kvoteinfo`() {
        val kvoteinfo = KvoteInfo(
            maxDato = LocalDate.of(2020, 1, 2),
                totaltForbruktKvote = BigDecimal.valueOf(23).setScale(2)
        )
        val uttakJanuar = dummyUttaksplanPLS(heleJanuar, kvoteinfo)
        val grunnlag = dummyRegelGrunnlagPLS(heleJanuar)

        uttakRepository.lagre(uttaksplan = uttakJanuar, regelGrunnlag = grunnlag)

        val uttaksplan = uttakRepository.hent(grunnlag.behandlingUUID)
        assertThat(uttaksplan).isNotNull
        assertThat(uttaksplan).isEqualTo(uttakJanuar)
    }

    @Test
    internal fun `Livets sluttfase - Uttaksplan kan lagres og hentes opp igjen, med maxDato null`() {
        val kvoteinfo = KvoteInfo(
            maxDato = null,
                totaltForbruktKvote = BigDecimal.ZERO.setScale(2)
        )
        val uttakJanuar = dummyUttaksplanPLS(heleJanuar, kvoteinfo)
        val grunnlag = dummyRegelGrunnlagPLS(heleJanuar)

        uttakRepository.lagre(uttaksplan = uttakJanuar, regelGrunnlag = grunnlag)

        val uttaksplan = uttakRepository.hent(grunnlag.behandlingUUID)
        assertThat(uttaksplan).isNotNull
        assertThat(uttaksplan).isEqualTo(uttakJanuar)
    }

    @Test
    internal fun `Livets sluttfase - Uttaksplan kan lagres og hentes opp igjen, med kvoteinfo null`() {
        val uttakJanuar = dummyUttaksplanPLS(heleJanuar, null)
        val grunnlag = dummyRegelGrunnlagPLS(heleJanuar)

        uttakRepository.lagre(uttaksplan = uttakJanuar, regelGrunnlag = grunnlag)

        val uttaksplan = uttakRepository.hent(grunnlag.behandlingUUID)
        assertThat(uttaksplan).isNotNull
        assertThat(uttaksplan).isEqualTo(uttakJanuar)
    }

    @Test
    internal fun `Ny uttaksplan på samme behanding, skal føre til at den opprinnelig uttaksplanen blir slettet`() {
        val behandlingUUID = UUID.randomUUID()

        val grunnlagJanuar = dummyRegelGrunnlag(heleJanuar, behandlingUUID)
        val grunnlagFebruar = dummyRegelGrunnlag(heleFebruar, behandlingUUID)
        val uttakJanuar = dummyUttaksplan(heleJanuar)
        val uttakFebruar = dummyUttaksplan(heleFebruar)

        uttakRepository.lagre(uttaksplan = uttakJanuar, regelGrunnlag = grunnlagJanuar)
        uttakRepository.lagre(uttaksplan = uttakFebruar, regelGrunnlag = grunnlagFebruar)


        val uttaksplan = uttakRepository.hent(behandlingUUID)
        assertThat(uttaksplan).isNotNull
        assertThat(uttaksplan).isEqualTo(uttakFebruar)
    }


    @Test
    internal fun `Flere behandlinger på samme saksnummer skal hente ut med nyeste uttaksplan`() {
        val saksnummer = "123456"

        val uttakJanuar = dummyUttaksplan(heleJanuar)
        val uttakFebruar = dummyUttaksplan(heleFebruar)

        uttakRepository.lagre(
            uttaksplan = uttakJanuar,
            regelGrunnlag = dummyRegelGrunnlag(heleJanuar).copy(
                saksnummer = saksnummer,
                behandlingUUID = UUID.randomUUID()
            )
        )
        TimeUnit.MILLISECONDS.sleep(25L) //vent 25 ms
        uttakRepository.lagre(
            uttaksplan = uttakFebruar,
            regelGrunnlag = dummyRegelGrunnlag(heleFebruar).copy(
                saksnummer = saksnummer,
                behandlingUUID = UUID.randomUUID()
            )
        )


        val uttaksplan = uttakRepository.hent(saksnummer)
        assertThat(uttaksplan!!.perioder).hasSize(1)
        assertThat(uttaksplan.perioder[heleJanuar]).isNull()
        assertThat(uttaksplan.perioder[heleFebruar]).isNotNull()
    }


    @Test
    internal fun `Skal ikke finne forrige behandling når det er en behandling`() {
        val saksnummer = "123456"
        val behandlingUUID = UUID.randomUUID()

        val uttakJanuar = dummyUttaksplan(heleJanuar)

        uttakRepository.lagre(
            uttaksplan = uttakJanuar,
            regelGrunnlag = dummyRegelGrunnlag(heleJanuar).copy(
                saksnummer = saksnummer,
                behandlingUUID = behandlingUUID
            )
        )

        val forrigeUttaksplan = uttakRepository.hentForrige(saksnummer, behandlingUUID)

        assertThat(forrigeUttaksplan).isNull()
    }

    @Test
    internal fun `Skal finne forrige behandling når det er to behandlinger`() {
        val saksnummer = "123456"
        val behandlingUUID1 = UUID.randomUUID()
        val behandlingUUID2 = UUID.randomUUID()

        val uttakJanuar = dummyUttaksplan(heleJanuar)
        val uttakFebruar = dummyUttaksplan(heleFebruar)

        uttakRepository.lagre(
            uttaksplan = uttakJanuar,
            regelGrunnlag = dummyRegelGrunnlag(heleJanuar).copy(
                saksnummer = saksnummer,
                behandlingUUID = behandlingUUID1
            )
        )
        TimeUnit.MILLISECONDS.sleep(25L) //vent 25 ms
        uttakRepository.lagre(
            uttaksplan = uttakFebruar,
            regelGrunnlag = dummyRegelGrunnlag(heleFebruar).copy(
                saksnummer = saksnummer,
                behandlingUUID = behandlingUUID2
            )
        )

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

        uttakRepository.lagre(
            uttaksplan = uttakJanuar,
            regelGrunnlag = dummyRegelGrunnlag(heleJanuar).copy(
                saksnummer = saksnummer,
                behandlingUUID = behandlingUUID1
            )
        )
        TimeUnit.MILLISECONDS.sleep(25L) //vent 25 ms
        uttakRepository.lagre(
            uttaksplan = uttakFebruar,
            regelGrunnlag = dummyRegelGrunnlag(heleFebruar).copy(
                saksnummer = saksnummer,
                behandlingUUID = behandlingUUID2
            )
        )
        TimeUnit.MILLISECONDS.sleep(25L) //vent 25 ms
        uttakRepository.lagre(
            uttaksplan = uttakMars,
            regelGrunnlag = dummyRegelGrunnlag(heleMars).copy(saksnummer = saksnummer, behandlingUUID = behandlingUUID3)
        )

        val forrigeUttaksplan = uttakRepository.hentForrige(saksnummer, behandlingUUID3)

        assertThat(forrigeUttaksplan).isNotNull()
        assertThat(forrigeUttaksplan).isEqualTo(uttakFebruar)
    }

    @Test
    internal fun `Skal finne forrige behandling når det ikke er registrert noen uttaksplan på nåværende behandling`() {
        val saksnummer = "123456"

        val uttakJanuar = dummyUttaksplan(heleJanuar)

        uttakRepository.lagre(
            uttaksplan = uttakJanuar,
            regelGrunnlag = dummyRegelGrunnlag(heleJanuar).copy(saksnummer = saksnummer)
        )

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

    private fun dummyRegelGrunnlag(periode: LukketPeriode, behandlingUUID: UUID = UUID.randomUUID()): RegelGrunnlag {
        return RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            behandlingUUID = behandlingUUID,
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

    private fun dummyUttaksplan(periode: LukketPeriode): Uttaksplan {
        return Uttaksplan(
            perioder = mapOf(
                periode to UttaksperiodeInfo.oppfylt(
                    kildeBehandlingUUID = UUID.randomUUID().toString(),
                    uttaksgrad = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                    årsak = Årsak.FULL_DEKNING,
                    pleiebehov = Pleiebehov.PROSENT_200.prosent.setScale(2, RoundingMode.HALF_UP),
                    knekkpunktTyper = setOf(),
                    utbetalingsgrader = listOf(
                        Utbetalingsgrader(
                            arbeidsforhold = arbeidsforhold1,
                            utbetalingsgrad = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                            normalArbeidstid = FULL_DAG,
                            faktiskArbeidstid = Duration.ZERO
                        )
                    ),
                    søkersTapteArbeidstid = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                    oppgittTilsyn = null,
                    annenPart = AnnenPart.ALENE,
                    nattevåk = null,
                    beredskap = null,
                    utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN)
                )
            )
        )
    }

    private fun dummyIkkeOppfyltUttaksplan(periode: LukketPeriode): Uttaksplan {
        return Uttaksplan(
            perioder = mapOf(
                periode to UttaksperiodeInfo.ikkeOppfylt(
                    kildeBehandlingUUID = UUID.randomUUID().toString(),
                    årsaker = setOf(Årsak.FOR_MANGE_DAGER_UTENLANDSOPPHOLD),
                    pleiebehov = Pleiebehov.PROSENT_200.prosent.setScale(2, RoundingMode.HALF_UP),
                    knekkpunktTyper = setOf(),
                    utbetalingsgrader = listOf(
                        Utbetalingsgrader(
                            arbeidsforhold = arbeidsforhold1,
                            utbetalingsgrad = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                            normalArbeidstid = FULL_DAG,
                            faktiskArbeidstid = Duration.ZERO
                        )
                    ),
                    søkersTapteArbeidstid = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                    oppgittTilsyn = null,
                    annenPart = AnnenPart.ALENE,
                    nattevåk = null,
                    beredskap = null,
                    utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN)
                )
            ),
            trukketUttak = listOf()
        )
    }

    private fun dummyUttaksplanMedUtenlandsopphold(
        periode: LukketPeriode, landkode: String,
        utenlandsoppholdÅrsak: UtenlandsoppholdÅrsak
    ): Uttaksplan {
        return Uttaksplan(
            perioder = mapOf(
                periode to UttaksperiodeInfo.oppfylt(
                    kildeBehandlingUUID = UUID.randomUUID().toString(),
                    uttaksgrad = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                    årsak = Årsak.FULL_DEKNING,
                    pleiebehov = Pleiebehov.PROSENT_200.prosent.setScale(2, RoundingMode.HALF_UP),
                    knekkpunktTyper = setOf(),
                    utbetalingsgrader = listOf(
                        Utbetalingsgrader(
                            arbeidsforhold = arbeidsforhold1,
                            utbetalingsgrad = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                            normalArbeidstid = FULL_DAG,
                            faktiskArbeidstid = Duration.ZERO
                        )
                    ),
                    søkersTapteArbeidstid = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                    oppgittTilsyn = null,
                    annenPart = AnnenPart.ALENE,
                    nattevåk = null,
                    beredskap = null,
                    utenlandsopphold = Utenlandsopphold(landkode, utenlandsoppholdÅrsak)
                )
            ),
            trukketUttak = listOf()
        )
    }

    private fun dummyIkkeOppfyltUttaksplanMedUtenlandsopphold(
        periode: LukketPeriode, landkode: String,
        utenlandsoppholdÅrsak: UtenlandsoppholdÅrsak
    ): Uttaksplan {
        return Uttaksplan(
            perioder = mapOf(
                periode to UttaksperiodeInfo.ikkeOppfylt(
                    kildeBehandlingUUID = UUID.randomUUID().toString(),
                    årsaker = setOf(Årsak.FOR_MANGE_DAGER_UTENLANDSOPPHOLD),
                    pleiebehov = Pleiebehov.PROSENT_200.prosent.setScale(2, RoundingMode.HALF_UP),
                    knekkpunktTyper = setOf(),
                    utbetalingsgrader = listOf(
                        Utbetalingsgrader(
                            arbeidsforhold = arbeidsforhold1,
                            utbetalingsgrad = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                            normalArbeidstid = FULL_DAG,
                            faktiskArbeidstid = Duration.ZERO
                        )
                    ),
                    søkersTapteArbeidstid = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                    oppgittTilsyn = null,
                    annenPart = AnnenPart.ALENE,
                    nattevåk = null,
                    beredskap = null,
                    utenlandsopphold = Utenlandsopphold(landkode, utenlandsoppholdÅrsak)
                )
            ),
            trukketUttak = listOf()
        )
    }

    private fun dummyUttaksplanPLS(periode: LukketPeriode, kvoteInfo: KvoteInfo?): Uttaksplan {
        return Uttaksplan(
            perioder = mapOf(
                periode to UttaksperiodeInfo.oppfylt(
                    kildeBehandlingUUID = UUID.randomUUID().toString(),
                    uttaksgrad = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                    årsak = Årsak.FULL_DEKNING,
                    pleiebehov = Pleiebehov.PROSENT_100.prosent.setScale(2, RoundingMode.HALF_UP),
                    knekkpunktTyper = setOf(),
                    utbetalingsgrader = listOf(
                        Utbetalingsgrader(
                            arbeidsforhold = arbeidsforhold1,
                            utbetalingsgrad = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                            normalArbeidstid = FULL_DAG,
                            faktiskArbeidstid = Duration.ZERO
                        )
                    ),
                    søkersTapteArbeidstid = Prosent(100).setScale(2, RoundingMode.HALF_UP),
                    oppgittTilsyn = null,
                    annenPart = AnnenPart.ALENE,
                    nattevåk = null,
                    beredskap = null,
                    utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN)
                )

            ),
            trukketUttak = listOf(),
            kvoteInfo = kvoteInfo
        )
    }

    private fun dummyRegelGrunnlagPLS(periode: LukketPeriode, behandlingUUID: UUID = UUID.randomUUID()): RegelGrunnlag {
        return RegelGrunnlag(
            ytelseType = YtelseType.PLS,
            saksnummer = nesteSaksnummer(),
            behandlingUUID = behandlingUUID,
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

}
