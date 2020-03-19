package no.nav.pleiepengerbarn.uttak.regler.mapper

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import java.time.LocalDate

internal class GrunnlagMapperTest {
    @Test
    internal fun `Duplikate arbeidsforholdreferanser med info satt skal feile`() {
        val arbeidsforholdReferanse = ArbeidsforholdReferanse(
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
        val arbeidsforholdReferanse = ArbeidsforholdReferanse()

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
                            arbeidsforholdReferanse1 = ArbeidsforholdReferanse(),
                            arbeidsforholdReferanse2 = ArbeidsforholdReferanse(
                                    type = "Seiler"
                            )
                    ),
                    andrePartersUttakplan = mapOf()
            )
    }

    private fun lagUttaksgrunnag(
            arbeidsforholdReferanse1: ArbeidsforholdReferanse,
            arbeidsforholdReferanse2: ArbeidsforholdReferanse) = Uttaksgrunnlag(
                    søker = Søker(
                        fødselsdato = LocalDate.now().minusYears(50)
                    ),
                    saksnummer = "1",
                    behandlingId = "2",
                    medlemskap = mapOf(),
                    søknadsperioder = listOf(LukketPeriode("2020-01-01/2021-01-01")),
                    tilsynsbehov = mapOf(),
                    arbeid = listOf(
                            Arbeidsforhold(
                                    arbeidsforhold = arbeidsforholdReferanse1,
                                    perioder = mapOf()
                            ),
                            Arbeidsforhold(
                                    arbeidsforhold = arbeidsforholdReferanse2,
                                    perioder = mapOf()
                            )
                    )
            )
}