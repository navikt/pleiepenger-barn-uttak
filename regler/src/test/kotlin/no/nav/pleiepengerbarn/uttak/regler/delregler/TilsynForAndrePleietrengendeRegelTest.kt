package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.NULL_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

internal class TilsynForAndrePleietrengendeRegelTest {

    private val regel: TilsynForAndrePleietrengendeRegel = TilsynForAndrePleietrengendeRegel()
    private val periode: LukketPeriode = LukketPeriode("2020-01-01/2020-01-31")

    @Test
    internal fun `Søker uten andre pleietrengende`() {
        val resultat =
            regel.kjør(periode, grunnlagUtenAndrePleietrengende())
        Assertions.assertThat(resultat).isInstanceOf(TilBeregningAvGrad::class.java)
        val tilBeregningAvGrad = resultat as TilBeregningAvGrad
        Assertions.assertThat(tilBeregningAvGrad.overstyrtÅrsak).isNull()
    }

    @Test
    internal fun `Søker med behandling for annen pleietrengende med lavere prio`() {
        val resultat =
            regel.kjør(periode, grunnlagMedBehandlingMedLaverePrio())
        Assertions.assertThat(resultat).isInstanceOf(TilBeregningAvGrad::class.java)
        val tilBeregningAvGrad = resultat as TilBeregningAvGrad
        Assertions.assertThat(tilBeregningAvGrad.overstyrtÅrsak).isNull()
    }

    @Test
    internal fun `Søker med behandling for annen pleietrengende med høyere prio, men avslått uttaksresultat`() {
        val resultat =
            regel.kjør(periode, grunnlagMedBehandlingMedHøyerePrioMenAvslåttUttaksresultat())
        Assertions.assertThat(resultat).isInstanceOf(TilBeregningAvGrad::class.java)
        val tilBeregningAvGrad = resultat as TilBeregningAvGrad
        Assertions.assertThat(tilBeregningAvGrad.overstyrtÅrsak).isNull()
    }

    @Test
    internal fun `Søker med behandling for annen pleietrengende med hæyere prio, og oppfylt uttaksresultat`() {
        val resultat =
            regel.kjør(periode, grunnlagMedBehandlingMedHøyerePrio())
        Assertions.assertThat(resultat).isInstanceOf(IkkeOppfylt::class.java)
        val tilBeregningAvGrad = resultat as IkkeOppfylt
        Assertions.assertThat(tilBeregningAvGrad.årsaker).isNotNull()
        Assertions.assertThat(tilBeregningAvGrad.årsaker).contains(Årsak.ANNEN_PLEIETRENGENDE_MED_HØYERE_PRIO)

    }


    private fun nesteSaksnummer(): Saksnummer = UUID.randomUUID().toString().takeLast(19)
    private fun nesteBehandlingUUID() = UUID.randomUUID()


    private fun grunnlagUtenAndrePleietrengende(): RegelGrunnlag {
        return RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            //Feltene nedenfor er ikke relevante for testen
            behandlingUUID = UUID.randomUUID(),
            søker = Søker(aktørId = "456"),
            pleiebehov = mapOf(),
            søktUttak = listOf(),
            arbeid = listOf(),
            barn = Barn(aktørId = "123"),
        )
    }

    private fun grunnlagMedBehandlingMedLaverePrio(): RegelGrunnlag {
        val denneBehandlingen = nesteBehandlingUUID()
        val enAnnenBehandling = nesteBehandlingUUID()
        return RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            kravprioritetForEgneBehandlinger = mapOf(Pair(periode, listOf<UUID>(denneBehandlingen, enAnnenBehandling))),
            egneUttaksplanerAllePleietrengendePerBehandling = mapOf(),
            //Feltene nedenfor er ikke relevante for testen
            behandlingUUID = denneBehandlingen,
            søker = Søker(aktørId = "456"),
            pleiebehov = mapOf(),
            søktUttak = listOf(),
            arbeid = listOf(),
            barn = Barn(aktørId = "123"),
        )
    }

    private fun grunnlagMedBehandlingMedHøyerePrioMenAvslåttUttaksresultat(): RegelGrunnlag {
        val denneBehandlingen = nesteBehandlingUUID()
        val enAnnenBehandling = nesteBehandlingUUID()
        return RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            kravprioritetForEgneBehandlinger = mapOf(Pair(periode, listOf<UUID>(enAnnenBehandling, denneBehandlingen))),
            egneUttaksplanerAllePleietrengendePerBehandling = mapOf(
                Pair(
                    enAnnenBehandling,
                    Uttaksplan(perioder = mapOf(Pair(periode, avslåttUttaksperiode())))
                )
            ),
            //Feltene nedenfor er ikke relevante for testen
            behandlingUUID = denneBehandlingen,
            søker = Søker(aktørId = "456"),
            pleiebehov = mapOf(),
            søktUttak = listOf(),
            arbeid = listOf(),
            barn = Barn(aktørId = "123"),
        )
    }


    private fun grunnlagMedBehandlingMedHøyerePrio(): RegelGrunnlag {
        val denneBehandlingen = nesteBehandlingUUID()
        val enAnnenBehandling = nesteBehandlingUUID()
        return RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            kravprioritetForEgneBehandlinger = mapOf(Pair(periode, listOf<UUID>(enAnnenBehandling, denneBehandlingen))),
            egneUttaksplanerAllePleietrengendePerBehandling = mapOf(
                Pair(
                    enAnnenBehandling,
                    Uttaksplan(perioder = mapOf(Pair(periode, oppfyltUttaksperiode())))
                )
            ),
            //Feltene nedenfor er ikke relevante for testen
            behandlingUUID = denneBehandlingen,
            søker = Søker(aktørId = "456"),
            pleiebehov = mapOf(),
            søktUttak = listOf(),
            arbeid = listOf(),
            barn = Barn(aktørId = "123"),
        )
    }


    private fun avslåttUttaksperiode() = UttaksperiodeInfo(
        utfall = Utfall.IKKE_OPPFYLT,
        utbetalingsgrader = listOf(),
        annenPart = AnnenPart.ALENE,
        beredskap = null,
        nattevåk = null,
        graderingMotTilsyn = GraderingMotTilsyn(
            etablertTilsyn = NULL_PROSENT,
            overseEtablertTilsynÅrsak = null,
            andreSøkeresTilsyn = NULL_PROSENT,
            tilgjengeligForSøker = NULL_PROSENT,
            andreSøkeresTilsynReberegnet = false
        ),
        kildeBehandlingUUID = "123",
        oppgittTilsyn = Duration.ZERO,
        pleiebehov = Pleiebehov.PROSENT_100.prosent,
        søkersTapteArbeidstid = null,
        uttaksgrad = HUNDRE_PROSENT,
        årsaker = setOf(Årsak.BARNETS_DØDSFALL),
        uttaksgradUtenReduksjonGrunnetInntektsgradering = null,
        uttaksgradMedReduksjonGrunnetInntektsgradering = null,
    )

    private fun oppfyltUttaksperiode() = UttaksperiodeInfo(
        utfall = Utfall.OPPFYLT,
        utbetalingsgrader = listOf(),
        annenPart = AnnenPart.ALENE,
        beredskap = null,
        nattevåk = null,
        graderingMotTilsyn = GraderingMotTilsyn(
            etablertTilsyn = NULL_PROSENT,
            overseEtablertTilsynÅrsak = null,
            andreSøkeresTilsyn = NULL_PROSENT,
            tilgjengeligForSøker = NULL_PROSENT,
            andreSøkeresTilsynReberegnet = false
        ),
        kildeBehandlingUUID = "123",
        oppgittTilsyn = Duration.ZERO,
        pleiebehov = Pleiebehov.PROSENT_100.prosent,
        søkersTapteArbeidstid = null,
        uttaksgrad = HUNDRE_PROSENT,
        årsaker = setOf(Årsak.AVKORTET_MOT_INNTEKT),
        uttaksgradUtenReduksjonGrunnetInntektsgradering = null,
        uttaksgradMedReduksjonGrunnetInntektsgradering = null,
    )


}
