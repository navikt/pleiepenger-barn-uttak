package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*

object GradBeregner {

    fun beregnGrad(uttaksperiode: Uttaksperiode, grunnlag: RegelGrunnlag):Prosent {
        val tilsynsbehov = finnTilsynsbehov(uttaksperiode.periode, grunnlag)
        val andreParter = finnSumAndreParter(uttaksperiode.periode, grunnlag)
        val arbeid = finnSumArbeid(uttaksperiode.periode, grunnlag)
        val tilsyn = finnTilsyn(uttaksperiode.periode, grunnlag)

        val gjenværendeProsentTilsynsbehov = tilsynsbehov - andreParter
        val søktProsent = Prosent(100) - (max(arbeid, tilsyn))
        if (søktProsent > gjenværendeProsentTilsynsbehov) {
            return gjenværendeProsentTilsynsbehov
        }
        return søktProsent
    }

    private fun finnSumAndreParter(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        var sumAndreParter = Prosent.ZERO
        grunnlag.andrePartersUttaksplan.forEach {
            val annenPartsPeriode = it.perioder.find { overlapper(it.periode, periode) }
            if (annenPartsPeriode != null) {
                sumAndreParter += annenPartsPeriode.uttaksperiodeResultat.grad
            }
        }
        return sumAndreParter
    }

    private fun finnSumArbeid(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        var sumArbeid = Prosent.ZERO
        grunnlag.arbeidsforhold.forEach { (arbeidsforhold, arbeidListe) ->
            val arbeid = arbeidListe.find { overlapper(it.periode, periode) }
            if (arbeid != null) {
                sumArbeid += arbeid.grad
            }
        }
        return sumArbeid
    }


    private fun finnTilsyn(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        val tilsyn = grunnlag.tilsynPerioder.find { overlapper(it.periode, periode) }
        if (tilsyn != null) {
            return tilsyn.grad
        }
        return Prosent.ZERO
    }

    private fun finnTilsynsbehov(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        val tilsynsbehovSomOverlapperMedPeriode = grunnlag.tilsynsbehov.find { overlapper(it.periode, periode) }
        return when (tilsynsbehovSomOverlapperMedPeriode?.tilsynsbehovStørrelse) {
            TilsynsbehovStørrelse.PROSENT_100 -> Prosent(100)
            TilsynsbehovStørrelse.PROSENT_200 -> Prosent(200)
            else -> Prosent.ZERO
        }
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