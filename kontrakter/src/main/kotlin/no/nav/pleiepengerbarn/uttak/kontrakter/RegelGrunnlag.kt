package no.nav.pleiepengerbarn.uttak.kontrakter

data class RegelGrunnlag(
        val tilsynsbehov: List<Tilsynsbehov>,
        val søktePerioder:List<LukketPeriode>,
        val arbeidsforhold: Map<Arbeidsforhold, List<Arbeid>> = mapOf(),
        val tilsynsperioder:List<Tilsyn> = listOf(),
        val ferier:List<LukketPeriode> = listOf(),
        val andrePartersUttaksplan: List<Uttaksplan> = listOf()
)