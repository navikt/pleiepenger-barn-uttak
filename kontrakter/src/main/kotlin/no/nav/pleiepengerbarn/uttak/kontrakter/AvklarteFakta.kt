package no.nav.pleiepengerbarn.uttak.kontrakter

data class AvklarteFakta(
        val tilsynsbehov: List<Tilsynsbehov>,
        val søktePerioder:List<LukketPeriode>,
        val ferier:List<LukketPeriode> = listOf(),
        val arbeidsperioder:Map<Arbeidsforhold, List<Arbeid>> = mapOf(),
        val andrePartersUttaksplam: List<Uttaksplan> = listOf()
)