package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkIkkeOppfylt
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkOppfylt
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.prosent
import no.nav.pleiepengerbarn.uttak.regler.somArbeid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class UttaksplanReglerKombinasjonsTest {

    private companion object {
        private val helePerioden = LukketPeriode("2020-01-06/2020-01-12")
        private val forventetGrad = Prosent(50)
        private val forventedeUtbetalingsgrader = mapOf("123" to Prosent(50))

        private const val aktørIdSøker = "123"
        private const val aktørIdBarn = "456"
    }

    @Test
    internal fun `Søker og barn dør samme dag`() {
        val søkersDødsdato = LocalDate.parse("2020-01-09")
        val søkersFødselsdato = søkersDødsdato.minusYears(50)

        val grunnlag = lagRegelGrunnlag(
                søkersFødselsdato = søkersFødselsdato,
                søkersDødsdato = søkersDødsdato,
                barnetsDødsdato = søkersDødsdato
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(3, uttaksplan.perioder.size)

        // Frem til dødsfallene uendret
        sjekkOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-09"),
                forventetGrad = forventetGrad,
                forventedeUtbetalingsgrader = forventedeUtbetalingsgrader,
                forventedeOppfyltÅrsak = Årsak.FULL_DEKNING
        )

        // Perioden som var oppfylt er nå ikke oppfylt
        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-10/2020-01-12"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.SØKERS_DØDSFALL
                )
        )

        // Blitt lagt til 6 ukers sorgperiode pga. barnets død som
        // I etterkant har blitt ikke oppfylt grunnet søkers død
        val seksUkerEtterBarnetsDød =
                søkersDødsdato.plusDays(1).plusWeeks(6)
        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-13/$seksUkerEtterBarnetsDød"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.SØKERS_DØDSFALL
                )
        )
    }

    @Test
    internal fun `Søker dør i sorgperioden etter barnets død`() {
        val barnetsDødsdato = LocalDate.parse("2020-01-09")
        val søkersDødsdato = barnetsDødsdato.plusWeeks(4)
        val søkersFødselsdato = søkersDødsdato.minusYears(30)

        val grunnlag = lagRegelGrunnlag(
                søkersFødselsdato = søkersFødselsdato,
                søkersDødsdato = søkersDødsdato,
                barnetsDødsdato = barnetsDødsdato
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(4, uttaksplan.perioder.size)

        // Frem til barnets dødsfall uendret
        sjekkOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-09"),
                forventetGrad = forventetGrad,
                forventedeUtbetalingsgrader = forventedeUtbetalingsgrader,
                forventedeOppfyltÅrsak = Årsak.FULL_DEKNING
        )

        // Etter dødsfall fortsatt avkortet mot inntekt
        sjekkOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-10/2020-01-12"),
                forventetGrad = forventetGrad,
                forventedeUtbetalingsgrader = forventedeUtbetalingsgrader,
                forventedeOppfyltÅrsak = Årsak.FULL_DEKNING
        )
        // Sorgperioden frem til søkers edød
        sjekkOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-13/2020-02-06"),
                forventetGrad = Prosent(100),
                forventedeUtbetalingsgrader = mapOf("123" to Prosent(100)),
                forventedeOppfyltÅrsak = Årsak.OPPFYLT_PGA_BARNETS_DØDSFALL
        )
        // Sorgperiode etter søkers død
        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-02-07/2020-02-21"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.SØKERS_DØDSFALL
                )
        )
    }

    @Test
    internal fun `Søker dør rett etter fylte 70`() {
        val søkersDødsdato = LocalDate.parse("2020-01-09")
        val søkersFødselsdato = søkersDødsdato.minusYears(70).minusDays(2)

        val grunnlag = lagRegelGrunnlag(
                søkersFødselsdato = søkersFødselsdato,
                søkersDødsdato = søkersDødsdato,
                barnetsDødsdato = null
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertEquals(3, uttaksplan.perioder.size)

        // Frem til fylte 70
        sjekkOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-06/2020-01-07"),
                forventetGrad = forventetGrad,
                forventedeUtbetalingsgrader = forventedeUtbetalingsgrader,
                forventedeOppfyltÅrsak = Årsak.FULL_DEKNING
        )

        // Frem til død
        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-08/2020-01-09"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.SØKERS_ALDER
                )
        )
        // Etter død
        sjekkIkkeOppfylt(
                uttaksplan = uttaksplan,
                forventetPeriode = LukketPeriode("2020-01-10/2020-01-12"),
                forventetIkkeOppfyltÅrsaker = setOf(
                        Årsak.SØKERS_ALDER,
                        Årsak.SØKERS_DØDSFALL
                )
        )
    }

    private fun lagRegelGrunnlag(
            søkersFødselsdato: LocalDate,
            søkersDødsdato: LocalDate?,
            barnetsDødsdato: LocalDate?
    ) : RegelGrunnlag {
        return RegelGrunnlag(
                behandlingUUID = UUID.randomUUID().toString(),
                søker = Søker(
                        aktørId = aktørIdSøker,
                        fødselsdato = søkersFødselsdato,
                        dødsdato = søkersDødsdato
                ),
                barn = Barn(
                        aktørId = aktørIdBarn,
                        dødsdato = barnetsDødsdato
                ),
                arbeid = mapOf(
                        "123" to mapOf(
                                helePerioden to ArbeidsforholdPeriodeInfo(
                                        jobberNormalt = Duration.ofHours(7).plusMinutes(30),
                                        jobberNå = Duration.ofHours(7).plusMinutes(30).prosent(50)
                                )
                        )
                ).somArbeid(),
                søktUttak = listOf(
                        SøktUttak(helePerioden)
                ),
                pleiebehov = mapOf(
                        helePerioden to Pleiebehov.PROSENT_100
                )
        )
    }
}