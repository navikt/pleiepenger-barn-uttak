package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkAvslåttInneholderAvslåttÅrsaker
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.inneholder
import no.nav.pleiepengerbarn.uttak.regler.print
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

internal class BarnsDødRegelTest {
    private companion object {
        private val hjemler = setOf(Hjemmel(
                henvisning = "Henvsining til en lov",
                anvendelse = "Testformål"
        ))
        private val innvilgetÅrsak = InnvilgetÅrsak(
                årsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                hjemler = hjemler
        )
    }

    @Test
    internal fun `Om barnet dør i midten av en innvilget periode`() {

    }

    @Test
    internal fun `Om barnet dør i en avslått periode`() {
        val grunnlag = lagGrunnlag(
                dødeIEnAvslåttPeriode = true
        )
        val dødsdato = grunnlag.barn.dødsdato!!

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        uttaksplanFørRegelkjøring.print(grunnlag)

        val uttaksplanEtterRegelkjøring = BarnsDødRegel().kjør(
                uttaksplan = uttaksplanFørRegelkjøring,
                grunnlag = grunnlag
        )

        uttaksplanEtterRegelkjøring.print(grunnlag)

        // Bør nå finnes en ny knekt periode
        assertEquals(uttaksplanFørRegelkjøring.perioder.size + 1 , uttaksplanEtterRegelkjøring.perioder.size)

        // Bør nå finnes en periode som har TOM = dødsdato
        assertTrue(uttaksplanEtterRegelkjøring.perioder.filterKeys { it.tom.isEqual(dødsdato) }.size == 1)

        // Bør nå finnes en periode som har FOM = (dødsdato + 1 dag)
        assertTrue(uttaksplanEtterRegelkjøring.perioder.filterKeys { it.fom.isEqual(dødsdato.plusDays(1)) }.size == 1)

        // Siste dag i siste periode bør være uendret
        assertEquals(uttaksplanFørRegelkjøring.perioder.keys.last(), uttaksplanEtterRegelkjøring.perioder.keys.last())

        // Alle perioder før dødsdato bør være uendret
        val periderFørDødsdato = uttaksplanEtterRegelkjøring.perioder
                .filterKeys { it.tom.isBefore(dødsdato) || it.tom.isEqual(dødsdato) }
                .forEach { (periode, periodeInfo) ->
                    if (!uttaksplanFørRegelkjøring.perioder.containsKey(periode)) {
                        assertEquals(periodeInfo, uttaksplanFørRegelkjøring.uttaksPeriodeInfoSomInneholder(dødsdato))
                    } else {
                        assertEquals(periodeInfo, uttaksplanFørRegelkjøring.perioder[periode])
                    }
                }


        // Alle perioder etter dødsdato bør være avslått med rett årsak
        uttaksplanEtterRegelkjøring.perioder
                .filterKeys { it.fom.isAfter(dødsdato) }
                .forEach { (periode, periodeInfo) ->
                    assertTrue(periodeInfo is AvslåttPeriode)
                    sjekkAvslåttInneholderAvslåttÅrsaker(
                            uttaksplan = uttaksplanEtterRegelkjøring,
                            forventetPeriode = periode,
                            forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.BarnetsDødsfall)
                    )
                }
    }

    @Test
    internal fun `Om barnet fortsatt lever har ikke kjøring av regel noen effekt`() {
        val grunnlag = lagGrunnlag()
        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        uttaksplanFørRegelkjøring.print(grunnlag)

        val uttaksplanEtterRegelkjøring = BarnsDødRegel().kjør(
                uttaksplan = uttaksplanFørRegelkjøring,
                grunnlag = grunnlag
        )

        assertEquals(uttaksplanFørRegelkjøring, uttaksplanEtterRegelkjøring)
    }

    @Test
    internal fun `Om barnet dør etter siste søknadsperiode har kjøring av regel noen effekt`() {
        val grunnlag = lagGrunnlag(
                dødeEtterSisteSøknadsperiode = true
        )
        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        uttaksplanFørRegelkjøring.print(grunnlag)

        val uttaksplanEtterRegelkjøring = BarnsDødRegel().kjør(
                uttaksplan = uttaksplanFørRegelkjøring,
                grunnlag = grunnlag
        )

        assertEquals(uttaksplanFørRegelkjøring, uttaksplanEtterRegelkjøring)
    }

    @Test
    internal fun `Om barnet dør før første søknadsperiode skal alle perioder blir avslått`() {
        val grunnlag = lagGrunnlag(
                dødeFørFørsteSøknadsperiode = true
        )
        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        uttaksplanFørRegelkjøring.print(grunnlag)

        val uttaksplanEtterRegelkjøring = BarnsDødRegel().kjør(
                uttaksplan = uttaksplanFørRegelkjøring,
                grunnlag = grunnlag
        )

        uttaksplanEtterRegelkjøring.print(grunnlag)

        assertEquals(uttaksplanFørRegelkjøring.perioder.size, uttaksplanEtterRegelkjøring.perioder.size)

        uttaksplanEtterRegelkjøring.perioder.forEach { (periode, _) ->
            sjekkAvslåttInneholderAvslåttÅrsaker(
                    uttaksplan = uttaksplanEtterRegelkjøring,
                    forventetPeriode = periode,
                    forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.BarnetsDødsfall)
            )
        }
    }

    private fun lagGrunnlag(
            dødeIEnAvslåttPeriode: Boolean = false,
            dødeIEnPeriodeGradertMotTilsyn: Boolean = false,
            dødeIEnPeriodeAvkortetMotInntekt: Boolean = false,
            dødeFørFørsteSøknadsperiode: Boolean = false,
            dødeEtterSisteSøknadsperiode: Boolean = false
    ) : RegelGrunnlag {
        val antallFlaggSatt =
                listOf(dødeIEnAvslåttPeriode, dødeIEnPeriodeGradertMotTilsyn, dødeIEnPeriodeAvkortetMotInntekt)
                        .filter { it }
                        .size

        if (antallFlaggSatt > 1) {
            throw IllegalStateException("Kun et flagg kan settes")
        }

        val helePerioden = LukketPeriode("2020-01-01/2020-03-01")

        val barnetsDødsdato =
                when {
                    dødeIEnAvslåttPeriode -> LocalDate.parse("2020-02-08")
                    dødeIEnPeriodeGradertMotTilsyn -> LocalDate.parse("2020-02-15")
                    dødeIEnPeriodeAvkortetMotInntekt -> LocalDate.parse("2020-01-15")
                    dødeFørFørsteSøknadsperiode-> LocalDate.parse("2019-12-31")
                    dødeEtterSisteSøknadsperiode -> LocalDate.parse("2020-03-02")
                    else -> null
                }

        return RegelGrunnlag(
                barn = Barn(
                        dødsdato = barnetsDødsdato
                ),
                arbeid = mapOf(
                        "123" to mapOf(
                                helePerioden to ArbeidInfo(
                                        jobberNormaltPerUke = Duration.ofHours(37).plusMinutes(30),
                                        skalJobbeProsent = Prosent(50)
                                )
                        )
                ),
                søknadsperioder = listOf(
                        LukketPeriode("2020-01-01/2020-01-20"),
                        LukketPeriode("2020-01-29/2020-03-01")
                ),
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(
                                prosent = TilsynsbehovStørrelse.PROSENT_100
                        )
                ),
                tilsynsperioder = mapOf(
                        LukketPeriode("2020-02-11/2020-03-01") to Tilsyn(
                                grad = Prosent(60)
                        )
                ),
                ikkeMedlem = listOf(
                        LukketPeriode("2020-02-01/2020-02-10")
                )
        )
    }

    private fun Uttaksplan.uttaksPeriodeInfoSomInneholder(dato: LocalDate) : UttaksPeriodeInfo {
        return perioder.filterKeys { it.inneholder(dato) }.values.firstOrNull() ?: throw IllegalStateException("Fant ikke uttaksperide mot tom=$dato")
    }
}