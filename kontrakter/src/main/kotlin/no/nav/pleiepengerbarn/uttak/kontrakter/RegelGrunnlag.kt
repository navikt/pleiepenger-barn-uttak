package no.nav.pleiepengerbarn.uttak.kontrakter

data class RegelGrunnlag(
        val tilsynsbehov: List<Tilsynsbehov>,
        val arbeidsforhold: Map<Arbeidsforhold, List<Arbeid>>,
        val søktePerioder:List<LukketPeriode>,
        val ferier:List<LukketPeriode> = listOf(),
        val andrePartersUttaksplam: List<Uttaksplan> = listOf()
)