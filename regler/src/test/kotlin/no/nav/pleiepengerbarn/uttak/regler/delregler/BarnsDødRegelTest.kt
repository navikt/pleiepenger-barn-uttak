package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkIkkeOppfylt
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkIkkeOppfyltPeriodeInneholderIkkeOppfyltÅrsaker
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkOppfylt
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.erLikEllerFør
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.inneholder
import no.nav.pleiepengerbarn.uttak.regler.somArbeid
import no.nav.pleiepengerbarn.uttak.regler.somUtbetalingsgrader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class BarnsDødRegelTest {
    private companion object {
        private val forventetGradVedAvkortingMotArbeid = Prosent(50)
        private val forventetUtbetalingsgraderVedAvkortingMotArbeid = mapOf(
                "123" to Prosent(50)
        )

        private val forventetGradVedGraderingMotTilsyn= Prosent(40)
        private val forventetUtbetalingsgraderVedGraderingMotTilsyn = mapOf(
                "123" to Prosent(40)
        )

        private val forventetGradOppfyltÅrsakBarnetsDødsfall = Prosent(100)
        private val forventetUtbetalingsgraderOppfyltÅrsakBarnetsDødsfall = mapOf(
                "123" to Prosent(100)
        )

        private const val aktørIdSøker = "123"
        private const val aktørIdBarn = "456"

    }

    @Test
    internal fun `Om barnet dør i en periode man er avkortet grunnet annen omsorgsperson skal denne avkortingen opphøre`() {
        val grunnlag = lagGrunnlagMedAnnenOmsorgsperson(
                denAndreOmsorgspersonensGrad = Prosent(80)
        )
        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                aktørId = aktørIdBarn,
                dødsdato = null
        ))

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)


        assertEquals(1, uttaksplanFørRegelkjøring.perioder.size)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)


        assertEquals(8, uttaksplanEtterRegelkjøring.perioder.size)

        // 1. Opprinnelig periode
        sjekkOppfylt(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-10"),
                forventedeOppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN,
                forventetGrad = Prosent(20),
                forventedeUtbetalingsgrader = mapOf(
                        "123" to Prosent(20)
                )
        )
        // Forventer at perioden er delt i to. Første TOM dødsfall lik
        // Den andre perioden har man fått 50% ettersom det ikke er avkortet mot annen
        // omsorgsperson lengre.
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-07"),
                forventedeOppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN,
                forventetGrad = Prosent(20),
                forventedeUtbetalingsgrader = mapOf(
                        "123" to Prosent(20)
                )
        )
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-10"),
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = Prosent(50),
                forventedeUtbetalingsgrader = mapOf(
                        "123" to Prosent(50)
                )
        )
        // Nyeperioder som er "sorgperioden" som går utover opprinnelig uttaksplan
        // Som strekker seg til 6 uker etter dødsfallet
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-01-13/2020-01-17"),
                    LukketPeriode("2020-01-20/2020-01-24"),
                    LukketPeriode("2020-01-27/2020-01-31"),
                    LukketPeriode("2020-02-03/2020-02-07"),
                    LukketPeriode("2020-02-10/2020-02-14"),
                    LukketPeriode("2020-02-17/2020-02-19")
                ),
                forventedeOppfyltÅrsak = Årsak.OPPFYLT_PGA_BARNETS_DØDSFALL,
                forventetGrad = forventetGradOppfyltÅrsakBarnetsDødsfall,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderOppfyltÅrsakBarnetsDødsfall
        )
    }

    @Test
    internal fun `Om barnet dør i midten av en oppfylt periode gradert mot tilsyn`() {
        val grunnlag = lagGrunnlag(
                dødeIEnPeriodeGradertMotTilsyn = true
        )
        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                aktørId = aktørIdBarn,
                dødsdato = null
        ))

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)


        assertEquals(10, uttaksplanFørRegelkjøring.perioder.size)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(14, uttaksplanEtterRegelkjøring.perioder.size)


        // 1. Opprinnelige Periode
        sjekkOppfylt(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-01-01/2020-01-03"),
                    LukketPeriode("2020-01-06/2020-01-10"),
                    LukketPeriode("2020-01-13/2020-01-17"),
                    LukketPeriode("2020-01-20/2020-01-20")
                ),
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // Forventer at denne perioden nå er lik som den var - periode før dødsfall
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-01-01/2020-01-03"),
                    LukketPeriode("2020-01-06/2020-01-10"),
                    LukketPeriode("2020-01-13/2020-01-17"),
                    LukketPeriode("2020-01-20/2020-01-20")
                ),
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )

        // 2. Opprinnelige periode -  Et "hull" mellom to uttaksperioder
        var periode = LukketPeriode("2020-01-21/2020-01-28")
        assertNull(uttaksplanFørRegelkjøring.perioder[periode])
        // I ny plan bør dette fortsatt være et hull - periode før dødsfall
        assertNull(uttaksplanEtterRegelkjøring.perioder[periode])

        // 3. Opprinnelige periode
        periode = LukketPeriode("2020-01-29/2020-01-31")
        sjekkOppfylt(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = periode,
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // Forventer at denne perioden skal være som den var - periode før dødsfall
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = periode,
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // 4. Opprinnelige periode
        sjekkIkkeOppfylt(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-02-03/2020-02-07"),
                    LukketPeriode("2020-02-10/2020-02-10")
                ),
                forventetIkkeOppfyltÅrsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT)
        )
        // Avslag skal forbli avslag, forventer det samme
        sjekkIkkeOppfylt(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-02-03/2020-02-07"),
                    LukketPeriode("2020-02-10/2020-02-10")
                ),
                forventetIkkeOppfyltÅrsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT)
        )
        // 5. Opprinnelig periode
        sjekkOppfylt(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-02-11/2020-02-14"),
                    LukketPeriode("2020-02-17/2020-02-21"),
                    LukketPeriode("2020-02-24/2020-02-28")
                ),
                forventedeOppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN,
                forventetGrad = forventetGradVedGraderingMotTilsyn,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedGraderingMotTilsyn
        )
        // Forventer at perioden er delt i to:
        //      1) 2020-02-11 - 2020-02-15 (Sistnevnte dødsdato)
        //         Denne perioden bør nå være Avkortet mot inntekt istedenfor Gradert mot tilsyn
        //      2) 2020-02-16 - 2020-03-01
        //         Denne perioden bør være avkortet mot inntekt
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-02-11/2020-02-14"),
                forventedeOppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN,
                forventetGrad = forventetGradVedGraderingMotTilsyn,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedGraderingMotTilsyn
        )
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-02-17/2020-02-21"),
                    LukketPeriode("2020-02-24/2020-02-28")
                ),
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // En helt ny periode som er "sorgperioden" som går utover opprinnelig uttaksplan
        // Som strekker seg til 6 uker etter dødsfallet
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-03-02/2020-03-06"),
                    LukketPeriode("2020-03-09/2020-03-13"),
                    LukketPeriode("2020-03-16/2020-03-20"),
                    LukketPeriode("2020-03-23/2020-03-27")
                ),
                forventedeOppfyltÅrsak = Årsak.OPPFYLT_PGA_BARNETS_DØDSFALL,
                forventetGrad = forventetGradOppfyltÅrsakBarnetsDødsfall,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderOppfyltÅrsakBarnetsDødsfall
        )
    }

    @Test
    internal fun `Om barnet dør i midten av en oppfylt periode avkortet mot inntekt`() {
        val grunnlag = lagGrunnlag(
                dødeIEnPeriodeAvkortetMotInntekt = true
        )
        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                aktørId = aktørIdBarn,
                dødsdato = null
        ))

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)


        assertEquals(10, uttaksplanFørRegelkjøring.perioder.size)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(14, uttaksplanEtterRegelkjøring.perioder.size)


        // 1. Opprinnelige Periode
        sjekkOppfylt(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-01-01/2020-01-03"),
                    LukketPeriode("2020-01-06/2020-01-10"),
                    LukketPeriode("2020-01-13/2020-01-17"),
                    LukketPeriode("2020-01-20/2020-01-20")
                ),
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // Barnet døde i denne perioden, så forventer nå at den har samme verdier, men er delt i to
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-01-01/2020-01-03"),
                    LukketPeriode("2020-01-06/2020-01-10"),
                    LukketPeriode("2020-01-13/2020-01-15"),
                ),
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-01-16/2020-01-17"),
                    LukketPeriode("2020-01-20/2020-01-20")
                ),
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )

        // 2. Opprinnelige periode -  Et "hull" mellom to uttaksperioder
        var periode = LukketPeriode("2020-01-21/2020-01-28")
        assertNull(uttaksplanFørRegelkjøring.perioder[periode])
        // I ny plan skal denne nå være oppfylt med 100% (med hull i helg)
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-01-21/2020-01-24"),
                    LukketPeriode("2020-01-27/2020-01-28")
                ),
                forventedeOppfyltÅrsak = Årsak.OPPFYLT_PGA_BARNETS_DØDSFALL,
                forventetGrad = forventetGradOppfyltÅrsakBarnetsDødsfall,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderOppfyltÅrsakBarnetsDødsfall
        )

        // 3. Opprinnelige periode
        periode = LukketPeriode("2020-01-29/2020-01-31")
        sjekkOppfylt(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventetPeriode = periode,
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // Forventer at denne perioden skal være som den var ettersom den er avkortet mot inntekt
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = periode,
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        // 4. Opprinnelige periode
        sjekkIkkeOppfylt(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-02-03/2020-02-07"),
                    LukketPeriode("2020-02-10/2020-02-10")
                ),
                forventetIkkeOppfyltÅrsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT)
        )
        // Avslag skal forbli avslag, forventer det samme
        sjekkIkkeOppfylt(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-02-03/2020-02-07"),
                    LukketPeriode("2020-02-10/2020-02-10")
                ),
                forventetIkkeOppfyltÅrsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT)
        )
        // 5. Opprinnelig periode
        sjekkOppfylt(
                uttaksplan = uttaksplanFørRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-02-11/2020-02-14"),
                    LukketPeriode("2020-02-17/2020-02-21"),
                    LukketPeriode("2020-02-24/2020-02-28")
                ),
                forventedeOppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN,
                forventetGrad = forventetGradVedGraderingMotTilsyn,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedGraderingMotTilsyn
        )
        // Forventer at perioden er delt i to:
        //      1) 2020-02-11 - 2020-02-27 (Sistnevnte 6 uker etter dødsfallet)
        //         Denne perioden bør nå være Avkortet mot inntekt istedenfor Gradert mot tilsyn
        //      2) 2020-02-28 - 2020-03-01
        //         Denne perioden bør være ikke oppfylt med en årsak; 'BARNETS_DØDSFALL'
        sjekkOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventedePerioder = listOf(
                    LukketPeriode("2020-02-11/2020-02-14"),
                    LukketPeriode("2020-02-17/2020-02-21"),
                    LukketPeriode("2020-02-24/2020-02-27")
                ),
                forventedeOppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
                forventetGrad = forventetGradVedAvkortingMotArbeid,
                forventedeUtbetalingsgrader = forventetUtbetalingsgraderVedAvkortingMotArbeid
        )
        sjekkIkkeOppfylt(
                uttaksplan = uttaksplanEtterRegelkjøring,
                forventetPeriode = LukketPeriode("2020-02-28/2020-02-28"),
                forventetIkkeOppfyltÅrsaker = setOf(Årsak.BARNETS_DØDSFALL)
        )
    }

    @Test
    internal fun `Om barnet dør i en ikke oppfylt periode`() {
        val grunnlag = lagGrunnlag(
                dødeIEnIkkeOppfyltPeriode = true
        )
        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                aktørId = aktørIdBarn,
                dødsdato = null
        ))

        val dødsdato = grunnlag.barn.dødsdato!!

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)


        assertEquals(10, uttaksplanFørRegelkjøring.perioder.size)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(11, uttaksplanEtterRegelkjøring.perioder.size)


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


        // Alle perioder etter dødsdato bør være ikke oppfylt med rett årsak
        uttaksplanEtterRegelkjøring.perioder
                .filterKeys { it.fom.isAfter(dødsdato) }
                .forEach { (periode, periodeInfo) ->
                    assertThat(periodeInfo.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                    sjekkIkkeOppfyltPeriodeInneholderIkkeOppfyltÅrsaker(
                            uttaksplan = uttaksplanEtterRegelkjøring,
                            forventetPeriode = periode,
                            forventetIkkeOppfyltÅrsaker = setOf(Årsak.BARNETS_DØDSFALL)
                    )
                }
    }

    @Test
    internal fun `Om barnet fortsatt lever har ikke kjøring av regel noen effekt`() {
        val grunnlag = lagGrunnlag()

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)


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
                aktørId = aktørIdBarn,
                dødsdato = null
        ))

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)


        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(uttaksplanFørRegelkjøring, uttaksplanEtterRegelkjøring)
    }

    @Test
    internal fun `Om barnet dør før første søknadsperiode skal alle perioder blir ikke oppfylt`() {
        val grunnlag = lagGrunnlag(
                dødeFørFørsteSøknadsperiode = true
        )

        val grunnlagUtenBarnetsDødsdato = grunnlag.copy(barn = Barn(
                aktørId = aktørIdBarn,
                dødsdato = null
        ))

        val uttaksplanFørRegelkjøring = UttakTjeneste.uttaksplan(grunnlagUtenBarnetsDødsdato)


        assertEquals(10, uttaksplanFørRegelkjøring.perioder.size)

        val uttaksplanEtterRegelkjøring = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(10, uttaksplanEtterRegelkjøring.perioder.size)


        assertEquals(uttaksplanFørRegelkjøring.perioder.size, uttaksplanEtterRegelkjøring.perioder.size)

        uttaksplanEtterRegelkjøring.perioder.forEach { (periode, _) ->
            sjekkIkkeOppfyltPeriodeInneholderIkkeOppfyltÅrsaker(
                    uttaksplan = uttaksplanEtterRegelkjøring,
                    forventetPeriode = periode,
                    forventetIkkeOppfyltÅrsaker = setOf(Årsak.BARNETS_DØDSFALL)
            )
        }
    }

    private fun lagGrunnlag(
        dødeIEnIkkeOppfyltPeriode: Boolean = false,
        dødeIEnPeriodeGradertMotTilsyn: Boolean = false,
        dødeIEnPeriodeAvkortetMotInntekt: Boolean = false,
        dødeFørFørsteSøknadsperiode: Boolean = false,
        dødeEtterSisteSøknadsperiode: Boolean = false
    ) : RegelGrunnlag {
        val antallFlaggSatt = listOf(
                dødeIEnIkkeOppfyltPeriode,
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
                    dødeIEnIkkeOppfyltPeriode -> LocalDate.parse("2020-02-06")
                    dødeIEnPeriodeGradertMotTilsyn -> LocalDate.parse("2020-02-15")
                    dødeIEnPeriodeAvkortetMotInntekt -> LocalDate.parse("2020-01-15")
                    dødeFørFørsteSøknadsperiode-> LocalDate.parse("2019-12-31")
                    dødeEtterSisteSøknadsperiode -> LocalDate.parse("2020-03-02")
                    else -> null
                }

        return RegelGrunnlag(
                behandlingUUID = UUID.randomUUID().toString(),
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                        aktørId = aktørIdBarn,
                        dødsdato = barnetsDødsdato
                ),
                arbeid = mapOf(
                        "123" to mapOf(
                                helePerioden to ArbeidsforholdPeriodeInfo(
                                        jobberNormalt = Duration.ofHours(7).plusMinutes(30),
                                        jobberNå = Duration.ofHours(3).plusMinutes(45)
                                )
                        )
                ).somArbeid(),
                søktUttak = listOf(
                        SøktUttak(LukketPeriode("2020-01-01/2020-01-20")),
                        SøktUttak(LukketPeriode("2020-01-29/2020-03-01"))
                ),
                pleiebehov = mapOf(
//                    helePerioden.copy(tom = helePerioden.tom.plusWeeks(6)) to Pleiebehov.PROSENT_100
                        helePerioden to Pleiebehov.PROSENT_100
                ),
                tilsynsperioder = mapOf(
                        LukketPeriode("2020-02-11/2020-03-01") to Prosent(60)
                ).somTilsynperioder(),
                inngangsvilkår = mapOf(
                    "MEDLEMSKAPSVILKÅRET" to listOf(Vilkårsperiode(LukketPeriode("2020-02-01/2020-02-10"), Utfall.IKKE_OPPFYLT))
                )
        )
    }

    private fun lagGrunnlagMedAnnenOmsorgsperson(
        denAndreOmsorgspersonensGrad: Prosent
    ) : RegelGrunnlag {
        val helePerioden = LukketPeriode("2020-01-06/2020-01-12")
        val barnetsDødsdato = LocalDate.parse("2020-01-07")
        return RegelGrunnlag(
                behandlingUUID = UUID.randomUUID().toString(),
                søker = Søker(
                        aktørId = aktørIdSøker
                ),
                barn = Barn(
                        aktørId = aktørIdBarn,
                        dødsdato = barnetsDødsdato
                ),
                arbeid = mapOf(
                        "123" to mapOf(
                                helePerioden to ArbeidsforholdPeriodeInfo(
                                        jobberNormalt = Duration.ofHours(7).plusMinutes(30),
                                        jobberNå = Duration.ofHours(3).plusMinutes(45)
                                )
                        )
                ).somArbeid(),
                søktUttak = listOf(
                        SøktUttak(helePerioden)
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_100
                ),
                andrePartersUttaksplan = mapOf(
                        "999" to Uttaksplan(
                                perioder = mapOf(
                                        helePerioden to UttaksperiodeInfo.oppfylt(
                                                kildeBehandlingUUID = UUID.randomUUID().toString(),
                                                knekkpunktTyper = setOf(),
                                                uttaksgrad = denAndreOmsorgspersonensGrad,
                                                utbetalingsgrader = mapOf(
                                                        "123" to Prosent(100)
                                                ).somUtbetalingsgrader(),
                                                søkersTapteArbeidstid = Prosent(100),
                                                oppgittTilsyn = null,
                                                årsak = Årsak.AVKORTET_MOT_INNTEKT,
                                                pleiebehov = Pleiebehov.PROSENT_100.prosent,
                                                annenPart = AnnenPart.ALENE,
                                                nattevåk = null,
                                                beredskap = null
                                        )
                                )
                        )
                )
        )
    }

    private fun Uttaksplan.uttaksPeriodeInfoSomInneholder(dato: LocalDate) : UttaksperiodeInfo {
        return perioder.filterKeys { it.inneholder(dato) }.values.firstOrNull() ?: throw IllegalStateException("Fant ikke uttaksperide mot tom=$dato")
    }
}