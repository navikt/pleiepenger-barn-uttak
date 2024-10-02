package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.NULL_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class RegelGrunnlagExtKtTest {

    @Test
    internal fun `Sjekk at periode som overlapper med annen part gir MED_ANDRE`() {
        val grunnlag = dummyGrunnlag().copy(
            andrePartersUttaksplanPerBehandling = mapOf(
                UUID.randomUUID() to uttaksplan("2021-01-01/2021-01-31"),
                UUID.randomUUID() to uttaksplan("2021-02-01/2021-02-28")
            )

        )
        val overlapperMedAnnenPart = grunnlag.annenPart(LukketPeriode("2021-02-10/2021-02-20"))
        assertThat(overlapperMedAnnenPart).isEqualTo(AnnenPart.MED_ANDRE)
    }

    @Test
    internal fun `Sjekk at periode som ikke overlapper med annen part gir ALENE`() {
        val grunnlag = dummyGrunnlag().copy(
            andrePartersUttaksplanPerBehandling = mapOf(
                UUID.randomUUID() to uttaksplan("2021-01-01/2021-01-31"),
                UUID.randomUUID() to uttaksplan("2021-03-01/2021-03-31")
            )

        )
        val overlapperMedAnnenPart = grunnlag.annenPart(LukketPeriode("2021-02-10/2021-02-20"))
        assertThat(overlapperMedAnnenPart).isEqualTo(AnnenPart.ALENE)
    }

    private fun uttaksplan(perioderString: String): Uttaksplan {
        return Uttaksplan(
            perioder = mapOf(
                LukketPeriode(perioderString) to UttaksperiodeInfo.oppfylt(
                    // Fyll med dummy data
                    kildeBehandlingUUID = UUID.randomUUID().toString(),
                    uttaksgrad = HUNDRE_PROSENT,
                    uttaksgradUtenReduksjonGrunnetInntektsgradering = HUNDRE_PROSENT,
                    uttaksgradMedReduksjonGrunnetInntektsgradering = null,
                    årsak = Årsak.FULL_DEKNING,
                    pleiebehov = HUNDRE_PROSENT,
                    knekkpunktTyper = setOf(),
                    utbetalingsgrader = listOf(),
                    søkersTapteArbeidstid = HUNDRE_PROSENT,
                    oppgittTilsyn = null,
                    annenPart = AnnenPart.ALENE,
                    nattevåk = null,
                    beredskap = null,
                    utenlandsopphold = Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN),
                )
            )
        )
    }

    private fun dummyGrunnlag(): RegelGrunnlag {
        return RegelGrunnlag(
            saksnummer = UUID.randomUUID().toString(),
            behandlingUUID = UUID.randomUUID(),
            søker = Søker(aktørId = "123"),
            barn = Barn(aktørId = "456"),
            pleiebehov = mapOf(),
            søktUttak = listOf(),
            arbeid = listOf()
        )
    }

}
