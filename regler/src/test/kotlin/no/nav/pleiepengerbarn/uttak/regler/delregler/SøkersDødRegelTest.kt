package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SøkersDødRegelTest {

    private val regel: SøkersDødRegel = SøkersDødRegel()

    @Test
    internal fun `Søker dør etter periode`() {
        val resultat = regel.kjør(LukketPeriode("2020-01-01/2020-01-31"), grunnlagMedDødSøker(LocalDate.parse("2020-02-15")))
        Assertions.assertThat(resultat).isInstanceOf(TilBeregningAvGrad::class.java)
        val tilBeregningAvGrad = resultat as TilBeregningAvGrad
        Assertions.assertThat(tilBeregningAvGrad.overstyrtÅrsak).isNull()
    }

    @Test
    internal fun `Søker tom dødsdato periode`() {
        val resultat = regel.kjør(LukketPeriode("2020-01-01/2020-01-31"), grunnlagMedDødSøker(LocalDate.parse("2020-01-31")))
        Assertions.assertThat(resultat).isInstanceOf(TilBeregningAvGrad::class.java)
        val tilBeregningAvGrad = resultat as TilBeregningAvGrad
        Assertions.assertThat(tilBeregningAvGrad.overstyrtÅrsak).isNull()
    }

    @Test
    internal fun `Søker fom dagen etter dødsdato periode`() {
        val resultat = regel.kjør(LukketPeriode("2020-02-01/2020-02-02"), grunnlagMedDødSøker(LocalDate.parse("2020-01-31")))
        Assertions.assertThat(resultat).isInstanceOf(IkkeOppfylt::class.java)
        val tilBeregningAvGrad = resultat as IkkeOppfylt
        Assertions.assertThat(tilBeregningAvGrad.årsaker).isEqualTo(setOf(Årsak.SØKERS_DØDSFALL))
    }

    @Test
    internal fun `Periode er etter søkers død`() {
        val resultat = regel.kjør(LukketPeriode("2020-01-16/2020-01-31"), grunnlagMedDødSøker(LocalDate.parse("2020-01-15")))
        Assertions.assertThat(resultat).isInstanceOf(IkkeOppfylt::class.java)
        val tilBeregningAvGrad = resultat as IkkeOppfylt
        Assertions.assertThat(tilBeregningAvGrad.årsaker).isEqualTo(setOf(Årsak.SØKERS_DØDSFALL))
    }

    @Test
    internal fun `Periode er etter søkers død med mer enn litt`() {
        val resultat = regel.kjør(LukketPeriode("2020-05-16/2020-05-30"), grunnlagMedDødSøker(LocalDate.parse("2020-01-15")))
        Assertions.assertThat(resultat).isInstanceOf(IkkeOppfylt::class.java)
        val ikkeOppfylt = resultat as IkkeOppfylt
        Assertions.assertThat(ikkeOppfylt.årsaker).isEqualTo(setOf(Årsak.SØKERS_DØDSFALL))
    }


    private fun grunnlagMedDødSøker(dødsdato: LocalDate): RegelGrunnlag {
        return RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            barn = Barn(aktørId = "123"),
            //Feltene nedenfor er ikke relevante for testen
            behandlingUUID = UUID.randomUUID(),
            søker = Søker(aktørId = "456", dødsdato = dødsdato),
            pleiebehov = mapOf(),
            søktUttak = listOf(),
            arbeid = listOf()
        )
    }

}

private fun nesteSaksnummer(): Saksnummer = UUID.randomUUID().toString().takeLast(19)
