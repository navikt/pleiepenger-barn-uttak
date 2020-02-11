package no.nav.pleiepengerbarn.uttak.kontrakter

data class RegelGrunnlag(
        val tilsynsbehov: List<Tilsynsbehov>,
        val arbeidsforhold: Map<Arbeidsforhold, List<Arbeid>>,
        val søktePerioder:List<LukketPeriode>,
        val tilsynPerioder:List<Tilsyn> = listOf(),
        val ferier:List<LukketPeriode> = listOf(),
        val andrePartersUttaksplan: List<Uttaksplan> = listOf()
)