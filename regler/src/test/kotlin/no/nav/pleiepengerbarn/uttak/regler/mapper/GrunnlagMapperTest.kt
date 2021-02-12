package no.nav.pleiepengerbarn.uttak.regler.mapper

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import java.time.LocalDate

internal class GrunnlagMapperTest {

    private companion object {
        private const val aktørIdSøker = "123"
        private const val aktørIdBarn = "456"
    }

    @Test
    internal fun `Duplikate arbeidsforholdreferanser med info satt skal feile`() {
        val arbeidsforholdReferanse = Arbeidsforhold(
                type = "Sjømann",
                organisasjonsnummer = "123",
                aktørId = "345",
                arbeidsforholdId = "678"
        )

        assertThrows<IllegalStateException>("Arbeidsforholdene i grunnlaget må være unike.") {
            GrunnlagMapper.tilRegelGrunnlag(
                    uttaksgrunnlag = lagUttaksgrunnag(
                            arbeidsforholdReferanse1 = arbeidsforholdReferanse,
                            arbeidsforholdReferanse2 = arbeidsforholdReferanse
                    ),
                    andrePartersUttakplan = mapOf()
            )
        }
    }

    @Test
    internal fun `Duplikate arbeidsforholdreferanser som kun har nullverdier skal feile`() {
        val arbeidsforholdReferanse = Arbeidsforhold(type = "frilans")

        assertThrows<IllegalStateException>("Arbeidsforholdene i grunnlaget må være unike.") {
            GrunnlagMapper.tilRegelGrunnlag(
                    uttaksgrunnlag = lagUttaksgrunnag(
                            arbeidsforholdReferanse1 = arbeidsforholdReferanse,
                            arbeidsforholdReferanse2 = arbeidsforholdReferanse
                    ),
                    andrePartersUttakplan = mapOf()
            )
        }
    }

    @Test
    internal fun `Om det er to forskjellige arbeidsforholdreferanser og en kun har nullverdier er det ok`() {
        GrunnlagMapper.tilRegelGrunnlag(
                    uttaksgrunnlag = lagUttaksgrunnag(
                            arbeidsforholdReferanse1 = Arbeidsforhold(
                                    type = "selvstendig"
                            ),
                            arbeidsforholdReferanse2 = Arbeidsforhold(
                                    type = "frilans"
                            )
                    ),
                    andrePartersUttakplan = mapOf()
            )
    }

    private fun lagUttaksgrunnag(
            arbeidsforholdReferanse1: Arbeidsforhold,
            arbeidsforholdReferanse2: Arbeidsforhold) = Uttaksgrunnlag(
                    søker = Søker(
                        aktørId = aktørIdSøker,
                        fødselsdato = LocalDate.now().minusYears(50)
                    ),
                    barn = Barn(
                        aktørId = aktørIdBarn
                    ),
                    saksnummer = "1",
                    behandlingUUID = "2",
                    søktUttak = listOf(SøktUttak(LukketPeriode("2020-01-01/2021-01-01"))),
                    pleiebehov = mapOf(),
                    arbeid = listOf(
                            Arbeid(
                                    arbeidsforhold = arbeidsforholdReferanse1,
                                    perioder = mapOf()
                            ),
                            Arbeid(
                                    arbeidsforhold = arbeidsforholdReferanse2,
                                    perioder = mapOf()
                            )
                    )
            )
}