package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkAvslått
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkAvslåttInneholderAvslåttÅrsaker
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkInnvilget
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.erLikEllerFør
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.inneholder
import no.nav.pleiepengerbarn.uttak.regler.print
import no.nav.pleiepengerbarn.uttak.regler.somArbeid
import no.nav.pleiepengerbarn.uttak.regler.somUtbetalingsgrader
import org.junit.jupiter.api.Assertions.*
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

        private val forventetGradVedAvkortingMotArbeid = Prosent(50)
        private val forventetUtbetalingsgraderVedAvkortingMotArbeid = mapOf(
                "123" to Prosent(50)
        )

        private val forventetGradVedGraderingMotTilsyn= Prosent(40)
        private val forventetUtbetalingsgraderVedGraderingMotTilsyn = mapOf(
                "123" to Prosent(40)
        )

        private val forventetGradInnvilgetÅrsakBarnetsDødsfall = Prosent(100)
        private val forventetUtbetalingsgraderInnvilgetÅrsakBarnetsDødsfall = mapOf(
                "123" to Prosent(100)
        )
    }

    @Test
    internal fun `Om barnet dør i en periode man er avkortet grunnet annen omsorgsperson skal denne avkortingen opphøre`() {
        val grunnlag = lagGrunnlagMedAnnenOmsorgsperson(
                denAndreOmsorgsPersonensGrad = Prosent(80)
        )
        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                dødsdato = null
        ))

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)

        uttaksplanFørRegelkjøring.print(grunnlag)

        assertEquals(1, uttaksplanFørRegelkjøring.perioder.size)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        uttaksplanEtterRegelkjøring.print(grunnlag)

        assertEquals(3, uttaksplanEtterRegelkjøring.perioder.size)

        // 1. Opprinnelig periode
        sjekkInnvilget(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-12"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = Prosent(20),
                forventedeUtbetalingsgrader = mapOf(
                        "123" to Prosent(20)
                )
        )
        // Forventer at perioden er delt i to. Første TOM dødsfall lik
        // Den andre perioden har man fått 50% ettersom det ikke er avkortet mot annen
        // omsorgsperson lengre.
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-07"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = Prosent(20),
                forventedeUtbetalingsgrader = mapOf(
                        "123" to Prosent(20)
                )
        )
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-12"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = Prosent(50),
                forventedeUtbetalingsgrader = mapOf(
                        "123" to Prosent(50)
                )
        )
        // En helt ny periode som er "sorgperioden" som går utover opprinnelig uttaksplan
        // Som strekker seg til 6 uker etter dødsfallet
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-13/2020-01-25"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.BarnetsDødsfall,
                forventetGrad = forventetGradInnvilgetÅrsakBarnetsDødsfall,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderInnvilgetÅrsakBarnetsDødsfall
        )
    }

    @Test
    internal fun `Om barnet dør i en periode man har fått avslag grunnet annen omsorgsperson forblir det et avslag`() {
        val grunnlag = lagGrunnlagMedAnnenOmsorgsperson(
                denAndreOmsorgsPersonensGrad = Prosent(81)
        )
        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                dødsdato = null
        ))

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)

        uttaksplanFørRegelkjøring.print(grunnlag)

        assertEquals(1, uttaksplanFørRegelkjøring.perioder.size)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        uttaksplanEtterRegelkjøring.print(grunnlag)

        assertEquals(2, uttaksplanEtterRegelkjøring.perioder.size)

        // 1. Opprinnelig periode
        sjekkAvslått(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-12"),
                forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.ForLavGrad)
        )

        // Forventer at perioden er delt i to. Første TOM dødsfall lik
        // Den andre perioden er lik med har også fått en ny AvslåttÅrsak 'BARNETS_DØDSFALL'
        sjekkAvslått(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-07"),
                forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.ForLavGrad)
        )
        sjekkAvslått(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-12"),
                forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.ForLavGrad, AvslåttÅrsaker.BarnetsDødsfall)
        )
    }

    @Test
    internal fun `Om barnet dør i midten av en innvilget periode gradert mot tilsyn`() {
        val grunnlag = lagGrunnlag(
                dødeIEnPeriodeGradertMotTilsyn = true
        )
        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                dødsdato = null
        ))

        val dødsdato = grunnlag.barn.dødsdato!!

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)

        uttaksplanFørRegelkjøring.print(grunnlag)

        assertEquals(4, uttaksplanFørRegelkjøring.perioder.size)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(6, uttaksplanEtterRegelkjøring.perioder.size)

        uttaksplanEtterRegelkjøring.print(grunnlag)

        // 1. Opprinnelige Periode
        var periode = LukketPeriode("2020-01-01/2020-01-20")
        sjekkInnvilget(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = periode,
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // Forventer at denne perioden nå er lik som den var - periode før dødsfall
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = periode,
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )

        // 2. Opprinnelige periode -  Et "hull" mellom to uttaksperioder
        periode = LukketPeriode("2020-01-21/2020-01-28")
        assertNull(uttaksplanFørRegelkjøring.perioder[periode])
        // I ny plan bør dette fortsatt være et hull - periode før dødsfall
        assertNull(uttaksplanEtterRegelkjøring.perioder[periode])

        // 3. Opprinnelige periode
        periode = LukketPeriode("2020-01-29/2020-01-31")
        sjekkInnvilget(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = periode,
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // Forventer at denne perioden skal være som den var - periode før dødsfall
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = periode,
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // 4. Opprinnelige periode
        periode = LukketPeriode("2020-02-01/2020-02-10")
        sjekkAvslått(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = periode,
                forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.IkkeMedlemIFolketrygden)
        )
        // Avslag skal forbli avslag, forventer det samme
        sjekkAvslått(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = periode,
                forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.IkkeMedlemIFolketrygden)
        )
        // 5. Opprinnelig periode
        sjekkInnvilget(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = LukketPeriode("2020-02-11/2020-03-01"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.GradertMotTilsyn,
                forventetGrad = forventetGradVedGraderingMotTilsyn,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedGraderingMotTilsyn
        )
        // Forventer at perioden er delt i to:
        //      1) 2020-02-11 - 2020-02-15 (Sistnevnte dødsdato)
        //         Denne perioden bør nå være Avkortet mot inntekt istedenfor Gradert mot tilsyn
        //      2) 2020-02-16 - 2020-03-01
        //         Denne perioden bør være avkortet mot inntekt
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-02-11/2020-02-15"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.GradertMotTilsyn,
                forventetGrad = forventetGradVedGraderingMotTilsyn,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedGraderingMotTilsyn
        )
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-02-16/2020-03-01"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // En helt ny periode som er "sorgperioden" som går utover opprinnelig uttaksplan
        // Som strekker seg til 6 uker etter dødsfallet
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-03-02/2020-03-29"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.BarnetsDødsfall,
                forventetGrad = forventetGradInnvilgetÅrsakBarnetsDødsfall,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderInnvilgetÅrsakBarnetsDødsfall
        )
    }

    @Test
    internal fun `Om barnet dør i midten av en innvilget periode avkortet mot inntekt`() {
        val grunnlag = lagGrunnlag(
                dødeIEnPeriodeAvkortetMotInntekt = true
        )
        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                dødsdato = null
        ))

        val dødsdato = grunnlag.barn.dødsdato!!

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)

        uttaksplanFørRegelkjøring.print(grunnlag)

        assertEquals(4, uttaksplanFørRegelkjøring.perioder.size)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(7, uttaksplanEtterRegelkjøring.perioder.size)

        uttaksplanEtterRegelkjøring.print(grunnlag)

        // 1. Opprinnelige Periode
        sjekkInnvilget(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-20"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // Barnet døde i denne perioden, så forventer nå at den har samme verdier, men er delt i to
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-01/2020-01-15"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-16/2020-01-20"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )

        // 2. Opprinnelige periode -  Et "hull" mellom to uttaksperioder
        var periode = LukketPeriode("2020-01-21/2020-01-28")
        assertNull(uttaksplanFørRegelkjøring.perioder[periode])
        // I ny plan skal denne nå være innvilget med 100%
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = periode,
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.BarnetsDødsfall,
                forventetGrad = forventetGradInnvilgetÅrsakBarnetsDødsfall,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderInnvilgetÅrsakBarnetsDødsfall
        )

        // 3. Opprinnelige periode
        periode = LukketPeriode("2020-01-29/2020-01-31")
        sjekkInnvilget(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = periode,
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // Forventer at denne perioden skal være som den var ettersom den er avkortet mot inntekt
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = periode,
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // 4. Opprinnelige periode
        periode = LukketPeriode("2020-02-01/2020-02-10")
        sjekkAvslått(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = periode,
                forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.IkkeMedlemIFolketrygden)
        )
        // Avslag skal forbli avslag, forventer det samme
        sjekkAvslått(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = periode,
                forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.IkkeMedlemIFolketrygden)
        )
        // 5. Opprinnelig periode
        sjekkInnvilget(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = LukketPeriode("2020-02-11/2020-03-01"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.GradertMotTilsyn,
                forventetGrad = forventetGradVedGraderingMotTilsyn,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedGraderingMotTilsyn
        )
        // Forventer at perioden er delt i to:
        //      1) 2020-02-11 - 2020-02-27 (Sistnevnte 6 uker etter dødsfallet)
        //         Denne perioden bør nå være Avkortet mot inntekt istedenfor Gradert mot tilsyn
        //      2) 2020-02-28 - 2020-03-01
        //         Denne perioden bør være avslått med en årsak; 'BARNETS_DØDSFALL'
        sjekkInnvilget(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-02-11/2020-02-27"),
                forventedeInnvilgetÅrsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        sjekkAvslått(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-02-28/2020-03-01"),
                forventetAvslåttÅrsaker = setOf(AvslåttÅrsaker.BarnetsDødsfall)
        )
    }

    @Test
    internal fun `Om barnet dør i en avslått periode`() {
        val grunnlag = lagGrunnlag(
                dødeIEnAvslåttPeriode = true
        )
        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                dødsdato = null
        ))

        val dødsdato = grunnlag.barn.dødsdato!!

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)

        uttaksplanFørRegelkjøring.print(grunnlag)

        assertEquals(4, uttaksplanFørRegelkjøring.perioder.size)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(5, uttaksplanEtterRegelkjøring.perioder.size)

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
        uttaksplanEtterRegelkjøring.perioder
                .filterKeys { it.tom.erLikEllerFør(dødsdato) }
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
        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                dødsdato = null
        ))

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)

        uttaksplanFørRegelkjøring.print(grunnlag)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(uttaksplanFørRegelkjøring, uttaksplanEtterRegelkjøring)
    }

    @Test
    internal fun `Om barnet dør før første søknadsperiode skal alle perioder blir avslått`() {
        val grunnlag = lagGrunnlag(
                dødeFørFørsteSøknadsperiode = true
        )

        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                dødsdato = null
        ))

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)

        uttaksplanFørRegelkjøring.print(grunnlag)

        assertEquals(4, uttaksplanFørRegelkjøring.perioder.size)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(4, uttaksplanEtterRegelkjøring.perioder.size)

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
        val antallFlaggSatt = listOf(
                dødeIEnAvslåttPeriode,
                dødeIEnPeriodeGradertMotTilsyn,
                dødeIEnPeriodeAvkortetMotInntekt,
                dødeFørFørsteSøknadsperiode,
                dødeEtterSisteSøknadsperiode)
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
                                helePerioden to ArbeidsforholdPeriodeInfo(
                                        jobberNormaltPerUke = Duration.ofHours(37).plusMinutes(30),
                                        skalJobbeProsent = Prosent(50)
                                )
                        )
                ).somArbeid(),
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

    private fun lagGrunnlagMedAnnenOmsorgsperson(
            denAndreOmsorgsPersonensGrad: Prosent
    ) : RegelGrunnlag {
        val helePerioden = LukketPeriode("2020-01-06/2020-01-12")
        val barnetsDødsdato = LocalDate.parse("2020-01-07")
        return RegelGrunnlag(
                barn = Barn(
                        dødsdato = barnetsDødsdato
                ),
                arbeid = mapOf(
                        "123" to mapOf(
                                helePerioden to ArbeidsforholdPeriodeInfo(
                                        jobberNormaltPerUke = Duration.ofHours(37).plusMinutes(30),
                                        skalJobbeProsent = Prosent(50)
                                )
                        )
                ).somArbeid(),
                søknadsperioder = listOf(
                        helePerioden
                ),
                tilsynsbehov = mapOf(
                        helePerioden to Tilsynsbehov(
                                prosent = TilsynsbehovStørrelse.PROSENT_100
                        )
                ),
                andrePartersUttaksplan = listOf(
                        Uttaksplan(
                                perioder = mapOf(
                                        helePerioden to InnvilgetPeriode(
                                                knekkpunktTyper = setOf(),
                                                grad = denAndreOmsorgsPersonensGrad,
                                                utbetalingsgrader = mapOf(
                                                        "123" to Prosent(100)
                                                ).somUtbetalingsgrader(),
                                                årsak = InnvilgetÅrsak(
                                                        årsak = InnvilgetÅrsaker.AvkortetMotInntekt,
                                                        hjemler = setOf()
                                                )
                                        )
                                )
                        )
                )
        )
    }

    private fun Uttaksplan.uttaksPeriodeInfoSomInneholder(dato: LocalDate) : UttaksPeriodeInfo {
        return perioder.filterKeys { it.inneholder(dato) }.values.firstOrNull() ?: throw IllegalStateException("Fant ikke uttaksperide mot tom=$dato")
    }
}