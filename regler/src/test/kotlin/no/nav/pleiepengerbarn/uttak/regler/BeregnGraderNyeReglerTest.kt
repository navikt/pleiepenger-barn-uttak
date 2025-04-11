package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.kontrakter.Pleiebehov.PROSENT_100
import no.nav.pleiepengerbarn.uttak.regler.domene.GraderBeregnet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate

/**
 * BeregnGraderTest er splittet i to tester, denne og BeregningGamleReglerTest. Denne inneholder bare de testene
 * som fikk forskjellig resultat med de nye reglene.
 */
internal class BeregnGraderNyeReglerTest {

    private val IKKE_ETABLERT_TILSYN = Duration.ZERO
    private val INGENTING = Duration.ZERO
    private val ARBEIDSGIVER1 = Arbeidsforhold(type = "AT", organisasjonsnummer = "123456789")
    private val ARBEIDSGIVER2 = Arbeidsforhold(type = "AT", organisasjonsnummer = "987654321")
    private val IKKE_YRKESAKTIV = Arbeidsforhold(type = Arbeidstype.IKKE_YRKESAKTIV.kode)
    private val INAKTIV = Arbeidsforhold(type = Arbeidstype.INAKTIV.kode)
    private val DAGPENGER = Arbeidsforhold(type = Arbeidstype.DAGPENGER.kode)
    private val FRILANS = Arbeidsforhold(type = Arbeidstype.FRILANSER.kode)
    private val SN = Arbeidsforhold(type = Arbeidstype.SELVSTENDIG_NÆRINGSDRIVENDE.kode)
    private val PERIODE = LukketPeriode("2023-01-01/2023-01-31")
    private val NYE_REGLER_UTBETALINGSGRAD_DATO =  LocalDate.parse("2022-01-01")
    private val IKKE_AKTIV_FRILANS = Arbeidsforhold(type = Arbeidstype.FRILANSER_IKKE_AKTIV.kode)
    private val IKKE_AKTIV_SN = Arbeidsforhold(type = Arbeidstype.SELVSTENDIG_NÆRINGSDRIVENDE_IKKE_AKTIV.kode)
    private val FULL_DAG = Duration.ofHours(7).plusMinutes(30)

    @Test
    internal fun `AT + AVSLUTTA ARBEIDSFORHOLD og omsorgsstønad (frilans)`() {
        val grader = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = IKKE_ETABLERT_TILSYN,
                andreSøkeresTilsyn = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(
                        jobberNormalt = Duration.ofHours(4),
                        jobberNå = Duration.ofHours(0)
                    ),
                    ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(
                        jobberNormalt = Duration.ofHours(3),
                        jobberNå = Duration.ofHours(0)
                    ),
                    FRILANS to ArbeidsforholdPeriodeInfo(
                        jobberNormalt = Duration.ofHours(1),
                        jobberNå = Duration.ofHours(1)
                    )
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(88),
            ARBEIDSGIVER1 to Prosent(100),
            ARBEIDSGIVER2 to Prosent(100),
            FRILANS to Prosent(0)
        )

        val grader4 = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = IKKE_ETABLERT_TILSYN,
                andreSøkeresTilsyn = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(
                        jobberNormalt = Duration.ofHours(4),
                        jobberNå = Duration.ofHours(0)
                    ),
                    IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(
                        jobberNormalt = Duration.ofHours(8),
                        jobberNå = Duration.ofHours(0)
                    ),
                    FRILANS to ArbeidsforholdPeriodeInfo(
                        jobberNormalt = Duration.ofHours(1),
                        jobberNå = Duration.ofHours(1)
                    )
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader4.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(92),
            IKKE_YRKESAKTIV to Prosent(100).setScale(2, RoundingMode.HALF_UP),
            ARBEIDSGIVER1 to Prosent(100),
            FRILANS to Prosent(0)
        )
    }


    @Test
    internal fun `Frilans og frilans ikke aktiv vektes likt`() {
        val grader = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = IKKE_ETABLERT_TILSYN,
                andreSøkeresTilsyn = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    IKKE_AKTIV_SN to ArbeidsforholdPeriodeInfo(
                        jobberNormalt = Duration.ofHours(4),
                        jobberNå = Duration.ofHours(0)
                    ),
                    SN to ArbeidsforholdPeriodeInfo(
                        jobberNormalt = Duration.ofHours(4),
                        jobberNå = Duration.ofHours(0)
                    )
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader.assert(
            Årsak.FULL_DEKNING,
            Prosent(100),
            IKKE_AKTIV_SN to Prosent(100),
            SN to Prosent(100),
        )
    }

    @Test
    internal fun `Selvstendig næringsdrivende og SN ikke aktiv vektes likt`() {
        val grader = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = IKKE_ETABLERT_TILSYN,
                andreSøkeresTilsyn = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    IKKE_AKTIV_FRILANS to ArbeidsforholdPeriodeInfo(
                        jobberNormalt = Duration.ofHours(4),
                        jobberNå = Duration.ofHours(0)
                    ),
                    FRILANS to ArbeidsforholdPeriodeInfo(
                        jobberNormalt = Duration.ofHours(4),
                        jobberNå = Duration.ofHours(0)
                    )
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader.assert(
            Årsak.FULL_DEKNING,
            Prosent(100),
            IKKE_AKTIV_FRILANS to Prosent(100),
            FRILANS to Prosent(100),
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med IKKE_YRKESAKTIV dersom det finnes andre arbeidsforhold`() {
        val grader = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = IKKE_ETABLERT_TILSYN,
                andreSøkeresTilsyn = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(50)),
                    IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(75),
            ARBEIDSGIVER1 to Prosent(50),
            IKKE_YRKESAKTIV to Prosent(100)
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med IKKE_YRKESAKTIV dersom det finnes andre arbeidsforhold med tilsyn`() {
        val grader = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = FULL_DAG.prosent(47),
                andreSøkeresTilsyn = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(40)),
                    IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader.assert(
            Årsak.GRADERT_MOT_TILSYN,
            Prosent(53),
            ARBEIDSGIVER1 to Prosent(40),
            IKKE_YRKESAKTIV to Prosent(66)
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med IKKE_YRKESAKTIV dersom det finnes andre arbeidsforhold med tilsyn fra annen søker`() {
        val grader = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = INGENTING,
                andreSøkeresTilsyn = Prosent(47),
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(40)),
                    IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader.assert(
            Årsak.GRADERT_MOT_TILSYN,
            Prosent(53),
            ARBEIDSGIVER1 to Prosent(40),
            IKKE_YRKESAKTIV to Prosent(66)
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med IKKE_YRKESAKTIV dersom det finnes to andre arbeidsforhold`() {
        val grader = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = IKKE_ETABLERT_TILSYN,
                andreSøkeresTilsyn = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(50)),
                    ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(25)),
                    IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(75),
            ARBEIDSGIVER1 to Prosent(50),
            ARBEIDSGIVER2 to Prosent(75),
            IKKE_YRKESAKTIV to Prosent(100)
        )
    }

    @Test
    internal fun `Ikke se bort fra arbeidsforhold med DAGPENGER og IKKE_YRKESAKTIV dersom det finnes andre aktiviteter`() {
        val grader = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = IKKE_ETABLERT_TILSYN,
                andreSøkeresTilsyn = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(40)),
                    DAGPENGER to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING),
                    IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(87),
            ARBEIDSGIVER1 to Prosent(60),
            DAGPENGER to Prosent(100),
            IKKE_YRKESAKTIV to Prosent(100)
        )
    }

    @Test
    internal fun `Se bort fra INAKTIVT arbeidsforhold og IKKE_YRKESAKTIV dersom det finnes andre aktiviteter`() {
        val grader = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = IKKE_ETABLERT_TILSYN,
                andreSøkeresTilsyn = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(40)),
                    INAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING),
                    IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(87),
            ARBEIDSGIVER1 to Prosent(60),
            INAKTIV to Prosent(100),
            IKKE_YRKESAKTIV to Prosent(100)
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med DAGPENGER og IKKE_YRKESAKTIV dersom det finnes flere andre aktiviteter`() {
        val grader = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = IKKE_ETABLERT_TILSYN,
                andreSøkeresTilsyn = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(40)),
                    ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(60)),
                    DAGPENGER to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING),
                    IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(75),
            ARBEIDSGIVER1 to Prosent(60),
            ARBEIDSGIVER2 to Prosent(40),
            DAGPENGER to Prosent(100),
            IKKE_YRKESAKTIV to Prosent(100).setScale(2, RoundingMode.HALF_UP)
        )
    }

    @Test
    internal fun `Avslå periode dersom annet arbeidsforhold med IKKE_YRKESAKTIV gjør at uttaksgrad kommer under 20 prosent`() {
        val grader = BeregnGrader.beregn(
            BeregnGraderGrunnlag(
                pleiebehov = PROSENT_100,
                etablertTilsyn = IKKE_ETABLERT_TILSYN,
                andreSøkeresTilsyn = NULL_PROSENT,
                andreSøkeresTilsynReberegnet = false,
                arbeid = mapOf(
                    ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(
                        jobberNormalt = FULL_DAG,
                        jobberNå = Duration.ofHours(6).plusMinutes(45)
                    ),
                    IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
                ),
                ytelseType = YtelseType.PSB,
                periode = PERIODE,
                nyeReglerUtbetalingsgrad = NYE_REGLER_UTBETALINGSGRAD_DATO,
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            BigDecimal(55),
            ARBEIDSGIVER1 to BigDecimal(10),
            IKKE_YRKESAKTIV to BigDecimal(100)
        )
    }

    private fun GraderBeregnet.assert(
        årsak: Årsak,
        uttaksgrad: Prosent,
        vararg utbetalingsgrader: Pair<Arbeidsforhold, Prosent>
    ) {
        assertThat(this.årsak).isEqualTo(årsak)
        assertThat(this.uttaksgrad).isEqualByComparingTo(uttaksgrad)
        assertThat(this.utbetalingsgrader.size).isEqualTo(utbetalingsgrader.size)
        utbetalingsgrader.forEach {
            assertThat(this.utbetalingsgrader[it.first]!!.utbetalingsgrad).isEqualByComparingTo(it.second)
        }
    }
}
