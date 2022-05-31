package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.Pleiebehov.PROSENT_100
import no.nav.pleiepengerbarn.uttak.kontrakter.Pleiebehov.PROSENT_200
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.domene.GraderBeregnet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

internal class BeregnGraderTest {

    private val IKKE_ETABLERT_TILSYN = Duration.ZERO
    private val INGENTING = Duration.ZERO
    private val ARBEIDSGIVER1 = Arbeidsforhold(type = "AT", organisasjonsnummer = "123456789")
    private val ARBEIDSGIVER2 = Arbeidsforhold(type = "AT", organisasjonsnummer = "987654321")
    private val IKKE_YRKESAKTIV = Arbeidsforhold(type = Arbeidstype.IKKE_YRKESAKTIV.kode)
    private val INAKTIV = Arbeidsforhold(type = Arbeidstype.INAKTIV.kode)
    private val DAGPENGER = Arbeidsforhold(type = Arbeidstype.DAGPENGER.kode)
    private val KUN_YTELSE = Arbeidsforhold(type = Arbeidstype.KUN_YTELSE.kode)

    @BeforeEach
    internal fun setUp() {
        System.setProperty("TILSYN_GRADERING_ENDRINGER", "true")
    }

    @AfterEach
    internal fun tearDown() {
        System.clearProperty("TILSYN_GRADERING_ENDRINGER")
    }

    @Test
    internal fun `100 prosent vanlig uttak`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = NULL_PROSENT,
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.FULL_DEKNING,
            HUNDRE_PROSENT,
            ARBEIDSGIVER1 to HUNDRE_PROSENT
        )
        assertThat(grader.graderingMotTilsyn.overseEtablertTilsynÅrsak).isNull()
    }

    @Test
    internal fun `50 prosent vanlig uttak`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = NULL_PROSENT,
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.dividedBy(2))
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(50),
            ARBEIDSGIVER1 to Prosent(50)
        )
    }

    @Test
    internal fun `50 prosent uttak når annen part også tar ut 50 prosent`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = Prosent(50),
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.dividedBy(2))
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT, //Dersom andre tilsyn og arbeid er likt, så skal årsaken være AVKORTET_MOT_INNTEKT
            Prosent(50),
            ARBEIDSGIVER1 to Prosent(50)
        )
    }

    @Test
    internal fun `100 prosent uttak når annen part også tar ut 50 prosent, men pleiebehovet er 200 prosent`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_200,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = Prosent(50),
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.FULL_DEKNING,
            HUNDRE_PROSENT,
            ARBEIDSGIVER1 to HUNDRE_PROSENT
        )
    }

    @Test
    internal fun `100 prosent arbeid når annen part også tar ut 50 prosent blir redusert`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = Prosent(50),
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.GRADERT_MOT_TILSYN,
            Prosent(50),
            ARBEIDSGIVER1 to Prosent(50)
        )
    }


    @Test
    internal fun `50 prosent arbeid av en stilling på 10 timer, skal gi 50 prosent uttaksgrad og utbetalingsgrad`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = NULL_PROSENT,
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(
                    jobberNormalt = Duration.ofHours(10),
                    jobberNå = Duration.ofHours(5)
                )
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(50),
            ARBEIDSGIVER1 to Prosent(50)
        )
    }

    @Test
    internal fun `100 prosent fravær hos 2 arbeidsgivere`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = NULL_PROSENT,
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), jobberNå = INGENTING),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(
                    jobberNormalt = Duration.ofHours(4).plusMinutes(30),
                    jobberNå = INGENTING
                )
            )
        )

        grader.assert(
            Årsak.FULL_DEKNING,
            HUNDRE_PROSENT,
            ARBEIDSGIVER1 to HUNDRE_PROSENT,
            ARBEIDSGIVER2 to HUNDRE_PROSENT
        )

    }

    @Test
    internal fun `Delvis arbeid hos 2 arbeidsgivere`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = NULL_PROSENT,
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(
                    jobberNormalt = Duration.ofHours(3),
                    jobberNå = Duration.ofHours(1).plusMinutes(30)
                ),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(
                    jobberNormalt = Duration.ofHours(4).plusMinutes(30),
                    jobberNå = Duration.ofHours(1).plusMinutes(30)
                )
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(60),
            ARBEIDSGIVER1 to Prosent(50),
            ARBEIDSGIVER2 to Prosent(67)
        )
    }

    @Test
    internal fun `Delvis arbeid hos 2 arbeidsgivere som tilsammen er mindre enn en 100 prosent stilling`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = NULL_PROSENT,
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(
                    jobberNormalt = Duration.ofHours(3),
                    jobberNå = Duration.ofHours(1).plusMinutes(30)
                ),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(
                    jobberNormalt = Duration.ofHours(3),
                    jobberNå = Duration.ofHours(2).plusMinutes(15)
                )
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(38),
            ARBEIDSGIVER1 to Prosent(50),
            ARBEIDSGIVER2 to Prosent(25)
        )
    }

    @Test
    internal fun `Delvis arbeid hos 2 arbeidsgivere som tilsammen er mer enn en 100 prosent stilling`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = NULL_PROSENT,
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(
                    jobberNormalt = Duration.ofHours(4).plusMinutes(30),
                    jobberNå = Duration.ofHours(2).plusMinutes(30)
                ),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(
                    jobberNormalt = Duration.ofHours(4),
                    jobberNå = Duration.ofHours(2)
                )
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(47),
            ARBEIDSGIVER1 to Prosent(44),
            ARBEIDSGIVER2 to Prosent(50)
        )
    }

    @Test
    internal fun `Søker vil ha 100 prosent uttak hos to arbeidsgiver, og motpart har allerede tatt ut 50 prosent av 100 prosent pleiebehov`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = Prosent(50),
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(
                    jobberNormalt = Duration.ofHours(4).plusMinutes(30),
                    jobberNå = INGENTING
                ),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4), jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.GRADERT_MOT_TILSYN,
            Prosent(50),
            ARBEIDSGIVER1 to Prosent(50),
            ARBEIDSGIVER2 to Prosent(50)
        )
    }

    @Test
    internal fun `Søker vil ha 100 prosent uttak hos to arbeidsgiver, og motpart har allerede tatt ut 50 prosent av 200 prosent pleiebehov`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_200,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsyn = Prosent(50),
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(
                    jobberNormalt = Duration.ofHours(4).plusMinutes(30),
                    jobberNå = INGENTING
                ),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4), jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.FULL_DEKNING,
            HUNDRE_PROSENT,
            ARBEIDSGIVER1 to HUNDRE_PROSENT,
            ARBEIDSGIVER2 to HUNDRE_PROSENT
        )
    }

    @Test
    internal fun `Etablert tilsyn skal redusere uttaksgrad og utbetalingsgrad`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = FULL_DAG.prosent(60),
            andreSøkeresTilsyn = Prosent(0),
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING),
            )
        )

        grader.assert(
            Årsak.GRADERT_MOT_TILSYN,
            Prosent(40),
            ARBEIDSGIVER1 to Prosent(40)
        )
    }

    @Test
    internal fun `Etablert tilsyn og delvis arbeid som tilsammen er under 100 prosent skal føre til at søkt periode blir innvilget`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = FULL_DAG.prosent(60),
            andreSøkeresTilsyn = Prosent(0),
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(70)),
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(30),
            ARBEIDSGIVER1 to Prosent(30)
        )
    }

    @Test
    internal fun `Etablert tilsyn og delvis arbeid som tilsammen går utover 100 prosent skal føre til reduserte grader`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = FULL_DAG.prosent(60),
            andreSøkeresTilsyn = Prosent(0),
            andreSøkeresTilsynReberegnet = false,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(40)),
            )
        )

        grader.assert(
            Årsak.GRADERT_MOT_TILSYN,
            Prosent(40),
            ARBEIDSGIVER1 to Prosent(40)
        )
    }

    @Test
    internal fun `100 prosent fravær, men kun 40 prosent pleiebehov`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            oppgittTilsyn = FULL_DAG.prosent(40),
            andreSøkeresTilsynReberegnet = false,
            andreSøkeresTilsyn = NULL_PROSENT,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_SØKERS_ØNSKE,
            Prosent(40),
            ARBEIDSGIVER1 to Prosent(40)
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med IKKE_YRKESAKTIV dersom det finnes andre arbeidsforhold`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsynReberegnet = false,
            andreSøkeresTilsyn = NULL_PROSENT,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(50)),
                IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(50),
            ARBEIDSGIVER1 to Prosent(50),
            IKKE_YRKESAKTIV to Prosent(50)
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med IKKE_YRKESAKTIV dersom det finnes andre arbeidsforhold med tilsyn`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = FULL_DAG.prosent(47),
            andreSøkeresTilsynReberegnet = false,
            andreSøkeresTilsyn = NULL_PROSENT,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(40)),
                IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.GRADERT_MOT_TILSYN,
            Prosent(53),
            ARBEIDSGIVER1 to Prosent(53),
            IKKE_YRKESAKTIV to Prosent(53)
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med IKKE_YRKESAKTIV dersom det finnes andre arbeidsforhold med tilsyn fra annen søker`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = INGENTING,
            andreSøkeresTilsynReberegnet = false,
            andreSøkeresTilsyn = Prosent(47),
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(40)),
                IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.GRADERT_MOT_TILSYN,
            Prosent(53),
            ARBEIDSGIVER1 to Prosent(53),
            IKKE_YRKESAKTIV to Prosent(53)
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med IKKE_YRKESAKTIV dersom det finnes to andre arbeidsforhold`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsynReberegnet = false,
            andreSøkeresTilsyn = NULL_PROSENT,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(50)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(25)),
                IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(63),
            ARBEIDSGIVER1 to Prosent(50),
            ARBEIDSGIVER2 to Prosent(75),
            IKKE_YRKESAKTIV to Prosent(62.5)
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med IKKE_YRKESAKTIV dersom det finnes andre aktiviteter`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsynReberegnet = false,
            andreSøkeresTilsyn = NULL_PROSENT,
            arbeid = mapOf(
                DAGPENGER to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING),
                IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.FULL_DEKNING,
            Prosent(100),
            DAGPENGER to Prosent(100),
            IKKE_YRKESAKTIV to Prosent(100)
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med DAGPENGER og IKKE_YRKESAKTIV dersom det finnes andre aktiviteter`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsynReberegnet = false,
            andreSøkeresTilsyn = NULL_PROSENT,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(40)),
                DAGPENGER to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING),
                IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(60),
            ARBEIDSGIVER1 to Prosent(60),
            DAGPENGER to Prosent(60),
            IKKE_YRKESAKTIV to Prosent(60)
        )
    }

    @Test
    internal fun `Se bort fra arbeidsforhold med DAGPENGER og IKKE_YRKESAKTIV dersom det finnes flere andre aktiviteter`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsynReberegnet = false,
            andreSøkeresTilsyn = NULL_PROSENT,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(40)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(60)),
                DAGPENGER to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING),
                IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(50),
            ARBEIDSGIVER1 to Prosent(60),
            ARBEIDSGIVER2 to Prosent(40),
            DAGPENGER to Prosent(50),
            IKKE_YRKESAKTIV to Prosent(50)
        )
    }

    @Test
    internal fun `Avslå periode dersom annet arbeidsforhold med IKKE_YRKESAKTIV gjør at uttaksgrad kommer under 20 prosent`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsynReberegnet = false,
            andreSøkeresTilsyn = NULL_PROSENT,
            arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(
                    jobberNormalt = FULL_DAG,
                    jobberNå = Duration.ofHours(6).plusMinutes(45)
                ),
                IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)
            )
        )

        grader.assert(
            Årsak.FOR_LAV_TAPT_ARBEIDSTID,
            NULL_PROSENT,
            ARBEIDSGIVER1 to NULL_PROSENT,
            IKKE_YRKESAKTIV to NULL_PROSENT
        )
    }

    @Test
    internal fun `Dersom ikke yrkesaktiv er eneste arbeidsforhold så skal det gi utbetalingsgrad`() {
        val grader = BeregnGrader.beregn(
            pleiebehov = PROSENT_100,
            etablertTilsyn = IKKE_ETABLERT_TILSYN,
            andreSøkeresTilsynReberegnet = false,
            andreSøkeresTilsyn = NULL_PROSENT,
            arbeid = mapOf(
                IKKE_YRKESAKTIV to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = FULL_DAG.prosent(25))
            )
        )

        grader.assert(
            Årsak.AVKORTET_MOT_INNTEKT,
            Prosent(75),
            IKKE_YRKESAKTIV to Prosent(75)
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
