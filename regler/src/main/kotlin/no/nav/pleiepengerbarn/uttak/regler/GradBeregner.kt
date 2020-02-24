package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import java.math.BigDecimal
import java.math.RoundingMode

object GradBeregner {


    fun beregnGrad(uttaksperiode: Uttaksperiode, grunnlag: RegelGrunnlag):Prosent {
        val tilsynsbehov = finnTilsynsbehov(uttaksperiode.periode, grunnlag)
        val andreParter = finnSumAndreParter(uttaksperiode.periode, grunnlag)

        val sumForventetInntekt = finnSumForventetInntekt(uttaksperiode.periode, grunnlag)
        val sumFaktiskInntekt = finnSumFaktiskInntekt(uttaksperiode.periode, grunnlag)


        var fårJobbetProsent = BigDecimal.ZERO
        if (sumForventetInntekt.compareTo(BigDecimal.ZERO) != 0) {
            fårJobbetProsent = sumFaktiskInntekt / sumForventetInntekt
        }
        val tilsynProsent = finnTilsyn(uttaksperiode.periode, grunnlag)

        val gjenværendeTilsynsbehovProsent = tilsynsbehov - andreParter


        val utbetalingsgrad = Prosent(100) - max(fårJobbetProsent, tilsynProsent)
        if (utbetalingsgrad > gjenværendeTilsynsbehovProsent) {
            return gjenværendeTilsynsbehovProsent
        }
        return utbetalingsgrad
    }

    private fun finnSumAndreParter(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        var sumAndreParter = Prosent.ZERO
        grunnlag.andrePartersUttaksplan.forEach {uttaksplan ->
            val annenPartsPeriode = uttaksplan.perioder.find { overlapper(it.periode, periode) }
            if (annenPartsPeriode != null) {
                sumAndreParter += annenPartsPeriode.uttaksperiodeResultat.grad
            }
        }
        return sumAndreParter
    }

    private fun finnSumForventetInntekt(periode:LukketPeriode, grunnlag: RegelGrunnlag):Beløp {
        var sumForventetInntekt = Beløp.ZERO.setScale(2, RoundingMode.HALF_EVEN)
        grunnlag.arbeidsforhold.forEach { (_, arbeidListe) ->
            val arbeid = arbeidListe.find { overlapper(it.periode, periode) }
            if (arbeid != null) {
                sumForventetInntekt += arbeid.inntekt
            }
        }
        return sumForventetInntekt
    }

    private fun finnSumFaktiskInntekt(periode:LukketPeriode, grunnlag: RegelGrunnlag):Beløp {
        var sumFaktiskInntekt = Beløp.ZERO.setScale(2, RoundingMode.HALF_EVEN)
        grunnlag.arbeidsforhold.forEach { (_, arbeidListe) ->
            val arbeid = arbeidListe.find { overlapper(it.periode, periode) }
            if (arbeid != null) {
                sumFaktiskInntekt += arbeid.arbeidsprosent.setScale(2, RoundingMode.HALF_EVEN)*arbeid.inntekt
            }
        }
        return sumFaktiskInntekt
    }

    private fun finnTilsyn(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        val tilsyn = grunnlag.tilsynsperioder.find { overlapper(it.periode, periode) }
        if (tilsyn != null) {
            return tilsyn.grad.setScale(2, RoundingMode.HALF_EVEN)
        }
        return Prosent.ZERO.setScale(2, RoundingMode.HALF_EVEN)
    }

    private fun finnTilsynsbehov(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        val tilsynsbehovSomOverlapperMedPeriode = grunnlag.tilsynsbehov.find { overlapper(it.periode, periode) }
        return when (tilsynsbehovSomOverlapperMedPeriode?.tilsynsbehovStørrelse) {
            TilsynsbehovStørrelse.PROSENT_100 -> Prosent(100)
            TilsynsbehovStørrelse.PROSENT_200 -> Prosent(200)
            else -> Prosent.ZERO
        }.setScale(2, RoundingMode.HALF_EVEN)
    }

    private fun max(a:Prosent, b:Prosent):Prosent {
        if (b > a) {
            return b
        }
        return a
    }

    private fun overlapper(periode1: LukketPeriode, periode2: LukketPeriode) =
            (periode1.fom == periode2.fom || periode1.fom.isBefore(periode2.fom)) &&
                    (periode1.tom == periode2.tom || periode1.tom.isAfter(periode2.tom))



}