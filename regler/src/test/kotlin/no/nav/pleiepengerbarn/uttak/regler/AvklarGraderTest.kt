package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.kontrakter.TilsynsbehovStørrelse.PROSENT_100
import no.nav.pleiepengerbarn.uttak.kontrakter.TilsynsbehovStørrelse.PROSENT_200
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

internal class AvklarGraderTest {

    private val NULL_PROSENT = Prosent.ZERO
    private val HUNDRE_PROSENT = Prosent(100)
    private val IKKE_ETABLERT_TILSYN = Duration.ZERO
    private val FULL_DAG = Duration.ofHours(7).plusMinutes(30)
    private val ARBEIDSGIVER1 = Arbeidsforhold(type = "arbeidsgiver", organisasjonsnummer = "123456789")
    private val ARBEIDSGIVER2 = Arbeidsforhold(type = "arbeidsgiver", organisasjonsnummer = "987654321")


    //TODO: Sjekk på årsaker også i alle testene

    @Test
    internal fun `100 prosent vanlig uttak`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
            ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG, søkersTilsyn = FULL_DAG)
        ))

        grader.assert(
                InnvilgetÅrsaker.FULL_DEKNING,
                HUNDRE_PROSENT,
                ARBEIDSGIVER1 to HUNDRE_PROSENT
        )
    }

    @Test
    internal fun `50 prosent vanlig uttak`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG.dividedBy(2), søkersTilsyn = FULL_DAG.dividedBy(2))
        ))

        grader.assert(
                InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT,
                Prosent(50),
                ARBEIDSGIVER1 to Prosent(50)
        )
    }

    @Test
    internal fun `50 prosent uttak når annen part også tar ut 50 prosent`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = Prosent(50), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG.dividedBy(2), søkersTilsyn = FULL_DAG.dividedBy(2))
        ))

        grader.assert(
                InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT, //TODO: skal det også være gradert mot andres tilsyn?
                Prosent(50),
                ARBEIDSGIVER1 to Prosent(50)
        )
    }

    @Test
    internal fun `100 prosent uttak når annen part også tar ut 50 prosent, men tilsynsbehovet er 200 prosent`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_200, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = Prosent(50), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG, søkersTilsyn = FULL_DAG)
        ))

        grader.assert(
                InnvilgetÅrsaker.FULL_DEKNING,
                HUNDRE_PROSENT,
                ARBEIDSGIVER1 to HUNDRE_PROSENT
        )
    }

    @Test
    internal fun `100 prosent arbeid når annen part også tar ut 50 prosent blir redusert`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = Prosent(50), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG, søkersTilsyn = FULL_DAG)
        ))

        grader.assert(
                InnvilgetÅrsaker.GRADERT_MOT_TILSYN,
                Prosent(50),
                ARBEIDSGIVER1 to Prosent(50)
        )
    }


    @Test
    internal fun `50 prosent arbeid av en stilling på 10 timer, skal gi 50 prosent uttaksgrad og utbetalingsgrad`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(10), taptArbeidstid = Duration.ofHours(5), søkersTilsyn = Duration.ofHours(5))
        ))

        grader.assert(
                InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT,
                Prosent(67),
                ARBEIDSGIVER1 to Prosent(50)
        )
    }

    @Test
    internal fun `100 prosent fravær hos 2 arbeidsgivere`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), taptArbeidstid = Duration.ofHours(3), søkersTilsyn = Duration.ofHours(3)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4).plusMinutes(30), taptArbeidstid = Duration.ofHours(4).plusMinutes(30), søkersTilsyn = Duration.ofHours(4).plusMinutes(30))
        ))

        grader.assert(
                InnvilgetÅrsaker.FULL_DEKNING,
                HUNDRE_PROSENT,
                ARBEIDSGIVER1 to HUNDRE_PROSENT,
                ARBEIDSGIVER2 to HUNDRE_PROSENT
        )

    }

    @Test
    internal fun `Delvis arbeid hos 2 arbeidsgivere`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), taptArbeidstid = Duration.ofHours(1).plusMinutes(30), søkersTilsyn = Duration.ofHours(1).plusMinutes(30)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4).plusMinutes(30), taptArbeidstid = Duration.ofHours(3), søkersTilsyn = Duration.ofHours(3))
        ))

        grader.assert(
                InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT,
                Prosent(60),
                ARBEIDSGIVER1 to Prosent(50),
                ARBEIDSGIVER2 to Prosent(67)
        )
    }

    @Test
    internal fun `Delvis arbeid hos 2 arbeidsgivere som tilsammen er mindre enn en 100 prosent stilling`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), taptArbeidstid = Duration.ofHours(1).plusMinutes(30), søkersTilsyn = Duration.ofHours(1).plusMinutes(30)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), taptArbeidstid = Duration.ofMinutes(45), søkersTilsyn = Duration.ofMinutes(45))
        ))

        grader.assert(
                InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT,
                Prosent(30),
                ARBEIDSGIVER1 to Prosent(50),
                ARBEIDSGIVER2 to Prosent(25)
        )
    }

    @Test
    internal fun `Delvis arbeid hos 2 arbeidsgivere som tilsammen er mer enn en 100 prosent stilling`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4).plusMinutes(30), taptArbeidstid = Duration.ofHours(2), søkersTilsyn = Duration.ofHours(2)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4), taptArbeidstid = Duration.ofHours(2), søkersTilsyn = Duration.ofHours(2))
        ))

        grader.assert(
                InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT,
                Prosent(53),
                ARBEIDSGIVER1 to Prosent(44),
                ARBEIDSGIVER2 to Prosent(50)
        )
    }

    @Test
    internal fun `Søker vil ha 100 prosent uttak hos to arbeidsgiver, og motpart har allerede tatt ut 50 prosent av 100 prosent tilsynsbehov`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = Prosent(50), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4).plusMinutes(30), taptArbeidstid = Duration.ofHours(4).plusMinutes(30), søkersTilsyn = Duration.ofHours(4).plusMinutes(30)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4), taptArbeidstid = Duration.ofHours(4), søkersTilsyn = Duration.ofHours(4))
        ))

        grader.assert(
                InnvilgetÅrsaker.GRADERT_MOT_TILSYN,
                Prosent(50),
                ARBEIDSGIVER1 to Prosent(50),
                ARBEIDSGIVER2 to Prosent(50)
        )
    }

    @Test
    internal fun `Søker vil ha 100 prosent uttak hos to arbeidsgiver, og motpart har allerede tatt ut 50 prosent av 200 prosent tilsynsbehov`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_200, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = Prosent(50), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4).plusMinutes(30), taptArbeidstid = Duration.ofHours(4).plusMinutes(30), søkersTilsyn = Duration.ofHours(4).plusMinutes(30)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4), taptArbeidstid = Duration.ofHours(4), søkersTilsyn = Duration.ofHours(4))
        ))

        grader.assert(
                InnvilgetÅrsaker.FULL_DEKNING,
                HUNDRE_PROSENT,
                ARBEIDSGIVER1 to HUNDRE_PROSENT,
                ARBEIDSGIVER2 to HUNDRE_PROSENT
        )
    }

    @Test
    internal fun `Etablert tilsyn skal redusere uttaksgrad og utbetalingsgrad`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = FULL_DAG.prosent(60), andreSøkeresTilsyn = Prosent(0), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG, søkersTilsyn = FULL_DAG),
        ))

        grader.assert(
                InnvilgetÅrsaker.GRADERT_MOT_TILSYN,
                Prosent(40),
                ARBEIDSGIVER1 to Prosent(40)
        )
    }

    @Test
    internal fun `Etablert tilsyn og delvis arbeid som tilsammen er under 100 prosent skal føre til at søkt periode blir innvilget`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = FULL_DAG.prosent(60), andreSøkeresTilsyn = Prosent(0), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG.prosent(30), søkersTilsyn = FULL_DAG.prosent(30)),
        ))

        grader.assert(
                InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT,
                Prosent(30),
                ARBEIDSGIVER1 to Prosent(30)
        )
    }

    @Test
    internal fun `Etablert tilsyn og delvis arbeid som tilsammen går utover 100 prosent skal føre til reduserte grader`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = FULL_DAG.prosent(60), andreSøkeresTilsyn = Prosent(0), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG.prosent(60), søkersTilsyn = FULL_DAG.prosent(60)),
        ))

        grader.assert(
                InnvilgetÅrsaker.GRADERT_MOT_TILSYN,
                Prosent(40),
                ARBEIDSGIVER1 to Prosent(40)
        )
    }

    private fun AvklarteGrader.assert(årsak: InnvilgetÅrsaker, uttaksgrad: Prosent, vararg utbetalingsgrader: Pair<Arbeidsforhold, Prosent>) {
        assertThat(this.årsak).isInstanceOf(InnvilgetÅrsak::class.java)
        assertThat((this.årsak as InnvilgetÅrsak).årsak).isEqualTo(årsak)
        assertThat(this.uttaksgrad).isEqualByComparingTo(uttaksgrad)
        assertThat(this.utbetalingsgrader.size).isEqualTo(utbetalingsgrader.size)
        utbetalingsgrader.forEach {
            assertThat(this.utbetalingsgrader[it.first]).isEqualByComparingTo(it.second)
        }
    }

}
