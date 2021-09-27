package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class BarnsDødPeriodeRegelTest {

    private val regel: BarnsDødPeriodeRegel = BarnsDødPeriodeRegel()

    @Test
    internal fun `Barn dør etter periode`() {
        val resultat = regel.kjør(LukketPeriode("2020-01-01/2020-01-31"), grunnlagMedDødtBarn(LocalDate.parse("2020-02-15"), RettVedDød.RETT_6_UKER))
        assertThat(resultat).isInstanceOf(TilBeregningAvGrad::class.java)
        val tilBeregningAvGrad = resultat as TilBeregningAvGrad
        assertThat(tilBeregningAvGrad.overstyrtÅrsak).isNull()
    }

    @Test
    internal fun `Periode er etter barns død men innenfor rett ved død`() {
        val resultat = regel.kjør(LukketPeriode("2020-01-16/2020-01-31"), grunnlagMedDødtBarn(LocalDate.parse("2020-01-15"), RettVedDød.RETT_6_UKER))
        assertThat(resultat).isInstanceOf(TilBeregningAvGrad::class.java)
        val tilBeregningAvGrad = resultat as TilBeregningAvGrad
        assertThat(tilBeregningAvGrad.overstyrtÅrsak).isEqualTo(Årsak.OPPFYLT_PGA_BARNETS_DØDSFALL_6_UKER)
    }

    @Test
    internal fun `Periode er etter barns død men etter rett ved død`() {
        val resultat = regel.kjør(LukketPeriode("2020-05-16/2020-05-30"), grunnlagMedDødtBarn(LocalDate.parse("2020-01-15"), RettVedDød.RETT_6_UKER))
        assertThat(resultat).isInstanceOf(IkkeOppfylt::class.java)
        val ikkeOppfylt = resultat as IkkeOppfylt
        assertThat(ikkeOppfylt.årsaker).isEqualTo(setOf(Årsak.BARNETS_DØDSFALL))
    }


    private fun grunnlagMedDødtBarn(dødsdatoBarn: LocalDate, rettVedDød: RettVedDød): RegelGrunnlag {
        return RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            barn = Barn(aktørId = "123", dødsdato = dødsdatoBarn, rettVedDød = rettVedDød),
            //Feltene nedenfor er ikke relevante for testen
            behandlingUUID = UUID.randomUUID().toString(),
            søker = Søker("456"),
            pleiebehov = mapOf(),
            søktUttak = listOf(),
            arbeid = listOf()
        )
    }

}

private fun nesteSaksnummer(): Saksnummer = UUID.randomUUID().toString().takeLast(19)