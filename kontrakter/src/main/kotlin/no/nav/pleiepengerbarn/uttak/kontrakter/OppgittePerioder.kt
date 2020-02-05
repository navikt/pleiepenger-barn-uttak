package no.nav.pleiepengerbarn.uttak.kontrakter

data class OppgittePerioder(
        val søktePerioder:List<LukketPeriode>,
        val ferier:List<LukketPeriode> = listOf(),
        val arbeidsperioder:Map<Arbeidsforhold, List<Arbeid>> = mapOf()
)