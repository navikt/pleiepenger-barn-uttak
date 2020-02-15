package no.nav.pleiepengerbarn.uttak.kontrakter

data class Arbeid(
        val periode: LukketPeriode,
        val inntekt:Beløp,
        val arbeidsprosent: Prosent = Prosent.ZERO
)