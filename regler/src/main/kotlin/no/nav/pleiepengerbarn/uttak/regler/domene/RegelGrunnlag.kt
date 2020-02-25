package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.*

data class RegelGrunnlag(
        val tilsynsbehov: List<Tilsynsbehov>,
        val søknadsperioder:List<LukketPeriode>,
        val arbeidsforhold: List<Arbeidsforhold> = listOf(),
        val tilsynsperioder:List<Tilsyn> = listOf(),
        val ferier:List<LukketPeriode> = listOf(),
        val andrePartersUttaksplan: List<Uttaksplan> = listOf(),
        val ikkeMedlem: List<LukketPeriode> = listOf()
)
