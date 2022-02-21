package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class UttakTjenesteOppdaterPerioderTest {

    private val arbeidsforhold = UUID.randomUUID().toString()
    private val behandlingUUID = UUID.randomUUID().toString()

    @Test
    internal fun `Oppdater perioder uten noen perioder som skal oppdateres fører ikke ingen endring`() {
        val eksisterendeUttaksplan = lagUttaksplan(mapOf(LukketPeriode("2021-12-01/2021-12-03") to Utfall.OPPFYLT))
        val nyUttaksplan = UttakTjeneste.endreUttaksplan(eksisterendeUttaksplan, mapOf())

        assertThat(nyUttaksplan).isEqualTo(eksisterendeUttaksplan)
    }

    @Test
    internal fun `Oppdater periode med fullt overlapp`() {
        val helePerioden = LukketPeriode("2021-12-01/2021-12-03")
        val eksisterendeUttaksplan = lagUttaksplan(mapOf(helePerioden to Utfall.OPPFYLT))
        val nyUttaksplan = UttakTjeneste.endreUttaksplan(eksisterendeUttaksplan, mapOf(helePerioden to Årsak.FOR_LAV_INNTEKT))

        assertThat(nyUttaksplan.perioder).hasSize(1)
        nyUttaksplan.sjekkIkkeOppfylt("2021-12-01/2021-12-03")
    }


    @Test
    internal fun `Oppdater periode med overlapp på slutten`() {
        val helePerioden = LukketPeriode("2021-12-01/2021-12-03")
        val ikkeOppfyltPeriode = LukketPeriode("2021-12-02/2021-12-03")
        val eksisterendeUttaksplan = lagUttaksplan(mapOf(helePerioden to Utfall.OPPFYLT))
        val nyUttaksplan = UttakTjeneste.endreUttaksplan(eksisterendeUttaksplan, mapOf(ikkeOppfyltPeriode to Årsak.FOR_LAV_INNTEKT))

        assertThat(nyUttaksplan.perioder).hasSize(2)
        nyUttaksplan.sjekkOppfylt("2021-12-01/2021-12-01")
        nyUttaksplan.sjekkIkkeOppfylt("2021-12-02/2021-12-03")
    }


    @Test
    internal fun `Oppdater periode med overlapp midt i perioden`() {
        val helePerioden = LukketPeriode("2021-12-01/2021-12-03")
        val ikkeOppfyltPeriode = LukketPeriode("2021-12-02/2021-12-02")
        val eksisterendeUttaksplan = lagUttaksplan(mapOf(helePerioden to Utfall.OPPFYLT))
        val nyUttaksplan = UttakTjeneste.endreUttaksplan(eksisterendeUttaksplan, mapOf(ikkeOppfyltPeriode to Årsak.FOR_LAV_INNTEKT))

        assertThat(nyUttaksplan.perioder).hasSize(3)
        nyUttaksplan.sjekkOppfylt("2021-12-01/2021-12-01")
        nyUttaksplan.sjekkIkkeOppfylt("2021-12-02/2021-12-02")
        nyUttaksplan.sjekkOppfylt("2021-12-03/2021-12-03")
    }

    private fun Uttaksplan.sjekkOppfylt(periodeString: String) {
        val periode = LukketPeriode(periodeString)
        val info = this.perioder[periode]!!
        assertThat(info.utfall).isEqualTo(Utfall.OPPFYLT)
        assertThat(info.uttaksgrad).isEqualByComparingTo(HUNDRE_PROSENT)
        info.utbetalingsgrader.forEach {
            assertThat(it.utbetalingsgrad).isEqualByComparingTo(HUNDRE_PROSENT)
        }
    }

    private fun Uttaksplan.sjekkIkkeOppfylt(periodeString: String) {
        val periode = LukketPeriode(periodeString)
        val info = this.perioder[periode]!!
        assertThat(info.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(info.uttaksgrad).isEqualByComparingTo(NULL_PROSENT)
        info.utbetalingsgrader.forEach {
            assertThat(it.utbetalingsgrad).isEqualByComparingTo(NULL_PROSENT)
        }
    }

    private fun lagUttaksplan(perioder: Map<LukketPeriode, Utfall>): Uttaksplan {
        val nyePerioder = perioder.entries.associate {
            if (it.value == Utfall.OPPFYLT) {
                it.key to lagOppfyltPeriode()
            } else {
                it.key to lagIkkeOppfyltPeriode()
            }
        }

        return Uttaksplan(perioder = nyePerioder)
    }

    private fun lagOppfyltPeriode(): UttaksperiodeInfo {
        return UttaksperiodeInfo.oppfylt(
            uttaksgrad = HUNDRE_PROSENT,
            utbetalingsgrader = mapOf(arbeidsforhold to HUNDRE_PROSENT).somUtbetalingsgrader(),
            søkersTapteArbeidstid = HUNDRE_PROSENT,
            oppgittTilsyn = null,
            årsak = Årsak.FULL_DEKNING,
            pleiebehov = HUNDRE_PROSENT,
            knekkpunktTyper = setOf(),
            kildeBehandlingUUID = behandlingUUID,
            annenPart = AnnenPart.ALENE,
            nattevåk = null,
            beredskap = null,
            landkode = null
        )
    }

    private fun lagIkkeOppfyltPeriode(): UttaksperiodeInfo {
        return  UttaksperiodeInfo.ikkeOppfylt(
            utbetalingsgrader = mapOf(arbeidsforhold to Prosent.ZERO).somUtbetalingsgrader(),
            søkersTapteArbeidstid = Prosent(100),
            oppgittTilsyn = null,
            årsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT),
            pleiebehov = Pleiebehov.PROSENT_100.prosent,
            knekkpunktTyper = setOf(),
            kildeBehandlingUUID = behandlingUUID,
            annenPart = AnnenPart.ALENE,
            nattevåk = null,
            beredskap = null
        )
    }

}