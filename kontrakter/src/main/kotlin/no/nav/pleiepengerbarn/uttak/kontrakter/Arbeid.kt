package no.nav.pleiepengerbarn.uttak.kontrakter

data class Arbeid(
        val arbeidsforhold: Arbeidsforhold,
        val periode: LukketPeriode,
        val inntekt:Beløp,
        val arbeidsprosent: Prosent = Prosent.ZERO
) {
    fun inntektstap():Beløp {
        return (Prosent(100)-arbeidsprosent)*inntekt/Prosent(100)
    }
}