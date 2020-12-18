package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
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
    internal fun `100 % vanlig uttak`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
            ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG, søkersTilsyn = FULL_DAG)
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(HUNDRE_PROSENT)
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(HUNDRE_PROSENT)
    }


    @Test
    internal fun `50 % vanlig uttak`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG.dividedBy(2), søkersTilsyn = FULL_DAG.dividedBy(2))
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(50))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(50))
    }

    @Test
    internal fun `50 % uttak når annen part også tar ut 50 %`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = Prosent(50), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG.dividedBy(2), søkersTilsyn = FULL_DAG.dividedBy(2))
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(50))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(50))
    }

    @Test
    internal fun `100 % uttak når annen part også tar ut 50 %, men tilsynsbehovet er 200%`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_200, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = Prosent(50), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG, søkersTilsyn = FULL_DAG)
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(100))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(100))
    }

    @Test
    internal fun `100 % arbeid når annen part også tar ut 50 % blir redusert`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = Prosent(50), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG, søkersTilsyn = FULL_DAG)
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(50))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(50))
    }


    @Test
    internal fun `50% arbeid av en stilling på 10 timer, skal gi 50 % uttaksgrad og utbetalingsgrad`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(10), taptArbeidstid = Duration.ofHours(5), søkersTilsyn = Duration.ofHours(5))
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(67))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(50))
    }

    @Test
    internal fun `100% fravær hos 2 arbeidsgivere`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), taptArbeidstid = Duration.ofHours(3), søkersTilsyn = Duration.ofHours(3)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4).plusMinutes(30), taptArbeidstid = Duration.ofHours(4).plusMinutes(30), søkersTilsyn = Duration.ofHours(4).plusMinutes(30))
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(HUNDRE_PROSENT)
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(HUNDRE_PROSENT)
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER2]).isEqualByComparingTo(HUNDRE_PROSENT)
    }

    @Test
    internal fun `Delvis arbeid hos 2 arbeidsgivere`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), taptArbeidstid = Duration.ofHours(1).plusMinutes(30), søkersTilsyn = Duration.ofHours(1).plusMinutes(30)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4).plusMinutes(30), taptArbeidstid = Duration.ofHours(3), søkersTilsyn = Duration.ofHours(3))
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(60))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(50))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER2]).isEqualByComparingTo(Prosent(67))
    }

    @Test
    internal fun `Delvis arbeid hos 2 arbeidsgivere som tilsammen er mindre enn en 100 % stilling`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), taptArbeidstid = Duration.ofHours(1).plusMinutes(30), søkersTilsyn = Duration.ofHours(1).plusMinutes(30)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(3), taptArbeidstid = Duration.ofMinutes(45), søkersTilsyn = Duration.ofMinutes(45))
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(30))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(50))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER2]).isEqualByComparingTo(Prosent(25))
    }

    @Test
    internal fun `Delvis arbeid hos 2 arbeidsgivere som tilsammen er mer enn en 100 % stilling`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = NULL_PROSENT, arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4).plusMinutes(30), taptArbeidstid = Duration.ofHours(2), søkersTilsyn = Duration.ofHours(2)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4), taptArbeidstid = Duration.ofHours(2), søkersTilsyn = Duration.ofHours(2))
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(53))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(44))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER2]).isEqualByComparingTo(Prosent(50))
    }

    @Test
    internal fun `Søker vil ha 100 % uttak hos to arbeidsgiver, og motpart har allerede tatt ut 50 % av 100 % tilsynsbehov`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = Prosent(50), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4).plusMinutes(30), taptArbeidstid = Duration.ofHours(4).plusMinutes(30), søkersTilsyn = Duration.ofHours(4).plusMinutes(30)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4), taptArbeidstid = Duration.ofHours(4), søkersTilsyn = Duration.ofHours(4))
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(50))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(50))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER2]).isEqualByComparingTo(Prosent(50))
    }

    @Test
    internal fun `Søker vil ha 100 % uttak hos to arbeidsgiver, og motpart har allerede tatt ut 50 % av 200 % tilsynsbehov`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_200, etablertTilsyn = IKKE_ETABLERT_TILSYN, andreSøkeresTilsyn = Prosent(50), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4).plusMinutes(30), taptArbeidstid = Duration.ofHours(4).plusMinutes(30), søkersTilsyn = Duration.ofHours(4).plusMinutes(30)),
                ARBEIDSGIVER2 to ArbeidsforholdPeriodeInfo(jobberNormalt = Duration.ofHours(4), taptArbeidstid = Duration.ofHours(4), søkersTilsyn = Duration.ofHours(4))
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(100))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(100))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER2]).isEqualByComparingTo(Prosent(100))
    }

    @Test
    internal fun `Etablert tilsyn skal redusere uttaksgrad og utbetalingsgrad`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = FULL_DAG.prosent(60), andreSøkeresTilsyn = Prosent(0), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG, søkersTilsyn = FULL_DAG),
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(40))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(40))
    }

    @Test
    internal fun `Etablert tilsyn og delvis arbeid som tilsammen er under 100% skal føre til at søkt periode blir innvilget`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = FULL_DAG.prosent(60), andreSøkeresTilsyn = Prosent(0), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG.prosent(30), søkersTilsyn = FULL_DAG.prosent(30)),
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(30))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(30))
    }

    @Test
    internal fun `Etablert tilsyn og delvis arbeid som tilsammen går utover 100% skal føre til reduserte grader`() {
        val grader = AvklarGrader.avklarGrader(tilsynsbehov = PROSENT_100, etablertTilsyn = FULL_DAG.prosent(60), andreSøkeresTilsyn = Prosent(0), arbeid = mapOf(
                ARBEIDSGIVER1 to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, taptArbeidstid = FULL_DAG.prosent(60), søkersTilsyn = FULL_DAG.prosent(60)),
        ))

        assertThat(grader.uttaksgrad).isEqualByComparingTo(Prosent(40))
        assertThat(grader.utbetalingsgrader[ARBEIDSGIVER1]).isEqualByComparingTo(Prosent(40))
    }

}
